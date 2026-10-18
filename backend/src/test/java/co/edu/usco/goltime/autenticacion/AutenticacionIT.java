package co.edu.usco.goltime.autenticacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

import co.edu.usco.goltime.BaseIntegracion;
import co.edu.usco.goltime.notificaciones.aplicacion.EnviadorCorreo;
import co.edu.usco.goltime.notificaciones.aplicacion.EnviadorCorreo.MensajeCorreo;

class AutenticacionIT extends BaseIntegracion {

    @MockitoBean
    EnviadorCorreo correo;

    @BeforeEach
    void limpiarMock() {
        reset(correo);
    }

    private String tokenDelUltimoCorreo() {
        ArgumentCaptor<MensajeCorreo> captor = ArgumentCaptor.forClass(MensajeCorreo.class);
        verify(correo).enviar(captor.capture());
        Matcher m = Pattern.compile("token=([^&\\s]+)").matcher((String) captor.getValue().variables().get("enlace"));
        assertThat(m.find()).isTrue();
        return m.group(1);
    }

    // --- Registro ---

    @Test
    void registraUnClienteSinExponerElHashNiPermitirElegirRol() throws Exception {
        String correoNuevo = "Nueva.Persona@Prueba.TEST";
        Map<String, Object> datos = new java.util.HashMap<>(datosRegistro(correoNuevo, CLAVE_VALIDA));
        datos.put("rol", "ADMIN"); // intento de escalar privilegios: debe ignorarse

        MvcResult r = enviar("/api/v1/autenticacion/registrar", datos);

        esperarEstado(r, 201);
        String cuerpo = r.getResponse().getContentAsString();
        assertThat((String) JsonPath.read(cuerpo, "$.rol")).isEqualTo("CLIENTE");
        assertThat((String) JsonPath.read(cuerpo, "$.correo")).isEqualTo("nueva.persona@prueba.test");
        assertThat(cuerpo).doesNotContain("claveHash").doesNotContain("clave_hash").doesNotContain("$2");
    }

    @Test
    void rechazaCorreoDuplicadoSinImportarMayusculas() throws Exception {
        String c = correoUnico();
        registrar(c, CLAVE_VALIDA);
        MvcResult r = enviar("/api/v1/autenticacion/registrar", datosRegistro(c.toUpperCase(), CLAVE_VALIDA));
        esperarEstado(r, 409);
        assertThat((String) JsonPath.read(r.getResponse().getContentAsString(), "$.codigo")).isEqualTo("CORREO_YA_REGISTRADO");
    }

    @Test
    void rechazaClavesDebilesYTerminosNoAceptados() throws Exception {
        esperarEstado(enviar("/api/v1/autenticacion/registrar", datosRegistro(correoUnico(), "sololetras")), 400);

        Map<String, Object> sinTerminos = new java.util.HashMap<>(datosRegistro(correoUnico(), CLAVE_VALIDA));
        sinTerminos.put("aceptaTerminos", false);
        MvcResult r = enviar("/api/v1/autenticacion/registrar", sinTerminos);
        esperarEstado(r, 400);
        assertThat((String) JsonPath.read(r.getResponse().getContentAsString(), "$.codigo")).isEqualTo("DATOS_INVALIDOS");

        Map<String, Object> noCoincide = new java.util.HashMap<>(datosRegistro(correoUnico(), CLAVE_VALIDA));
        noCoincide.put("confirmacionClave", "OtraClave99");
        esperarEstado(enviar("/api/v1/autenticacion/registrar", noCoincide), 400);
    }

    @Test
    void verificaElCorreoConUnTokenDeUnSoloUso() throws Exception {
        String c = correoUnico();
        registrar(c, CLAVE_VALIDA);
        String token = tokenDelUltimoCorreo();

        esperarEstado(enviar("/api/v1/autenticacion/verificar-correo", Map.of("token", token)), 204);
        assertThat(jdbc.queryForObject("SELECT correo_verificado FROM usuarios WHERE correo = ?", Boolean.class, c)).isTrue();

        MvcResult repetido = enviar("/api/v1/autenticacion/verificar-correo", Map.of("token", token));
        esperarEstado(repetido, 400);
        assertThat((String) JsonPath.read(repetido.getResponse().getContentAsString(), "$.codigo")).isEqualTo("TOKEN_INVALIDO");
    }

