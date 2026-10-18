package co.edu.usco.goltime.torneos;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

class TorneoGruposIT extends BaseTorneosIT {

    private Map<String, Object> cuerpoGrupos() {
        Map<String, Object> cuerpo = cuerpoTorneo("GRUPOS_Y_ELIMINACION");
        cuerpo.put("numGrupos", 2);
        cuerpo.put("clasificadosPorGrupo", 1);
        return cuerpo;
    }

    @Test
    void losClasificadosOcupanLaFinalAlTerminarLosGrupos() throws Exception {
        List<Long> ids = torneoConEquipos(cuerpoGrupos(), 4);
        long torneoId = ids.get(0);
        generarFixture(torneoId);

        // serpentina: A = {1, 4}, B = {2, 3}
        List<Map<String, Object>> todos = partidos(torneoId);
        assertThat(todos).hasSize(3);
        Map<String, Object> fin = partidoDe(torneoId, "Final", 1);
        assertThat(((Map<?, ?>) fin.get("local")).get("etiqueta")).isEqualTo("1° Grupo A");
        assertThat(((Map<?, ?>) fin.get("visitante")).get("etiqueta")).isEqualTo("1° Grupo B");
        assertThat(((Map<?, ?>) fin.get("local")).get("inscripcionId")).isNull();

        MvcResult tablas = obtener("/api/v1/admin/torneos/" + torneoId + "/posiciones", admin);
        assertThat((List<?>) leer(tablas, "$")).hasSize(2);
        assertThat((String) leer(tablas, "$[0].grupo")).isEqualTo("A");

        iniciar(torneoId);
        List<Map<String, Object>> deGrupos = todos.stream().filter(p -> p.get("grupo") != null).toList();
        assertThat(deGrupos).hasSize(2);
        ganaElDeMenorId(deGrupos.get(0));
        // todavía falta un partido de grupo: la final sigue por definir
        assertThat(((Map<?, ?>) partidoDe(torneoId, "Final", 1).get("local")).get("inscripcionId")).isNull();
        ganaElDeMenorId(deGrupos.get(1));

        fin = partidoDe(torneoId, "Final", 1);
        assertThat(inscripcionLocal(fin)).isEqualTo(ids.get(1)); // 1 ganó el grupo A (1 vs 4)
        assertThat(inscripcionVisitante(fin)).isEqualTo(ids.get(2)); // 2 ganó el grupo B (2 vs 3)

        // corregir un resultado de grupo antes de jugar la final reacomoda la final
        long idB = idPartido(deGrupos.get(1));
        esperarEstado(postear("/api/v1/admin/partidos/" + idB + "/reabrir", admin, Map.of()), 200);
        assertThat(((Map<?, ?>) partidoDe(torneoId, "Final", 1).get("visitante")).get("inscripcionId")).isNull();
        boolean ganaLocal = inscripcionLocal(deGrupos.get(1)) > inscripcionVisitante(deGrupos.get(1));
        resultado(idB, ganaLocal ? 1 : 0, ganaLocal ? 0 : 1); // ahora gana el de mayor id (3)
        assertThat(inscripcionVisitante(partidoDe(torneoId, "Final", 1))).isEqualTo(ids.get(3));

        // jugada la final, la fase de grupos queda cerrada
        ganaElDeMenorId(partidoDe(torneoId, "Final", 1));
        esperarEstado(postear("/api/v1/admin/partidos/" + idB + "/reabrir", admin, Map.of()), 409);
        esperarEstado(estado(torneoId, "FINALIZADO", null), 200);
        MvcResult detalle = obtener("/api/v1/publico/torneos/" + torneoId, null);
        assertThat((String) leer(detalle, "$.torneo.campeon")).isNotBlank();
    }

    @Test
    void validaLaConfiguracionDeGrupos() throws Exception {
        Map<String, Object> sinGrupos = cuerpoTorneo("GRUPOS_Y_ELIMINACION");
        esperarEstado(postear("/api/v1/admin/torneos", admin, sinGrupos), 400);

        List<Long> ids = torneoConEquipos(cuerpoGrupos(), 3);
        MvcResult r = postear("/api/v1/admin/torneos/" + ids.get(0) + "/fixture", admin, Map.of());
        esperarEstado(r, 400); // 2 grupos necesitan al menos 4 equipos
    }
}
