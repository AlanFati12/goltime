package co.edu.usco.goltime.reservas.dominio;

/** Una reserva pasó a COMPLETADA: cuenta para ingresos reales y, más adelante, para el programa de premios. */
public record ReservaCompletadaEvento(long reservaId, Long clienteId, long canchaId, int duracionMin) {
}
