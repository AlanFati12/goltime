package co.edu.usco.goltime.reservas.dominio;

import java.time.Instant;

/** Movimiento de dinero registrado por el admin sobre una reserva. */
public record Pago(Long id, long reservaId, int monto, MetodoPago metodo, String referencia, TipoPago tipo,
        long registradoPor, Instant fecha) {
}
