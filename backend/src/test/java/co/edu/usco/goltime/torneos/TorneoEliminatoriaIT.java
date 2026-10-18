package co.edu.usco.goltime.torneos;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

class TorneoEliminatoriaIT extends BaseTorneosIT {

    @Test
    void cincoEquiposConDescansosPenalesTercerPuestoYPodio() throws Exception {
        Map<String, Object> cuerpo = cuerpoTorneo("ELIMINACION_DIRECTA");
        cuerpo.put("tercerPuesto", true);
        List<Long> ids = torneoConEquipos(cuerpo, 5);
        long torneoId = ids.get(0);
        long i1 = ids.get(1);
        long i2 = ids.get(2);
        long i3 = ids.get(3);
        long i4 = ids.get(4);
        long i5 = ids.get(5);

        generarFixture(torneoId);
        List<Map<String, Object>> todos = partidos(torneoId);
        assertThat(todos).hasSize(8); // 4 cuartos + 2 semis + final + tercer puesto
        assertThat(todos.stream().filter(p -> Boolean.TRUE.equals(p.get("descanso")))).hasSize(3);

        // los descansos ya pasaron a semifinales: la segunda semifinal quedó 2 vs 3
        Map<String, Object> semi2 = partidoDe(torneoId, "Semifinal", 2);
        assertThat(inscripcionLocal(semi2)).isEqualTo(i2);
        assertThat(inscripcionVisitante(semi2)).isEqualTo(i3);
        Map<String, Object> semi1 = partidoDe(torneoId, "Semifinal", 1);
        assertThat(inscripcionLocal(semi1)).isEqualTo(i1);
        assertThat(semi1.get("visitante")).isNull(); // espera al ganador de 4 vs 5

        iniciar(torneoId);
        Map<String, Object> cuartos = todos.stream().filter(p -> !Boolean.TRUE.equals(p.get("descanso")) && "Cuartos de final".equals(p.get("nombreRonda")))
                .findFirst().orElseThrow();
        long idCuartos = idPartido(cuartos);
        assertThat(inscripcionLocal(cuartos)).isEqualTo(i4);
        assertThat(inscripcionVisitante(cuartos)).isEqualTo(i5);

        // empate sin penales, penales empatados o penales con marcador desigual: no valen
        esperarEstado(resultadoCrudo(idCuartos, 1, 1, null, null), 400);
        esperarEstado(resultadoCrudo(idCuartos, 1, 1, 3, 3), 400);
        esperarEstado(resultadoCrudo(idCuartos, 2, 1, 4, 3), 400);
        // el 5 gana por penales
        esperarEstado(resultadoCrudo(idCuartos, 1, 1, 3, 4), 200);
        semi1 = partidoDe(torneoId, "Semifinal", 1);
        assertThat(inscripcionVisitante(semi1)).isEqualTo(i5);

        // corregir: gana el 4 -> la semifinal se actualiza mientras no se haya jugado
        esperarEstado(resultadoCrudo(idCuartos, 1, 1, 5, 4), 200);
        assertThat(inscripcionVisitante(partidoDe(torneoId, "Semifinal", 1))).isEqualTo(i4);

        ganaElDeMenorId(partidoDe(torneoId, "Semifinal", 1)); // 1 gana a 4
        ganaElDeMenorId(partidoDe(torneoId, "Semifinal", 2)); // 2 gana a 3

        // ya se jugó la semifinal: no se puede cambiar quién ganó el cuarto
        esperarEstado(resultadoCrudo(idCuartos, 1, 1, 4, 5), 409);
        esperarEstado(postear("/api/v1/admin/partidos/" + idCuartos + "/reabrir", admin, Map.of()), 409);

        Map<String, Object> tercero = partidoDe(torneoId, "Tercer puesto", 2);
        assertThat(inscripcionLocal(tercero)).isEqualTo(i4);
        assertThat(inscripcionVisitante(tercero)).isEqualTo(i3);
        Map<String, Object> fin = partidoDe(torneoId, "Final", 1);
        assertThat(inscripcionLocal(fin)).isEqualTo(i1);
        assertThat(inscripcionVisitante(fin)).isEqualTo(i2);

        esperarEstado(estado(torneoId, "FINALIZADO", null), 409); // faltan final y tercer puesto
        ganaElDeMenorId(tercero); // 3 gana a 4
        ganaElDeMenorId(fin); // 1 gana a 2
        esperarEstado(estado(torneoId, "FINALIZADO", null), 200);

        MvcResult detalle = obtener("/api/v1/publico/torneos/" + torneoId, null);
        long campeon = ((Number) leer(detalle, "$.torneo.campeonId")).longValue();
        long sub = ((Number) leer(detalle, "$.torneo.subcampeonId")).longValue();
        long tercer = ((Number) leer(detalle, "$.torneo.terceroId")).longValue();
        assertThat(equipoDe(i1)).isEqualTo(campeon);
        assertThat(equipoDe(i2)).isEqualTo(sub);
        assertThat(equipoDe(i3)).isEqualTo(tercer);
    }

