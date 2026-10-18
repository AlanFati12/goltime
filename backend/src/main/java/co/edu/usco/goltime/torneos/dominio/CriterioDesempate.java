package co.edu.usco.goltime.torneos.dominio;

/** Criterios para ordenar la tabla, aplicados en el orden configurado en el torneo. */
public enum CriterioDesempate {
    PUNTOS,
    DIFERENCIA_GOL,
    GOLES_A_FAVOR,
    /** Solo los partidos entre los equipos empatados. */
    ENFRENTAMIENTO_DIRECTO,
    /** Menos tarjetas: amarilla = 1, roja = 3. */
    JUEGO_LIMPIO,
    /** Sorteo determinista (estable entre consultas). */
    SORTEO
}
