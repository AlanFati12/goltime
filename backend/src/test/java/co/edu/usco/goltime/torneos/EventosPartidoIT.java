package co.edu.usco.goltime.torneos;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

class EventosPartidoIT extends BaseTorneosIT {

    private Map<String, Object> evento(long inscripcion, long jugador, String tipo, Integer minuto, Long relacionado) {
        Map<String, Object> m = new HashMap<>();
        m.put("inscripcionId", inscripcion);
        m.put("jugadorId", jugador);
        m.put("tipo", tipo);
        m.put("minuto", minuto);
        m.put("eventoRelacionadoId", relacionado);
        return m;
    }

    private MvcResult agregar(long partido, Map<String, Object> evento) throws Exception {
        return postear("/api/v1/admin/partidos/" + partido + "/eventos", admin, evento);
    }

    @Test
    void golesAsistenciasAutogolesYEstadisticas() throws Exception {
        List<Long> ids = torneoConEquipos(cuerpoTorneo("LIGA"), 2);
        long torneoId = ids.get(0);
        long insLocal;
        long insVisita;
        generarFixture(torneoId);
        iniciar(torneoId);
        Map<String, Object> p = partidos(torneoId).get(0);
        long partido = idPartido(p);
        insLocal = inscripcionLocal(p);
        insVisita = inscripcionVisitante(p);
        List<Long> deLocal = jugadoresDe(torneoId, insLocal);
        List<Long> deVisita = jugadoresDe(torneoId, insVisita);

        MvcResult gol1 = agregar(partido, evento(insLocal, deLocal.get(0), "GOL", 10, null));
        esperarEstado(gol1, 201);
        long idGol1 = ((Number) leer(gol1, "$.eventos[0].id")).longValue();

        // asistencia: del mismo equipo, otro jugador, ligada a un gol
        esperarEstado(agregar(partido, evento(insLocal, deLocal.get(0), "ASISTENCIA", 10, idGol1)), 400); // el mismo goleador
        esperarEstado(agregar(partido, evento(insVisita, deVisita.get(0), "ASISTENCIA", 10, idGol1)), 400); // del otro equipo
        esperarEstado(agregar(partido, evento(insLocal, deLocal.get(1), "ASISTENCIA", 10, idGol1)), 201);
        // un jugador de otro equipo no puede figurar en el evento
        esperarEstado(agregar(partido, evento(insLocal, deVisita.get(0), "GOL", 12, null)), 400);
        // equipo que no juega el partido
        esperarEstado(agregar(partido, evento(ids.get(1) == insLocal ? ids.get(2) : ids.get(1), deLocal.get(0), "GOL", 12, null)), 400);

        esperarEstado(agregar(partido, evento(insLocal, deLocal.get(1), "GOL_PENAL", 30, null)), 201);
        esperarEstado(agregar(partido, evento(insVisita, deVisita.get(0), "AUTOGOL", 40, null)), 201); // suma al local
        esperarEstado(agregar(partido, evento(insVisita, deVisita.get(1), "AMARILLA", 50, null)), 201);
        esperarEstado(agregar(partido, evento(insVisita, deVisita.get(1), "ROJA", 60, null)), 201);

        // el marcador no puede ser menor que los goles ya registrados (3 del local)
        esperarEstado(resultadoCrudo(partido, 2, 0, null, null), 400);
        esperarEstado(resultadoCrudo(partido, 3, 1, null, null), 200); // 1 gol del visitante sin detallar
        // ya con resultado: un cuarto gol del local supera el marcador
        esperarEstado(agregar(partido, evento(insLocal, deLocal.get(0), "GOL", 70, null)), 400);
        esperarEstado(agregar(partido, evento(insVisita, deVisita.get(0), "GOL", 71, null)), 201); // el gol que faltaba del visitante
        esperarEstado(agregar(partido, evento(insVisita, deVisita.get(0), "GOL", 72, null)), 400);

        // no se puede borrar un gol que tiene asistencia sin quitar antes la asistencia
        esperarEstado(borrar("/api/v1/admin/partidos/" + partido + "/eventos/" + idGol1, admin), 409);

        MvcResult detalle = obtener("/api/v1/publico/partidos/" + partido, null);
        assertThat((List<?>) leer(detalle, "$.eventos")).hasSize(7);

        MvcResult stats = obtener("/api/v1/publico/torneos/" + torneoId + "/estadisticas", null);
        List<Integer> goles = leer(stats, "$.goleadores[*].goles");
        assertThat(goles).containsExactly(1, 1, 1); // dos del local (gol y penal) por jugadores distintos + 1 del visitante
        List<Integer> rojas = leer(stats, "$.tarjetas[*].rojas");
        assertThat(rojas).containsExactly(1);
        List<Integer> asistencias = leer(stats, "$.asistidores[*].asistencias");
        assertThat(asistencias).containsExactly(1);

        // un jugador con eventos no puede salir de la plantilla del torneo
        esperarEstado(borrar("/api/v1/admin/torneos/" + torneoId + "/inscripciones/" + insLocal + "/jugadores/" + deLocal.get(0), admin), 409);
        // perfil público del jugador: sin documento
        MvcResult perfil = obtener("/api/v1/publico/jugadores/" + deLocal.get(0), null);
        esperarEstado(perfil, 200);
        assertThat(perfil.getResponse().getContentAsString()).doesNotContain("documento");
        assertThat((Integer) leer(perfil, "$.torneos[0].goles")).isEqualTo(1);
    }