    private long equipoDe(long inscripcionId) {
        return jdbc.queryForObject("SELECT equipo_id FROM inscripciones_torneo WHERE id = ?", Long.class, inscripcionId);
    }

    @Test
    void ochoEquiposSinDescansosYElMarcadorNoPuedeSerNegativo() throws Exception {
        List<Long> ids = torneoConEquipos(cuerpoTorneo("ELIMINACION_DIRECTA"), 8);
        long torneoId = ids.get(0);
        generarFixture(torneoId);
        List<Map<String, Object>> todos = partidos(torneoId);
        assertThat(todos).hasSize(7);
        assertThat(todos).noneMatch(p -> Boolean.TRUE.equals(p.get("descanso")));
        // 1 contra 8, 4 contra 5, 2 contra 7, 3 contra 6
        Map<String, Object> primero = partidoDe(torneoId, "Cuartos de final", 1);
        assertThat(inscripcionLocal(primero)).isEqualTo(ids.get(1));
        assertThat(inscripcionVisitante(primero)).isEqualTo(ids.get(8));

        iniciar(torneoId);
        esperarEstado(resultadoCrudo(idPartido(primero), -1, 0, null, null), 400);
        // un partido sin rivales definidos no se puede jugar
        esperarEstado(resultadoCrudo(idPartido(partidoDe(torneoId, "Final", 1)), 1, 0, null, null), 409);
    }

    @Test
    void walkoverDaElMarcadorConfiguradoYAvanzaAlGanador() throws Exception {
        Map<String, Object> cuerpo = cuerpoTorneo("ELIMINACION_DIRECTA");
        cuerpo.put("marcadorWoGoles", 5);
        List<Long> ids = torneoConEquipos(cuerpo, 4);
        long torneoId = ids.get(0);
        generarFixture(torneoId);
        iniciar(torneoId);
        Map<String, Object> semi = partidoDe(torneoId, "Semifinal", 1); // 1 vs 4
        esperarEstado(postear("/api/v1/admin/partidos/" + idPartido(semi) + "/wo", admin, Map.of("ganador", "VISITANTE", "motivo", "No se presentó el local")), 200);

        MvcResult r = obtener("/api/v1/admin/partidos/" + idPartido(semi), admin);
        assertThat((String) leer(r, "$.partido.estado")).isEqualTo("WO");
        assertThat((Integer) leer(r, "$.partido.golesLocal")).isZero();
        assertThat((Integer) leer(r, "$.partido.golesVisitante")).isEqualTo(5);
        Map<String, Object> fin = partidoDe(torneoId, "Final", 1);
        assertThat(inscripcionLocal(fin)).isEqualTo(ids.get(4));
    }
}
