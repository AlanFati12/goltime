package co.edu.usco.goltime.reservas.dominio;

public enum MotivoBloqueo {
    MANTENIMIENTO("Mantenimiento"),
    EVENTO_PRIVADO("Evento privado"),
    TORNEO("Torneo"),
    CLIMA("Clima"),
    OTRO("Otro");

    private final String etiqueta;

    MotivoBloqueo(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String etiqueta() {
        return etiqueta;
    }
}
