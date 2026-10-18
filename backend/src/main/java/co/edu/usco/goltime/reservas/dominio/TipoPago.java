package co.edu.usco.goltime.reservas.dominio;

public enum TipoPago {
    /** Pago que completa el valor de la reserva. */
    PAGO,
    /** Pago parcial. */
    ABONO,
    /** Devolución de dinero al cliente. */
    REEMBOLSO
}
