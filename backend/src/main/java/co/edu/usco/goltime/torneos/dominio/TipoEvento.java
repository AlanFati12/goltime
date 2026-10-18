package co.edu.usco.goltime.torneos.dominio;

public enum TipoEvento {
    GOL,
    GOL_PENAL,
    AUTOGOL,
    ASISTENCIA,
    AMARILLA,
    ROJA,
    FALTA;

    public boolean esGol() {
        return this == GOL || this == GOL_PENAL || this == AUTOGOL;
    }
}
