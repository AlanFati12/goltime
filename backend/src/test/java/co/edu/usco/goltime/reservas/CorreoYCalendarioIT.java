package co.edu.usco.goltime.reservas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import co.edu.usco.goltime.notificaciones.aplicacion.EnviadorCorreo;
import co.edu.usco.goltime.notificaciones.aplicacion.EnviadorCorreo.MensajeCorreo;

class CorreoYCalendarioIT extends BaseReservasIT {

    @MockitoBean
    EnviadorCorreo enviador;

    private String admin;
    private long canchaId;
    private LocalDate fecha;

    @BeforeEach
    void preparar() throws Exception {
        admin = ingresarAdmin().token();
        canchaId = crearCancha(admin, crearSede(admin), "Cancha Correo");
        fecha = diaHabil(8);
    }

    private Sesion clienteVerificado() throws Exception {
        Sesion cliente = nuevoClienteConSesion();
        jdbc.update("UPDATE usuarios SET correo_verificado = TRUE WHERE id = ?", cliente.id());
        return cliente;
    }

    private String correoDe(Sesion cliente) {
        return jdbc.queryForObject("SELECT correo FROM usuarios WHERE id = ?", String.class, cliente.id());
    }

    private List<MensajeCorreo> enviadosA(String correo) {
        ArgumentCaptor<MensajeCorreo> captor = ArgumentCaptor.forClass(MensajeCorreo.class);
        verify(enviador, atLeastOnce()).enviar(captor.capture());
        return captor.getAllValues().stream().filter(m -> m.destinatario().equals(correo) && m.plantilla().equals("notificacion")).toList();
    }

    @Test
    void aprobarUnaReservaEnviaElCorreoConElEventoDeCalendarioAdjunto() throws Exception {
        Sesion cliente = clienteVerificado();
        long reservaId = solicitarOk(cliente.token(), canchaId, fecha, "19:00", "20:00");

        aprobar(admin, reservaId);

        List<MensajeCorreo> correos = enviadosA(correoDe(cliente));
        assertThat(correos).hasSize(1);
        MensajeCorreo correo = correos.get(0);
        assertThat(correo.asunto()).isEqualTo("¡Reserva confirmada!");
        assertThat(correo.plantilla()).isEqualTo("notificacion");
        assertThat(correo.adjuntos()).hasSize(1);
        var ics = correo.adjuntos().get(0);
        assertThat(ics.nombre()).endsWith(".ics");
        assertThat(ics.tipoContenido()).isEqualTo("text/calendar");
        String texto = new String(ics.contenido(), StandardCharsets.UTF_8);
        assertThat(texto).contains("BEGIN:VEVENT", "SUMMARY:Cancha Cancha Correo", "DTSTART:" + a(fecha, "19:00").toString().replace("-", "").replace(":", "").replace(".000", ""));
    }

    @Test
    void sinCorreoVerificadoNoSeEnviaNadaPeroLaNotificacionInAppSigue() throws Exception {
        Sesion cliente = nuevoClienteConSesion(); // sin verificar el correo
        long reservaId = solicitarOk(cliente.token(), canchaId, fecha, "19:00", "20:00");

        aprobar(admin, reservaId);

        assertThat(notificaciones(cliente.id(), "RESERVA_CONFIRMADA")).isEqualTo(1);
        org.mockito.Mockito.verify(enviador, org.mockito.Mockito.never()).enviar(org.mockito.ArgumentMatchers.argThat(m -> m != null && m.plantilla().equals("notificacion") && m.destinatario().equals(correoDe(cliente))));
    }

    @Test
    void elRechazoTambienLlegaPorCorreoPeroSinCalendario() throws Exception {
        Sesion cliente = clienteVerificado();
        long reservaId = solicitarOk(cliente.token(), canchaId, fecha, "19:00", "20:00");

        esperarEstado(postear("/api/v1/admin/reservas/" + reservaId + "/rechazar", admin, java.util.Map.of("motivo", "Cancha en mantenimiento")), 200);

        MensajeCorreo correo = enviadosA(correoDe(cliente)).get(0);
        assertThat(correo.asunto()).isEqualTo("Tu solicitud no fue aprobada");
        assertThat(correo.adjuntos()).isEmpty();
        assertThat(correo.variables().get("mensaje").toString()).contains("Cancha en mantenimiento");
    }

    @Test
    void laSolicitudDelClienteNoGeneraCorreoAlCliente() throws Exception {
        Sesion cliente = clienteVerificado();
        solicitarOk(cliente.token(), canchaId, fecha, "19:00", "20:00");

        verify(enviador, org.mockito.Mockito.never()).enviar(org.mockito.ArgumentMatchers.argThat(m -> m != null && m.plantilla().equals("notificacion") && m.destinatario().equals(correoDe(cliente))));
    }

    @Test
    void elClienteDescargaElIcsDeSuReservaYNoElDeOtro() throws Exception {
        Sesion cliente = nuevoClienteConSesion();
        Sesion otro = nuevoClienteConSesion();
        long reservaId = solicitarOk(cliente.token(), canchaId, fecha, "19:00", "20:00");

        MvcResult propio = obtener("/api/v1/reservas/" + reservaId + "/calendario.ics", cliente.token());
        esperarEstado(propio, 200);
        assertThat(propio.getResponse().getContentType()).startsWith("text/calendar");
        assertThat(propio.getResponse().getHeader("Content-Disposition")).contains(".ics");
        assertThat(propio.getResponse().getContentAsString(StandardCharsets.UTF_8)).contains("BEGIN:VCALENDAR", "END:VCALENDAR");

        esperarEstado(obtener("/api/v1/reservas/" + reservaId + "/calendario.ics", otro.token()), 404);
        esperarEstado(obtener("/api/v1/reservas/" + reservaId + "/calendario.ics", admin), 200);
        esperarEstado(obtener("/api/v1/reservas/" + reservaId + "/calendario.ics", null), 401);
    }
}