    // --- Ingreso ---

    @Test
    void ingresaYDejaElRefreshEnUnaCookieSegura() throws Exception {
        String c = correoUnico();
        registrar(c, CLAVE_VALIDA);

        MvcResult r = enviar("/api/v1/autenticacion/ingresar", Map.of("correo", c, "clave", CLAVE_VALIDA, "recordarme", true));

        esperarEstado(r, 200);
        String cabecera = r.getResponse().getHeader("Set-Cookie");
        assertThat(cabecera).contains("goltime_refresco=").contains("HttpOnly").contains("SameSite=Strict")
                .contains("Path=/api/v1/autenticacion").contains("Max-Age=");
        assertThat(r.getResponse().getContentAsString()).doesNotContain(valorCookie(r));
        // el access token abre /me
        Sesion s = sesionDe(r);
        MvcResult me = consultar("/api/v1/me", s.token());
        esperarEstado(me, 200);
        assertThat((String) JsonPath.read(me.getResponse().getContentAsString(), "$.correo")).isEqualTo(c);
    }

    @Test
    void sinRecordarmeLaCookieEsDeSesion() throws Exception {
        String c = correoUnico();
        registrar(c, CLAVE_VALIDA);
        MvcResult r = enviar("/api/v1/autenticacion/ingresar", Map.of("correo", c, "clave", CLAVE_VALIDA, "recordarme", false));
        assertThat(r.getResponse().getHeader("Set-Cookie")).doesNotContain("Max-Age=");
    }

    @Test
    void elErrorDeCredencialesEsGenericoParaCorreoInexistenteOClaveMala() throws Exception {
        String c = correoUnico();
        registrar(c, CLAVE_VALIDA);

        MvcResult claveMala = enviar("/api/v1/autenticacion/ingresar", Map.of("correo", c, "clave", "Incorrecta99"));
        MvcResult noExiste = enviar("/api/v1/autenticacion/ingresar", Map.of("correo", correoUnico(), "clave", "Incorrecta99"));

        esperarEstado(claveMala, 401);
        esperarEstado(noExiste, 401);
        assertThat(claveMala.getResponse().getContentAsString()).isEqualTo(noExiste.getResponse().getContentAsString());
        assertThat((String) JsonPath.read(claveMala.getResponse().getContentAsString(), "$.codigo")).isEqualTo("CREDENCIALES_INVALIDAS");
    }

