package co.edu.usco.goltime.autenticacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

import co.edu.usco.goltime.BaseIntegracion;

/**
 * Ingreso con Google (ADR 0023). En pruebas el verificador acepta credenciales {@code prueba|correo|nombres|apellidos}
 * en lugar de un token real de Google; todo lo demás (cuentas, sesión, cookie, renovación) es el código de producción.
 */
class AutenticacionIT extends BaseIntegracion {

    private static String texto(MvcResult r, String ruta) throws Exception {
        return JsonPath.read(r.getResponse().getContentAsString(), ruta);
    }

    // --- Ingreso con Google ---

    @SuppressWarnings("unchecked")
    @Test
    void laPrimeraVezCreaUnClienteConElCorreoVerificadoSinWhatsappNiContrasena() throws Exception {
        String correo = "Nueva.Persona-" + System.nanoTime() + "@Prueba.TEST";

        MvcResult r = ingresoGoogle(credencialGoogle(correo, "Nueva", "Persona"), true);

        esperarEstado(r, 200);
        String cuerpo = r.getResponse().getContentAsString();
        assertThat((String) JsonPath.read(cuerpo, "$.usuario.rol")).isEqualTo("CLIENTE");
        assertThat((String) JsonPath.read(cuerpo, "$.usuario.correo")).isEqualTo(correo.toLowerCase());
        assertThat((Boolean) JsonPath.read(cuerpo, "$.usuario.correoVerificado")).isTrue();
        assertThat((Map<String, Object>) JsonPath.read(cuerpo, "$.usuario")).as("Google no da el WhatsApp").doesNotContainKey("telefono");
        assertThat(cuerpo).doesNotContain("googleSub").doesNotContain("claveHash").doesNotContain("prueba-");
        // acepta los términos al entrar (aviso en la pantalla de ingreso)
        assertThat(jdbc.queryForObject("SELECT acepto_terminos_en IS NOT NULL FROM usuarios WHERE lower(correo) = ?", Boolean.class,
                correo.toLowerCase())).isTrue();
    }

    @Test
    void laSegundaVezEntraALaMismaCuentaSinDuplicarla() throws Exception {
        String correo = correoUnico();
        long primera = entrar(correo, false).id();
        long segunda = entrar(correo, false).id();

        assertThat(segunda).isEqualTo(primera);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM usuarios WHERE lower(correo) = ?", Integer.class, correo)).isEqualTo(1);
    }

    @Test
    void elAdministradorAutorizadoDeAntemanoEntraComoAdministradorYQuedaVinculado() throws Exception {
        Sesion admin = ingresarAdmin();

        assertThat((String) JsonPath.read(consultar("/api/v1/me", admin.token()).getResponse().getContentAsString(), "$.rol")).isEqualTo("ADMIN");
        esperarEstado(consultar("/api/v1/admin/clientes", admin.token()), 200);
        assertThat(jdbc.queryForObject("SELECT google_sub FROM usuarios WHERE correo = ?", String.class, ADMIN_CORREO)).isNotBlank();
    }

    @Test
    void cualquierOtraPersonaQueEntraSoloLlegaASerClienteYNoAbreLaAdministracion() throws Exception {
        Sesion cliente = nuevoClienteConSesion();

        assertThat((String) JsonPath.read(consultar("/api/v1/me", cliente.token()).getResponse().getContentAsString(), "$.rol")).isEqualTo("CLIENTE");
        esperarEstado(consultar("/api/v1/admin/clientes", cliente.token()), 403);
    }

    @Test
    void siGoogleNoConfirmaElCorreoNoSeEntraNiSeCreaLaCuenta() throws Exception {
        String correo = correoUnico();

        MvcResult r = ingresoGoogle("noverificado|" + correo + "|Ana|Pérez", false);

        esperarEstado(r, 403);
        assertThat(texto(r, "$.codigo")).isEqualTo("CORREO_NO_VERIFICADO");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM usuarios WHERE lower(correo) = ?", Integer.class, correo)).isZero();
    }

