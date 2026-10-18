package co.edu.usco.goltime.torneos.dominio;

import java.util.List;

public record PlanFixture(List<FasePlan> fases) {

    public List<PartidoPlan> todosLosPartidos() {
        return fases.stream().flatMap(f -> f.partidos().stream()).toList();
    }
}