    @Test
    void bloqueaTemporalmenteTrasVariosFallos() throws Exception {
        String c = correoUnico();
        registrar(c, CLAVE_VALIDA);
        for (int i = 0; i < 5; i++) {
            esperarEstado(enviar("/api/v1/autenticacion/ingresar", Map.of("correo", c, "clave", "Incorrecta99")), 401);
        }
        MvcResult bloqueado = enviar("/api/v1/autenticacion/ingresar", Map.of("correo", c, "clave", CLAVE_VALIDA));
        esperarEstado(bloqueado, 429);
        assertThat((String) JsonPath.read(bloqueado.getResponse().getContentAsString(), "$.codigo")).isEqualTo("DEMASIADOS_INTENTOS");
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

    // --- Cambio de contraseña ---

    @Test
    void cambiarLaClaveInvalidaElTokenActualLosRefreshYLaClaveVieja() throws Exception {
        String c = correoUnico();
        registrar(c, CLAVE_VALIDA);
        Sesion s = ingresar(c, CLAVE_VALIDA, false);

        esperarEstado(enviarConToken("/api/v1/me/clave",
                Map.of("claveActual", "Equivocada1", "claveNueva", "Nueva56789", "confirmacion", "Nueva56789"), s.token()), 401);

        esperarEstado(enviarConToken("/api/v1/me/clave",
                Map.of("claveActual", CLAVE_VALIDA, "claveNueva", "Nueva56789", "confirmacion", "Nueva56789"), s.token()), 204);

        esperarEstado(consultar("/api/v1/me", s.token()), 401);
        esperarEstado(enviar("/api/v1/autenticacion/refrescar", Map.of(), cookie(s.refresco())), 401);
        esperarEstado(enviar("/api/v1/autenticacion/ingresar", Map.of("correo", c, "clave", CLAVE_VALIDA)), 401);
        esperarEstado(enviar("/api/v1/autenticacion/ingresar", Map.of("correo", c, "clave", "Nueva56789")), 200);
    }

    // --- Recuperación ---

    @Test
    void recuperarLaClaveNoRevelaSiElCorreoExiste() throws Exception {
        esperarEstado(enviar("/api/v1/autenticacion/olvide-clave", Map.of("correo", correoUnico())), 202);
        verify(correo, never()).enviar(any());
    }

    @Test
    void restableceLaClaveConUnTokenDeUnSoloUsoYCierraLasSesiones() throws Exception {
        String c = correoUnico();
        registrar(c, CLAVE_VALIDA);
        reset(correo); // descarta el correo de verificación
        Sesion s = ingresar(c, CLAVE_VALIDA, true);

        esperarEstado(enviar("/api/v1/autenticacion/olvide-clave", Map.of("correo", c)), 202);
        String token = tokenDelUltimoCorreo();

        esperarEstado(enviar("/api/v1/autenticacion/restablecer-clave",
                Map.of("token", token, "claveNueva", "Restablecida7", "confirmacion", "Restablecida7")), 204);

        esperarEstado(enviar("/api/v1/autenticacion/ingresar", Map.of("correo", c, "clave", "Restablecida7")), 200);
        esperarEstado(enviar("/api/v1/autenticacion/refrescar", Map.of(), cookie(s.refresco())), 401);
        esperarEstado(consultar("/api/v1/me", s.token()), 401);

        // El mismo enlace no sirve dos veces.
        MvcResult repetido = enviar("/api/v1/autenticacion/restablecer-clave",
                Map.of("token", token, "claveNueva", "OtraMas1234", "confirmacion", "OtraMas1234"));
        esperarEstado(repetido, 400);
    }

    @Test
    void elTokenDeRecuperacionNoSirveParaVerificarCorreoNiAlReves() throws Exception {
        String c = correoUnico();
        registrar(c, CLAVE_VALIDA);
        String tokenVerificacion = tokenDelUltimoCorreo();

        esperarEstado(enviar("/api/v1/autenticacion/restablecer-clave",
                Map.of("token", tokenVerificacion, "claveNueva", "Restablecida7", "confirmacion", "Restablecida7")), 400);
    }

    @Test
    void soloElUltimoEnlaceDeRecuperacionEsValido() throws Exception {
        String c = correoUnico();
        registrar(c, CLAVE_VALIDA);
        reset(correo);

        enviar("/api/v1/autenticacion/olvide-clave", Map.of("correo", c));
        ArgumentCaptor<MensajeCorreo> primero = ArgumentCaptor.forClass(MensajeCorreo.class);
        verify(correo).enviar(primero.capture());
        String tokenViejo = token(primero.getValue());

        reset(correo);
        enviar("/api/v1/autenticacion/olvide-clave", Map.of("correo", c));
        esperarEstado(enviar("/api/v1/autenticacion/restablecer-clave",
                Map.of("token", tokenViejo, "claveNueva", "Restablecida7", "confirmacion", "Restablecida7")), 400);
        esperarEstado(enviar("/api/v1/autenticacion/restablecer-clave",
                Map.of("token", tokenDelUltimoCorreo(), "claveNueva", "Restablecida7", "confirmacion", "Restablecida7")), 204);
    }

    private static String token(MensajeCorreo mensaje) {
        Matcher m = Pattern.compile("token=([^&\\s]+)").matcher((String) mensaje.variables().get("enlace"));
        assertThat(m.find()).isTrue();
        return m.group(1);
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
