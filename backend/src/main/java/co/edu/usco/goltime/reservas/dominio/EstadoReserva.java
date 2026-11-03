package co.edu.usco.goltime.reservas.dominio;

/** Estados de una reserva. Las activas ({@link #ocupaHorario()}) retienen el horario de la cancha. */
public enum EstadoReserva {
    PENDIENTE("pendiente de aprobación"),
    CONFIRMADA("confirmada"),
    RECHAZADA("rechazada"),
    EXPIRADA("expirada"),
    CANCELADA("cancelada"),
    COMPLETADA("completada"),
    NO_ASISTIO("no asistió");

    private final String etiqueta;

    EstadoReserva(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Nombre para mostrar al usuario, en minúscula: "pendiente de aprobación", "no asistió"… */
    public String etiqueta() {
        return etiqueta;
    }

    /** Una reserva pendiente o confirmada retiene el horario; las demás lo liberan o ya pasaron. */
    public boolean ocupaHorario() {
        return this == PENDIENTE || this == CONFIRMADA;
    }

    /** Estados que liberan el horario al ocurrir. */
    public boolean liberaHorario() {
        return this == RECHAZADA || this == EXPIRADA || this == CANCELADA;
    }

    /**
     * Estados que conservan su fila en {@code ocupaciones_cancha}: las activas y las ya jugadas (COMPLETADA,
     * NO_ASISTIO), que siguen contando como horario usado aunque el administrador las marque antes de tiempo.
     */
    public boolean conservaOcupacion() {
        return !liberaHorario();
    }
}
