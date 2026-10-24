package co.edu.usco.goltime.reservas.dominio;

public enum OrigenReserva {
    WEB,
    ADMIN,
    TORNEO,
    RECURRENTE,
    /** Solicitud hecha por el bot de WhatsApp (ADR 0018). */
    WHATSAPP
}
