package co.edu.usco.goltime.torneos.dominio;

import java.util.Set;

/** BORRADOR -> INSCRIPCIONES -> EN_CURSO -> FINALIZADO; CANCELADO desde cualquier estado no final. */
public enum EstadoTorneo {
    BORRADOR,
    INSCRIPCIONES,
    EN_CURSO,
    FINALIZADO,
    CANCELADO;

    public Set<EstadoTorneo> siguientes() {
        return switch (this) {
            case BORRADOR -> Set.of(INSCRIPCIONES, EN_CURSO, CANCELADO);
            case INSCRIPCIONES -> Set.of(EN_CURSO, CANCELADO);
            case EN_CURSO -> Set.of(FINALIZADO, CANCELADO);
            case FINALIZADO, CANCELADO -> Set.of();
        };
    }

    /** Se muestran públicamente todos menos los borradores. */
    public boolean esPublico() {
        return this != BORRADOR;
    }

    public boolean permiteInscripciones() {
        return this == BORRADOR || this == INSCRIPCIONES;
    }
}