    @Test
    void unaCredencialInventadaOVaciaSeRechaza() throws Exception {
        MvcResult inventada = ingresoGoogle("esto-no-es-un-token", false);
        esperarEstado(inventada, 401);
        assertThat(texto(inventada, "$.codigo")).isEqualTo("CREDENCIALES_INVALIDAS");

        esperarEstado(enviar("/api/v1/autenticacion/google", Map.of("credencial", "", "recordarme", false)), 400);
    }

    @Test
    void dejaElRefreshEnUnaCookieSeguraYSinRecordarmeEsDeSesion() throws Exception {
        MvcResult recordado = ingresoGoogle(credencialGoogle(correoUnico(), "Ana", "Pérez"), true);
        esperarEstado(recordado, 200);
        String cabecera = recordado.getResponse().getHeader("Set-Cookie");
        assertThat(cabecera).contains("goltime_refresco=").contains("HttpOnly").contains("SameSite=Strict")
                .contains("Path=/api/v1/autenticacion").contains("Max-Age=");
        assertThat(recordado.getResponse().getContentAsString()).doesNotContain(valorCookie(recordado));

        MvcResult deSesion = ingresoGoogle(credencialGoogle(correoUnico(), "Ana", "Pérez"), false);
        assertThat(deSesion.getResponse().getHeader("Set-Cookie")).doesNotContain("Max-Age=");
    }

    @Test
    void laConfiguracionPublicaDiceComoMostrarElBoton() throws Exception {
        MvcResult r = consultar("/api/v1/autenticacion/configuracion", null);

        esperarEstado(r, 200);
        assertThat((Boolean) JsonPath.read(r.getResponse().getContentAsString(), "$.modoPrueba")).isTrue();
        assertThat((String) JsonPath.read(r.getResponse().getContentAsString(), "$.googleClientId")).isEmpty();
    }

    @Test
    void yaNoExistenElRegistroNiLasContrasenas() throws Exception {
        for (String ruta : new String[] { "/registrar", "/ingresar", "/olvide-clave", "/restablecer-clave", "/verificar-correo" }) {
            assertThat(enviar("/api/v1/autenticacion" + ruta, Map.of("correo", correoUnico(), "clave", "Clave1234")).getResponse().getStatus())
                    .as(ruta).isEqualTo(404);
        }
        Sesion s = nuevoClienteConSesion();
        assertThat(enviarConToken("/api/v1/me/clave", Map.of("claveActual", "x", "claveNueva", "y", "confirmacion", "y"), s.token())
                .getResponse().getStatus()).isEqualTo(404);
    }

    // --- Completar el perfil tras entrar con Google ---

    @SuppressWarnings("unchecked")
    @Test
    void unClienteNuevoNoTieneElPerfilCompletoYNoPuedeReservarHastaCompletarlo() throws Exception {
        MvcResult r = ingresoGoogle(credencialGoogle(correoUnico(), "Ana", "Pérez"), false);
        Sesion s = sesionDe(r);
        assertThat((Boolean) JsonPath.read(r.getResponse().getContentAsString(), "$.usuario.perfilCompleto")).isFalse();

        // sin WhatsApp no se puede solicitar una reserva (el servidor lo exige, no solo la pantalla)
        MvcResult bloqueada = enviarConToken("/api/v1/reservas", Map.of("canchaId", 1, "inicio", "2030-01-01T15:00:00Z", "fin", "2030-01-01T16:00:00Z"), s.token());
        esperarEstado(bloqueada, 409);
        assertThat(texto(bloqueada, "$.codigo")).isEqualTo("PERFIL_INCOMPLETO");

        MvcResult completo = enviarConToken("/api/v1/me/completar-perfil",
                Map.of("nombres", "Ana María", "apellidos", "Pérez Gómez", "telefono", "300 123 4567", "documento", "1075123456"), s.token());
        esperarEstado(completo, 200);
        String cuerpo = completo.getResponse().getContentAsString();
        assertThat((Boolean) JsonPath.read(cuerpo, "$.perfilCompleto")).isTrue();
        assertThat((String) JsonPath.read(cuerpo, "$.telefono")).isEqualTo("+573001234567");
        assertThat((Boolean) JsonPath.read(cuerpo, "$.telefonoVerificado")).as("el WhatsApp se verifica después, con el bot").isFalse();
        assertThat((Map<String, Object>) JsonPath.read(consultar("/api/v1/me", s.token()).getResponse().getContentAsString(), "$"))
                .containsEntry("perfilCompleto", true);
    }

