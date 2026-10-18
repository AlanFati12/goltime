package co.edu.usco.goltime.torneos.dominio;

public enum ModoEmparejamiento {
    /** El admin define el orden de las llaves o la composición de los grupos. */
    MANUAL,
    /** Sorteo. */
    ALEATORIO,
    /** Por cabezas de serie (la semilla de cada inscripción). */
    SEMBRADO
}
