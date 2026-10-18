package co.edu.usco.goltime.reservas.dominio;

/** Estados de una reserva. Las activas ({@link #ocupaHorario()}) retienen el horario de la cancha. */
public enum EstadoReserva {
    PENDIENTE,
    CONFIRMADA,
    RECHAZADA,
    EXPIRADA,
    CANCELADA,
    COMPLETADA,
    NO_ASISTIO;

    /** Una reserva pendiente o confirmada retiene el horario; las demás lo liberan o ya pasaron. */
    public boolean ocupaHorario() {
        return this == PENDIENTE || this == CONFIRMADA;
    }

    /** Estados que liberan el horario al ocurrir. */
    public boolean liberaHorario() {
        return this == RECHAZADA || this == EXPIRADA || this == CANCELADA;
    }
}
