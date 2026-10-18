package co.edu.usco.goltime.torneos.dominio;

import java.util.List;
import java.util.Map;

public class FixtureLiga implements GeneradorFixture {

    @Override
    public PlanFixture generar(ConfiguracionFixture c) {
        GeneradorFixture.exigirEquipos(c.inscripciones().size(), 2);
        List<PartidoPlan> partidos = RondasLiga.generar(c.inscripciones(), c.idaYVuelta(), null, 0);
        return new PlanFixture(List.of(new FasePlan(TipoFase.LIGA, 1, "Liga", Map.of(), partidos)));
    }
}
