package co.edu.usco.goltime.reservas.dominio;

/** La reserva dejó de ocupar el horario sin jugarse (cancelada, rechazada o expirada): se liberan sus descuentos. */
public record ReservaLiberadaEvento(long reservaId, Long clienteId) {
}
