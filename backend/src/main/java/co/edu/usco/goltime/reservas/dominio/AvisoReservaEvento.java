package co.edu.usco.goltime.reservas.dominio;

import java.time.Instant;

/**
 * Se le avisó al titular de una reserva de un cambio (aprobada, rechazada, expirada, cancelada o movida por el
 * administrador, cualquier cambio de estado del administrador ({@code RESERVA_ESTADO_<ESTADO>}, ADR 0019) o el
 * recordatorio). Otros canales, como WhatsApp (ADR 0018), lo escuchan para avisar también. {@code tipo} es el mismo de
 * la notificación de la web; {@code motivo} viene en rechazos, cancelaciones y cambios de estado del administrador.
 */
public record AvisoReservaEvento(long reservaId, String codigo, String tipo, Long clienteId, Long clienteMostradorId, OrigenReserva origen, String telefono,
        String cancha, Instant inicio, String motivo) {
}
