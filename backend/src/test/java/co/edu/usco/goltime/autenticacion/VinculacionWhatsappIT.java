package co.edu.usco.goltime.autenticacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

import co.edu.usco.goltime.BaseIntegracion;

/** WhatsApp en el registro (E.164) y vinculación con el bot mediante el código que se genera en el perfil. */
class VinculacionWhatsappIT extends BaseIntegracion {

    private static final String CLAVE = "clave-de-integracion-solo-para-pruebas-0123456789";
    private static final String CONFIRMAR = "/api/v1/integracion/whatsapp/vinculacion/confirmar";

    private String celular;

    @BeforeEach
    void preparar() {
        celular = "3" + ThreadLocalRandom.current().nextLong(100_000_000L, 999_999_999L);
    }

    private String registrarCon(String telefono) throws Exception {
        String correoNuevo = correoUnico();
        Map<String, Object> datos = new HashMap<>(datosRegistro(correoNuevo, CLAVE_VALIDA));
        datos.put("telefono", telefono);
        esperarEstado(enviar("/api/v1/autenticacion/registrar", datos), 201);
        return correoNuevo;
    }

    private String generarCodigo(Sesion sesion) throws Exception {
        MvcResult r = mvc.perform(post("/api/v1/me/whatsapp/codigo").header("Authorization", "Bearer " + sesion.token())).andReturn();
        esperarEstado(r, 200);
        return JsonPath.read(r.getResponse().getContentAsString(), "$.codigo");
    }

