package co.edu.usco.goltime.reservas.dominio;

import java.time.Instant;

/** Un cliente solicitó una reserva (por ejemplo, para cumplir lo que esperaba en la lista de espera). */
public record ReservaCreadaEvento(long reservaId, long clienteId, long canchaId, Instant inicio, Instant fin) {
}
