package co.edu.usco.goltime.torneos.dominio;

import java.util.List;
import java.util.Map;

/** Una fase del fixture con sus grupos (nombre de grupo -> inscripciones) y partidos. */
public record FasePlan(TipoFase tipo, int orden, String nombre, Map<String, List<Long>> grupos, List<PartidoPlan> partidos) {
}
