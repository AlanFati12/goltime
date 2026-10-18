package co.edu.usco.goltime.torneos.dominio;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;

/**
 * Fase de grupos (todos contra todos en cada grupo) y eliminatoria entre los clasificados. Los equipos se reparten
 * en serpentina según la siembra para equilibrar los grupos. Las llaves quedan con referencias del tipo
 * "1° Grupo A" que se resuelven al terminar la fase de grupos.
 */
public class FixtureGruposYEliminacion implements GeneradorFixture {

    @Override
    public PlanFixture generar(ConfiguracionFixture c) {
        int grupos = c.numGrupos();
        int clasifican = c.clasificadosPorGrupo();
        if (grupos < 2 || clasifican < 1) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Indica al menos 2 grupos y cuántos equipos clasifican de cada uno.");
        }
        GeneradorFixture.exigirEquipos(c.inscripciones().size(), grupos * 2);
        if (c.inscripciones().size() / grupos < clasifican) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Hay menos equipos por grupo que clasificados por grupo.");
        }
        if (grupos * clasifican < 2) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "La eliminatoria necesita al menos 2 clasificados.");
        }

        Map<String, List<Long>> repartidos = repartir(c.inscripciones(), grupos);
        List<PartidoPlan> partidosGrupos = new ArrayList<>();
        int clave = 0;
        for (Map.Entry<String, List<Long>> g : repartidos.entrySet()) {
            List<PartidoPlan> liga = RondasLiga.generar(g.getValue(), c.idaYVuelta(), g.getKey(), clave);
            partidosGrupos.addAll(liga);
            clave += liga.size();
        }

        List<Participante> clasificados = new ArrayList<>();
        for (int puesto = 1; puesto <= clasifican; puesto++) {
            for (String nombre : repartidos.keySet()) {
                clasificados.add(Participante.porDefinir(puesto + "° Grupo " + nombre, referencia(nombre, puesto)));
            }
        }
        List<PartidoPlan> llaves = Eliminatoria.generar(clasificados, c.tercerPuesto(), clave);
        return new PlanFixture(List.of(new FasePlan(TipoFase.GRUPOS, 1, "Fase de grupos", repartidos, partidosGrupos),
                new FasePlan(TipoFase.ELIMINATORIA, 2, "Eliminatoria", Map.of(), llaves)));
    }

    /** Referencia "G:A:1" = primer puesto del grupo A. */
    public static String referencia(String grupo, int puesto) {
        return "G:" + grupo + ":" + puesto;
    }

    /** Reparto en serpentina: A B C C B A A B C... */
    static Map<String, List<Long>> repartir(List<Long> ordenados, int grupos) {
        Map<String, List<Long>> resultado = new LinkedHashMap<>();
        for (int g = 0; g < grupos; g++) {
            resultado.put(nombreGrupo(g), new ArrayList<>());
        }
        List<String> nombres = new ArrayList<>(resultado.keySet());
        for (int i = 0; i < ordenados.size(); i++) {
            int vuelta = i / grupos;
            int pos = i % grupos;
            int g = vuelta % 2 == 0 ? pos : grupos - 1 - pos;
            resultado.get(nombres.get(g)).add(ordenados.get(i));
        }
        return resultado;
    }

    static String nombreGrupo(int indice) {
        return String.valueOf((char) ('A' + indice));
    }
}