    @Test
    void elWalkoverNoAceptaEventosYLosEventosNoVanSinTorneoEnCurso() throws Exception {
        List<Long> ids = torneoConEquipos(cuerpoTorneo("LIGA"), 2);
        long torneoId = ids.get(0);
        generarFixture(torneoId);
        Map<String, Object> p = partidos(torneoId).get(0);
        long partido = idPartido(p);
        long insLocal = inscripcionLocal(p);
        List<Long> deLocal = jugadoresDe(torneoId, insLocal);

        esperarEstado(agregar(partido, evento(insLocal, deLocal.get(0), "GOL", 5, null)), 409); // el torneo no ha iniciado
        iniciar(torneoId);
        esperarEstado(agregar(partido, evento(insLocal, deLocal.get(0), "GOL", 140, null)), 400); // minuto fuera de rango

        esperarEstado(postear("/api/v1/admin/partidos/" + partido + "/wo", admin, Map.of("ganador", "LOCAL")), 200);
        esperarEstado(agregar(partido, evento(insLocal, deLocal.get(0), "GOL", 5, null)), 409);
        MvcResult tabla = obtener("/api/v1/publico/torneos/" + torneoId + "/posiciones", null);
        List<Integer> puntos = leer(tabla, "$[0].filas[*].puntos");
        assertThat(puntos).containsExactly(3, 0);
    }

    @Test
    void lasEntidadesPublicasNoExponenDatosPrivadosNiBorradores() throws Exception {
        List<Long> ids = torneoConEquipos(cuerpoTorneo("LIGA"), 2);
        long torneoId = ids.get(0);
        long equipoId = jdbc.queryForObject("SELECT equipo_id FROM inscripciones_torneo WHERE id = ?", Long.class, ids.get(1));
        // el torneo sigue en borrador: el equipo no es visible al público
        esperarEstado(obtener("/api/v1/publico/equipos/" + equipoId, null), 404);
        List<Long> jugadores = jugadoresDe(torneoId, ids.get(1));
        esperarEstado(obtener("/api/v1/publico/jugadores/" + jugadores.get(0), null), 404);

        esperarEstado(estado(torneoId, "INSCRIPCIONES", null), 200);
        MvcResult equipo = obtener("/api/v1/publico/equipos/" + equipoId, null);
        esperarEstado(equipo, 200);
        String cuerpo = equipo.getResponse().getContentAsString();
        assertThat(cuerpo).doesNotContain("documento").doesNotContain("delegado").doesNotContain("usuarioResponsable");
        assertThat((List<?>) leer(equipo, "$.plantilla")).hasSize(2);
    }

    @Test
    void soloElAdminGestionaEquiposYTorneos() throws Exception {
        Sesion cliente = nuevoClienteConSesion();
        esperarEstado(obtener("/api/v1/admin/equipos", cliente.token()), 403);
        esperarEstado(obtener("/api/v1/admin/torneos", cliente.token()), 403);
        esperarEstado(postear("/api/v1/admin/torneos", cliente.token(), cuerpoTorneo("LIGA")), 403);
        esperarEstado(obtener("/api/v1/admin/torneos", null), 401);
        esperarEstado(obtener("/api/v1/publico/torneos", null), 200);
    }

    @Test
    void elMinutoEsOpcionalYLosEventosSinMinutoVanAlFinal() throws Exception {
        List<Long> ids = torneoConEquipos(cuerpoTorneo("LIGA"), 2);
        long torneoId = ids.get(0);
        generarFixture(torneoId);
        iniciar(torneoId);
        Map<String, Object> p = partidos(torneoId).get(0);
        long partido = idPartido(p);
        long insLocal = inscripcionLocal(p);
        List<Long> deLocal = jugadoresDe(torneoId, insLocal);

        esperarEstado(agregar(partido, evento(insLocal, deLocal.get(0), "GOL", null, null)), 201);
        MvcResult r = agregar(partido, evento(insLocal, deLocal.get(1), "AMARILLA", 30, null));

        esperarEstado(r, 201);
        assertThat((Integer) leer(r, "$.eventos[0].minuto")).isEqualTo(30);
        assertThat(((Map<?, ?>) leer(r, "$.eventos[1]")).containsKey("minuto")).isFalse(); // los nulos no viajan en el JSON
        assertThat((String) leer(r, "$.eventos[1].tipo")).isEqualTo("GOL");
    }
}
