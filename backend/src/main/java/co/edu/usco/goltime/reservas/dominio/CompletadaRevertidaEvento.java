package co.edu.usco.goltime.reservas.dominio;

/** Una reserva COMPLETADA se corrigió (por ejemplo a NO_ASISTIO): ya no cuenta para premios. */
public record CompletadaRevertidaEvento(long reservaId, Long clienteId) {
}
