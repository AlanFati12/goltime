package co.edu.usco.goltime.torneos.dominio;

public enum EstadoPartido {
    /** Sin fecha o con rivales por definir. */
    PENDIENTE,
    PROGRAMADO,
    EN_JUEGO,
    FINALIZADO,
    APLAZADO,
    /** Ganado por no presentación del rival. */
    WO,
    /** El equipo avanza sin jugar (no cuenta en estadísticas). */
    BYE;

    /** ¿Tiene resultado definitivo? */
    public boolean jugado() {
        return this == FINALIZADO || this == WO;
    }
}
