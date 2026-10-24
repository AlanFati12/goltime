package co.edu.usco.goltime.autenticacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

import co.edu.usco.goltime.BaseIntegracion;
import co.edu.usco.goltime.autenticacion.aplicacion.ServicioVerificacionTelefono.Situacion;
import co.edu.usco.goltime.autenticacion.aplicacion.ServicioVerificacionTelefono;
import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.notificaciones.aplicacion.EnviadorCorreo.MensajeCorreo;
import co.edu.usco.goltime.notificaciones.aplicacion.EnviadorCorreo;

class VerificacionTelefonoIT extends BaseIntegracion {

    @MockitoBean
    EnviadorCorreo correo;

    @Autowired
    ServicioVerificacionTelefono verificacion;

    private String celular;

    @BeforeEach
    void preparar() {
        reset(correo);
        celular = "3" + ThreadLocalRandom.current().nextLong(100_000_000L, 999_999_999L);
    }

    private String registrarCon(String telefono) throws Exception {
        String correoNuevo = correoUnico();
        Map<String, Object> datos = new HashMap<>(datosRegistro(correoNuevo, CLAVE_VALIDA));
        datos.put("telefono", telefono);
        esperarEstado(enviar("/api/v1/autenticacion/registrar", datos), 201);
        return correoNuevo;
    }

    /** Código enviado en el último correo de verificación de WhatsApp dirigido a {@code destinatario}. */
    private String codigoEnviadoA(String destinatario) {
        ArgumentCaptor<MensajeCorreo> captor = ArgumentCaptor.forClass(MensajeCorreo.class);
        verify(correo, atLeastOnce()).enviar(captor.capture());
        return captor.getAllValues().stream()
                .filter(m -> m.plantilla().equals("verificar-telefono") && m.destinatario().equals(destinatario))
                .reduce((a, b) -> b).map(m -> (String) m.variables().get("codigo")).orElseThrow();
    }

    private Boolean verificado(String correoCuenta) {
        return jdbc.queryForObject("SELECT telefono_verificado FROM usuarios WHERE correo = ?", Boolean.class, correoCuenta);
    }