    /** El bot envía el código con el número (o el @lid) de quien escribe. */
    private MvcResult confirmar(String cabecera, String valor, String codigo) throws Exception {
        return mvc.perform(post(CONFIRMAR).header("X-Clave-Integracion", CLAVE).header(cabecera, valor)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("codigo", codigo)))).andReturn();
    }

    private Map<String, Object> estado(String correoCuenta) {
        return jdbc.queryForMap("SELECT telefono, telefono_verificado, whatsapp_lid FROM usuarios WHERE correo = ?", correoCuenta);
    }

    @Test
    void elRegistroGuardaElWhatsappEnFormatoInternacionalYSinVerificar() throws Exception {
        String cuenta = registrarCon(celular.substring(0, 3) + " " + celular.substring(3));
        assertThat(estado(cuenta)).containsEntry("telefono", "+57" + celular).containsEntry("telefono_verificado", false);
    }

    @Test
    void elRegistroRechazaUnWhatsappInvalidoOAusente() throws Exception {
        Map<String, Object> datos = new HashMap<>(datosRegistro(correoUnico(), CLAVE_VALIDA));
        datos.put("telefono", "8712345");
        MvcResult invalido = enviar("/api/v1/autenticacion/registrar", datos);
        esperarEstado(invalido, 400);
        assertThat((String) JsonPath.read(invalido.getResponse().getContentAsString(), "$.codigo")).isEqualTo("DATOS_INVALIDOS");
        datos.remove("telefono");
        esperarEstado(enviar("/api/v1/autenticacion/registrar", datos), 400);
    }

    @Test
    void conElCodigoDelPerfilElWhatsappQuedaVinculadoYElCodigoSirveUnaVez() throws Exception {
        String cuenta = registrarCon(celular);
        String codigo = generarCodigo(ingresar(cuenta, CLAVE_VALIDA, false));
        assertThat(codigo).hasSize(6);

        // escribiéndolo en minúsculas y con espacios también sirve; vincula el WhatsApp desde el que llegó
        String otroCelular = "+573" + ThreadLocalRandom.current().nextLong(100_000_000L, 999_999_999L);
        MvcResult r = confirmar("X-WhatsApp-Numero", otroCelular, codigo.toLowerCase().substring(0, 3) + " " + codigo.substring(3));
        esperarEstado(r, 200);
        assertThat(estado(cuenta)).containsEntry("telefono", otroCelular).containsEntry("telefono_verificado", true);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_log WHERE accion = 'VINCULAR_WHATSAPP' AND entidad_id = "
                + "(SELECT id FROM usuarios WHERE correo = ?)", Integer.class, cuenta)).isEqualTo(1);
        esperarEstado(confirmar("X-WhatsApp-Numero", otroCelular, codigo), 400); // ya se usó
    }

    @Test
    void desdeUnLidSeGuardaElLidYUnCodigoNuevoAnulaElAnterior() throws Exception {
        String cuenta = registrarCon(celular);
        Sesion sesion = ingresar(cuenta, CLAVE_VALIDA, false);
        String viejo = generarCodigo(sesion);
        String nuevo = generarCodigo(sesion);
        esperarEstado(confirmar("X-WhatsApp-Lid", "164295400784036@lid", viejo), 400);
        esperarEstado(confirmar("X-WhatsApp-Lid", "164295400784036@lid", nuevo), 200);
        assertThat(estado(cuenta)).containsEntry("whatsapp_lid", "164295400784036").containsEntry("telefono_verificado", false);
        jdbc.update("UPDATE usuarios SET whatsapp_lid = NULL WHERE correo = ?", cuenta); // el lid fijo no estorba a otras pruebas
    }

    @Test
    void unWhatsappVinculadoNoSeVinculaAOtraCuenta() throws Exception {
        String telefono = "+57" + celular;
        String primera = registrarCon(celular);
        esperarEstado(confirmar("X-WhatsApp-Numero", telefono, generarCodigo(ingresar(primera, CLAVE_VALIDA, false))), 200);

        String segunda = registrarCon("3" + celular.substring(1, 9) + (celular.charAt(9) == '9' ? '0' : (char) (celular.charAt(9) + 1)));
        MvcResult r = confirmar("X-WhatsApp-Numero", telefono, generarCodigo(ingresar(segunda, CLAVE_VALIDA, false)));
        esperarEstado(r, 409);
        assertThat((String) JsonPath.read(r.getResponse().getContentAsString(), "$.codigo")).isEqualTo("TELEFONO_YA_REGISTRADO");
        // ni registrarse con ese número ya vinculado
        Map<String, Object> datos = new HashMap<>(datosRegistro(correoUnico(), CLAVE_VALIDA));
        datos.put("telefono", telefono);
        esperarEstado(enviar("/api/v1/autenticacion/registrar", datos), 409);
    }

    @Test
    void cincoCodigosEquivocadosBloqueanEseWhatsappUnaHora() throws Exception {
        String cuenta = registrarCon(celular);
        String bueno = generarCodigo(ingresar(cuenta, CLAVE_VALIDA, false));
        String telefono = "+57" + celular;
        for (int i = 0; i < 5; i++) {
            esperarEstado(confirmar("X-WhatsApp-Numero", telefono, i % 2 == 0 ? "AAAAAA" : "12"), 400);
        }
        MvcResult bloqueado = confirmar("X-WhatsApp-Numero", telefono, bueno);
        esperarEstado(bloqueado, 429);
        assertThat(estado(cuenta)).containsEntry("telefono_verificado", false);
    }

    @Test
    void cambiarElNumeroQuitaLaVerificacionYDesvincularQuitaTodo() throws Exception {
        String cuenta = registrarCon(celular);
        Sesion sesion = ingresar(cuenta, CLAVE_VALIDA, false);
        esperarEstado(confirmar("X-WhatsApp-Numero", "+57" + celular, generarCodigo(sesion)), 200);

        MvcResult edicion = mvc.perform(put("/api/v1/me").header("Authorization", "Bearer " + sesion.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("nombres", "Ana", "apellidos", "Pérez", "telefono", "3" + celular.substring(1, 9)
                        + (celular.charAt(9) == '9' ? '0' : (char) (celular.charAt(9) + 1))))))
                .andReturn();
        esperarEstado(edicion, 200);
        assertThat((Boolean) JsonPath.read(edicion.getResponse().getContentAsString(), "$.whatsappVinculado")).isFalse();

        esperarEstado(confirmar("X-WhatsApp-Lid", "987654321012345", generarCodigo(sesion)), 200);
        MvcResult sinVincular = mvc.perform(delete("/api/v1/me/whatsapp").header("Authorization", "Bearer " + sesion.token())).andReturn();
        esperarEstado(sinVincular, 200);
        assertThat((Boolean) JsonPath.read(sinVincular.getResponse().getContentAsString(), "$.whatsappVinculado")).isFalse();
        assertThat(estado(cuenta).get("whatsapp_lid")).isNull();
    }

    @Test
    void elAdministradorNoGeneraCodigos() throws Exception {
        MvcResult r = mvc.perform(post("/api/v1/me/whatsapp/codigo").header("Authorization", "Bearer " + ingresarAdmin().token())).andReturn();
        esperarEstado(r, 403);
    }
}
