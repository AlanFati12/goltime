package co.edu.usco.goltime.torneos.aplicacion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import co.edu.usco.goltime.torneos.aplicacion.Modelos.Fase;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.Grupo;
import co.edu.usco.goltime.torneos.dominio.CalculadoraPosiciones;
import co.edu.usco.goltime.torneos.dominio.CalculadoraPosiciones.FilaPosicion;
import co.edu.usco.goltime.torneos.dominio.CalculadoraPosiciones.PartidoJugado;
import co.edu.usco.goltime.torneos.dominio.Partido;
import co.edu.usco.goltime.torneos.dominio.TipoFase;
import co.edu.usco.goltime.torneos.dominio.Torneo;

/** Arma las tablas de posiciones de un torneo (una por liga o una por grupo) a partir de los partidos jugados. */
@Component
public class CalculoTablas {

    public record TablaPosiciones(long faseId, String faseNombre, Long grupoId, String grupo, List<FilaPosicion> filas) {
    }

    /**
     * @param inscripcionesLiga inscripciones aprobadas (solo se usan en formato liga, donde no hay grupos)
     */
    public List<TablaPosiciones> calcular(Torneo torneo, List<Fase> fases, List<Grupo> grupos, List<Partido> partidos, Map<Long, Integer> disciplina,
            List<Long> inscripcionesLiga) {
        List<TablaPosiciones> tablas = new ArrayList<>();
        for (Fase fase : fases) {
            if (fase.tipo() == TipoFase.LIGA) {
                tablas.add(new TablaPosiciones(fase.id(), fase.nombre(), null, null,
                        CalculadoraPosiciones.calcular(inscripcionesLiga, jugados(partidos, fase.id(), null), disciplina, torneo.reglas())));
            } else if (fase.tipo() == TipoFase.GRUPOS) {
                for (Grupo g : grupos) {
                    if (g.faseId() == fase.id()) {
                        tablas.add(new TablaPosiciones(fase.id(), fase.nombre(), g.id(), g.nombre(),
                                CalculadoraPosiciones.calcular(g.inscripcionIds(), jugados(partidos, fase.id(), g.id()), disciplina, torneo.reglas())));
                    }
                }
            }
        }
        return tablas;
    }

    private static List<PartidoJugado> jugados(List<Partido> partidos, long faseId, Long grupoId) {
        return partidos.stream()
                .filter(p -> p.faseId() == faseId && p.jugado() && (grupoId == null || grupoId.equals(p.grupoId())))
                .sorted(Comparator.comparing(Partido::inicio, Comparator.nullsLast(Comparator.naturalOrder())).thenComparingInt(Partido::ronda)
                        .thenComparingInt(Partido::numeroLlave))
                .map(p -> new PartidoJugado(p.localId(), p.visitanteId(), p.golesLocal(), p.golesVisitante()))
                .toList();
    }
}