    @Test
    void completarElPerfilExigeApellidosYWhatsappValido() throws Exception {
        Sesion s = sesionDe(ingresoGoogle(credencialGoogle(correoUnico(), "Ana", ""), false));

        esperarEstado(enviarConToken("/api/v1/me/completar-perfil", Map.of("nombres", "Ana", "apellidos", "", "telefono", "3001234567"), s.token()), 400);
        esperarEstado(enviarConToken("/api/v1/me/completar-perfil", Map.of("nombres", "Ana", "apellidos", "Pérez", "telefono", ""), s.token()), 400);
        MvcResult invalido = enviarConToken("/api/v1/me/completar-perfil", Map.of("nombres", "Ana", "apellidos", "Pérez", "telefono", "8712345"), s.token());
        esperarEstado(invalido, 400);
        assertThat(texto(invalido, "$.codigo")).isEqualTo("DATOS_INVALIDOS");
        // sin sesión no se puede
        esperarEstado(enviar("/api/v1/me/completar-perfil", Map.of("nombres", "Ana", "apellidos", "Pérez", "telefono", "3001234567")), 401);
    }

    @Test
    void elAdministradorNuncaNecesitaCompletarElPerfil() throws Exception {
        Sesion admin = ingresarAdmin();
        assertThat((Boolean) JsonPath.read(consultar("/api/v1/me", admin.token()).getResponse().getContentAsString(), "$.perfilCompleto")).isTrue();
    }

    // --- Refresh con rotación ---

    @Test
    void elRefreshRotaYLaReutilizacionRevocaTodaFamilia() throws Exception {
        Sesion inicial = nuevoClienteConSesion();

        MvcResult primera = enviar("/api/v1/autenticacion/refrescar", Map.of(), cookie(inicial.refresco()));
        esperarEstado(primera, 200);
        String rotado = valorCookie(primera);
        assertThat(rotado).isNotBlank().isNotEqualTo(inicial.refresco());
        esperarEstado(consultar("/api/v1/me", sesionDe(primera).token()), 200);

        // Reusar el token viejo (ya rotado) es señal de robo: 401 y se revoca la familia completa.
        esperarEstado(enviar("/api/v1/autenticacion/refrescar", Map.of(), cookie(inicial.refresco())), 401);
        esperarEstado(enviar("/api/v1/autenticacion/refrescar", Map.of(), cookie(rotado)), 401);
    }

    @Test
    void refrescarSinCookieOConCookieFalsaDa401() throws Exception {
        esperarEstado(enviar("/api/v1/autenticacion/refrescar", Map.of()), 401);
        esperarEstado(enviar("/api/v1/autenticacion/refrescar", Map.of(), cookie("inventado")), 401);
    }

    @Test
    void salirRevocaElRefreshYBorraLaCookie() throws Exception {
        Sesion s = nuevoClienteConSesion();

        MvcResult r = enviar("/api/v1/autenticacion/salir", Map.of(), cookie(s.refresco()));

        esperarEstado(r, 204);
        assertThat(r.getResponse().getHeader("Set-Cookie")).contains("Max-Age=0");
        esperarEstado(enviar("/api/v1/autenticacion/refrescar", Map.of(), cookie(s.refresco())), 401);
        // idempotente
        esperarEstado(enviar("/api/v1/autenticacion/salir", Map.of()), 204);
    }

    // --- Perfil ---

    @Test
    void elPerfilSoloMuestraYEditaLosDatosDelPropioUsuario() throws Exception {
        Sesion a = nuevoClienteConSesion();
        Sesion b = nuevoClienteConSesion();

        MvcResult edicion = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/me")
                .header("Authorization", "Bearer " + a.token())
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("nombres", "Carlos", "apellidos", "Gómez", "telefono", "3110000000"))))
                .andReturn();
        esperarEstado(edicion, 200);

        assertThat((String) JsonPath.read(consultar("/api/v1/me", a.token()).getResponse().getContentAsString(), "$.nombres")).isEqualTo("Carlos");
        assertThat((String) JsonPath.read(consultar("/api/v1/me", b.token()).getResponse().getContentAsString(), "$.nombres")).isEqualTo("Ana");
    }
}