    @Test
    void elRegistroGuardaElWhatsappEnFormatoInternacional() throws Exception {
        String cuenta = registrarCon(celular.substring(0, 3) + " " + celular.substring(3));

        assertThat(jdbc.queryForObject("SELECT telefono FROM usuarios WHERE correo = ?", String.class, cuenta)).isEqualTo("+57" + celular);
        assertThat(verificado(cuenta)).isFalse();
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
    void conElCodigoDelCorreoElNumeroQuedaVinculadoALaCuenta() throws Exception {
        String cuenta = registrarCon(celular);
        assertThat(verificacion.identificar("+57" + celular).situacion()).isEqualTo(Situacion.SIN_VERIFICAR);

        var enviado = verificacion.solicitarCodigo("57" + celular);
        assertThat(enviado.correos()).containsExactly(cuenta.charAt(0) + "***@prueba.test");

        verificacion.confirmarCodigo(celular, codigoEnviadoA(cuenta));

        assertThat(verificado(cuenta)).isTrue();
        var identificacion = verificacion.identificar(celular);
        assertThat(identificacion.situacion()).isEqualTo(Situacion.CLIENTE);
        assertThat(identificacion.nombres()).isEqualTo("Ana");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_log WHERE accion = 'VERIFICAR_TELEFONO' AND entidad_id = ?", Integer.class,
                identificacion.usuarioId())).isEqualTo(1);
        // el código ya se usó
        assertThatThrownBy(() -> verificacion.confirmarCodigo(celular, codigoEnviadoA(cuenta))).isInstanceOf(ExcepcionNegocio.class);
    }

    @Test
    void unCodigoEquivocadoGastaIntentosYAlQuintoDejaDeServir() throws Exception {
        String cuenta = registrarCon(celular);
        verificacion.solicitarCodigo(celular);
        String bueno = codigoEnviadoA(cuenta);
        String malo = bueno.equals("000000") ? "111111" : "000000";

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> verificacion.confirmarCodigo(celular, malo))
                    .isInstanceOfSatisfying(ExcepcionNegocio.class, e -> assertThat(e.codigo()).isEqualTo(CodigoError.TOKEN_INVALIDO));
        }
        assertThatThrownBy(() -> verificacion.confirmarCodigo(celular, bueno)).isInstanceOf(ExcepcionNegocio.class);
        assertThat(verificado(cuenta)).isFalse();
    }

    @Test
    void siDosCuentasEscribieronElMismoNumeroSoloLaDelCodigoSeVinculaYLaOtraYaNoPuede() throws Exception {
        String suplantadora = registrarCon(celular);
        String duena = registrarCon(celular);

        assertThat(verificacion.solicitarCodigo(celular).correos()).hasSize(2);
        verificacion.confirmarCodigo(celular, codigoEnviadoA(duena));

        assertThat(verificado(duena)).isTrue();
        assertThat(verificado(suplantadora)).isFalse();
        assertThatThrownBy(() -> verificacion.solicitarCodigo(celular))
                .isInstanceOfSatisfying(ExcepcionNegocio.class, e -> assertThat(e.codigo()).isEqualTo(CodigoError.TELEFONO_YA_REGISTRADO));
        // y nadie más puede registrarse con ese número ya verificado
        Map<String, Object> datos = new HashMap<>(datosRegistro(correoUnico(), CLAVE_VALIDA));
        datos.put("telefono", "+57 " + celular);
        MvcResult tercera = enviar("/api/v1/autenticacion/registrar", datos);
        esperarEstado(tercera, 409);
        assertThat((String) JsonPath.read(tercera.getResponse().getContentAsString(), "$.codigo")).isEqualTo("TELEFONO_YA_REGISTRADO");
    }

    @Test
    void cambiarElNumeroEnElPerfilQuitaLaVerificacion() throws Exception {
        String cuenta = registrarCon(celular);
        verificacion.solicitarCodigo(celular);
        verificacion.confirmarCodigo(celular, codigoEnviadoA(cuenta));
        Sesion sesion = ingresar(cuenta, CLAVE_VALIDA, false);
        // mismo celular con el último dígito cambiado: siempre es un número distinto
        String otroCelular = celular.substring(0, 9) + (celular.charAt(9) == '9' ? '0' : (char) (celular.charAt(9) + 1));

        MvcResult edicion = mvc.perform(put("/api/v1/me").header("Authorization", "Bearer " + sesion.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("nombres", "Ana", "apellidos", "Pérez", "telefono", otroCelular))))
                .andReturn();

        esperarEstado(edicion, 200);
        assertThat((Boolean) JsonPath.read(edicion.getResponse().getContentAsString(), "$.telefonoVerificado")).isFalse();
        assertThat(verificacion.identificar(celular).situacion()).isEqualTo(Situacion.SIN_CUENTA);
    }

    @Test
    void unNumeroSinCuentaNoRecibeCodigoYSeLimitanLosCodigosPorHora() throws Exception {
        assertThat(verificacion.identificar(celular).situacion()).isEqualTo(Situacion.SIN_CUENTA);
        assertThatThrownBy(() -> verificacion.solicitarCodigo(celular))
                .isInstanceOfSatisfying(ExcepcionNegocio.class, e -> assertThat(e.codigo()).isEqualTo(CodigoError.RECURSO_NO_ENCONTRADO));

        registrarCon(celular);
        for (int i = 0; i < 3; i++) {
            verificacion.solicitarCodigo(celular);
        }
        assertThatThrownBy(() -> verificacion.solicitarCodigo(celular))
                .isInstanceOfSatisfying(ExcepcionNegocio.class, e -> assertThat(e.codigo()).isEqualTo(CodigoError.DEMASIADOS_INTENTOS));
        assertThat(verificacion.identificar(celular).situacion()).isEqualTo(Situacion.SIN_VERIFICAR);
    }
}
