package co.edu.usco.goltime.torneos.dominio;

import java.util.List;

import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;

/**
 * Cómo se puntúa y se desempata un torneo: puntos por victoria/empate/derrota (por defecto 3/1/0),
 * criterios de desempate en orden y marcador de un W.O. (por defecto 3-0).
 */
public record ReglasPuntuacion(int puntosVictoria, int puntosEmpate, int puntosDerrota, List<CriterioDesempate> criterios,
        int marcadorWoGoles) {

    public static final List<CriterioDesempate> CRITERIOS_POR_DEFECTO = List.of(CriterioDesempate.PUNTOS, CriterioDesempate.DIFERENCIA_GOL,
            CriterioDesempate.GOLES_A_FAVOR, CriterioDesempate.ENFRENTAMIENTO_DIRECTO, CriterioDesempate.JUEGO_LIMPIO, CriterioDesempate.SORTEO);

    public ReglasPuntuacion {
        if (criterios == null || criterios.isEmpty()) {
            criterios = CRITERIOS_POR_DEFECTO;
        }
        criterios = List.copyOf(criterios.stream().distinct().toList());
        if (!criterios.contains(CriterioDesempate.PUNTOS)) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "El primer criterio de la tabla debe incluir los puntos.");
        }
        if (puntosVictoria < puntosEmpate || puntosEmpate < puntosDerrota) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Los puntos deben cumplir victoria >= empate >= derrota.");
        }
        if (marcadorWoGoles < 1) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "El marcador por W.O. debe ser de al menos 1 gol.");
        }
    }

    public static ReglasPuntuacion porDefecto() {
        return new ReglasPuntuacion(3, 1, 0, CRITERIOS_POR_DEFECTO, 3);
    }
}
