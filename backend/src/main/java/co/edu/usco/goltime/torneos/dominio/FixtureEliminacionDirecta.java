package co.edu.usco.goltime.torneos.dominio;

import java.util.List;
import java.util.Map;

public class FixtureEliminacionDirecta implements GeneradorFixture {

    @Override
    public PlanFixture generar(ConfiguracionFixture c) {
        GeneradorFixture.exigirEquipos(c.inscripciones().size(), 2);
        List<Participante> sembrados = c.inscripciones().stream().map(Participante::de).toList();
        List<PartidoPlan> partidos = Eliminatoria.generar(sembrados, c.tercerPuesto(), 0);
        return new PlanFixture(List.of(new FasePlan(TipoFase.ELIMINATORIA, 1, "Eliminatoria", Map.of(), partidos)));
    }
}
