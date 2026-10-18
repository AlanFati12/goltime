package co.edu.usco.goltime.torneos;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

class TorneoLigaIT extends BaseTorneosIT {

    @Test
    void ligaCompletaDeCuatroEquiposDesdeLaInscripcionHastaElCampeon() throws Exception {
        List<Long> ids = torneoConEquipos(cuerpoTorneo("LIGA"), 4);
        long torneoId = ids.get(0);
        long i1 = ids.get(1);
        long i2 = ids.get(2);
        long i3 = ids.get(3);
        long i4 = ids.get(4);

        // antes de iniciar: ni resultados ni fixture
        esperarEstado(estado(torneoId, "EN_CURSO", null), 409);
        generarFixture(torneoId);
        List<Map<String, Object>> partidos = partidos(torneoId);
        assertThat(partidos).hasSize(6);
        assertThat(partidos).allMatch(p -> "PENDIENTE".equals(p.get("estado")));
        esperarEstado(resultadoCrudo(idPartido(partidos.get(0)), 1, 0, null, null), 409); // torneo aún no inició

        iniciar(torneoId);
        for (Map<String, Object> p : partidos) {
            ganaElDeMenorId(p);
        }

        MvcResult tabla = obtener("/api/v1/admin/torneos/" + torneoId + "/posiciones", admin);
        esperarEstado(tabla, 200);
        List<Number> orden = leer(tabla, "$[0].filas[*].inscripcionId");
        assertThat(orden.stream().map(Number::longValue)).containsExactly(i1, i2, i3, i4);
        List<Integer> puntos = leer(tabla, "$[0].filas[*].puntos");
        assertThat(puntos).containsExactly(9, 6, 3, 0);
        List<Integer> diferencia = leer(tabla, "$[0].filas[*].diferencia");
        assertThat(diferencia).containsExactly(3, 1, -1, -3);

        // finalizar: campeón, subcampeón y tercero
        MvcResult fin = estado(torneoId, "FINALIZADO", null);
        esperarEstado(fin, 200);
        assertThat((String) leer(obtener("/api/v1/publico/torneos/" + torneoId, null), "$.torneo.estado")).isEqualTo("FINALIZADO");
        MvcResult detalle = obtener("/api/v1/publico/torneos/" + torneoId, null);
        assertThat((String) leer(detalle, "$.torneo.campeon")).isNotBlank();
        assertThat((String) leer(detalle, "$.torneo.subcampeon")).isNotBlank();
        assertThat((Integer) leer(detalle, "$.partidosJugados")).isEqualTo(6);
        // ya terminó: no se admiten más cambios
        esperarEstado(postear("/api/v1/admin/torneos/" + torneoId + "/fixture", admin, Map.of()), 409);
    }

    @Test
    void noSePuedeFinalizarConPartidosPendientesNiRegenerarElFixtureDespuesDeJugar() throws Exception {
        List<Long> ids = torneoConEquipos(cuerpoTorneo("LIGA"), 3);
        long torneoId = ids.get(0);
        generarFixture(torneoId);
        generarFixture(torneoId); // regenerar antes de jugar está permitido
        assertThat(partidos(torneoId)).hasSize(3);
        iniciar(torneoId);
        ganaElDeMenorId(partidos(torneoId).get(0));

        MvcResult pendiente = estado(torneoId, "FINALIZADO", null);
        esperarEstado(pendiente, 409);
        assertThat((String) leer(pendiente, "$.detail")).contains("Faltan 2 partidos");
        esperarEstado(postear("/api/v1/admin/torneos/" + torneoId + "/fixture", admin, Map.of()), 409);
        esperarEstado(borrar("/api/v1/admin/torneos/" + torneoId + "/fixture", admin), 409);
    }

    @Test
    void ligaIdaYVueltaYPuntuacionConfigurable() throws Exception {
        Map<String, Object> cuerpo = cuerpoTorneo("LIGA");
        cuerpo.put("idaYVuelta", true);
        cuerpo.put("puntosVictoria", 2);
        cuerpo.put("puntosEmpate", 1);
        List<Long> ids = torneoConEquipos(cuerpo, 3);
        long torneoId = ids.get(0);
        generarFixture(torneoId);
        List<Map<String, Object>> partidos = partidos(torneoId);
        assertThat(partidos).hasSize(6);
        iniciar(torneoId);
        // todos empatan 1-1
        for (Map<String, Object> p : partidos) {
            resultado(idPartido(p), 1, 1);
        }
        List<Integer> puntos = leer(obtener("/api/v1/publico/torneos/" + torneoId + "/posiciones", null), "$[0].filas[*].puntos");
        assertThat(puntos).containsExactly(4, 4, 4);
        // corregir un resultado: reabrir y volver a registrar
        long primero = idPartido(partidos.get(0));
        esperarEstado(postear("/api/v1/admin/partidos/" + primero + "/reabrir", admin, Map.of()), 200);
        assertThat((String) leer(obtener("/api/v1/admin/partidos/" + primero, admin), "$.partido.estado")).isEqualTo("PENDIENTE");
        resultado(primero, 2, 0);
        List<Integer> despues = leer(obtener("/api/v1/publico/torneos/" + torneoId + "/posiciones", null), "$[0].filas[*].puntos");
        assertThat(despues.get(0)).isEqualTo(5);
    }

    @Test
    void elBorradorNoEsPublicoHastaAbrirInscripciones() throws Exception {
        long torneoId = crearTorneo(cuerpoTorneo("LIGA"));

        esperarEstado(obtener("/api/v1/publico/torneos/" + torneoId, null), 404);
        List<Number> publicos = leer(obtener("/api/v1/publico/torneos", null), "$[*].id");
        assertThat(publicos.stream().map(Number::longValue)).doesNotContain(torneoId);
        List<Number> deAdmin = leer(obtener("/api/v1/admin/torneos", admin), "$[*].id");
        assertThat(deAdmin.stream().map(Number::longValue)).contains(torneoId);

        esperarEstado(estado(torneoId, "INSCRIPCIONES", null), 200);
        esperarEstado(obtener("/api/v1/publico/torneos/" + torneoId, null), 200);
        // transiciones inválidas
        esperarEstado(estado(torneoId, "FINALIZADO", null), 409);
        esperarEstado(estado(torneoId, "CANCELADO", null), 400); // exige motivo
        esperarEstado(estado(torneoId, "CANCELADO", "Sin equipos suficientes"), 200);
        esperarEstado(estado(torneoId, "INSCRIPCIONES", null), 409);
    }

    @Test
    void cupoPlantillasYReglasDeInscripcion() throws Exception {
        Map<String, Object> cuerpo = cuerpoTorneo("LIGA");
        cuerpo.put("cupoEquipos", 2);
        long torneoId = crearTorneo(cuerpo);
        long a = crearEquipo(2);
        long b = crearEquipo(2);
        long c = crearEquipo(2);
        long insA = inscribir(torneoId, a, 1);
        inscribir(torneoId, b, 2);

        Map<String, Object> m = new HashMap<>();
        m.put("equipoId", c);
        esperarEstado(postear("/api/v1/admin/torneos/" + torneoId + "/inscripciones", admin, m), 409); // cupo lleno
        m.put("equipoId", a);
        esperarEstado(postear("/api/v1/admin/torneos/" + torneoId + "/inscripciones", admin, m), 409); // ya inscrito

        // un jugador no puede estar en dos equipos del mismo torneo
        Long jugadorDeA = jugadoresDe(torneoId, insA).get(0);
        long otraInscripcion = jdbc.queryForObject("SELECT id FROM inscripciones_torneo WHERE torneo_id = ? AND equipo_id = ?", Long.class, torneoId, b);
        Map<String, Object> agregar = new HashMap<>();
        agregar.put("jugadorId", jugadorDeA);
        esperarEstado(postear("/api/v1/admin/torneos/" + torneoId + "/inscripciones/" + otraInscripcion + "/jugadores", admin, agregar), 409);

        // equipo con plantilla incompleta: no se puede generar el fixture
        long pequeno = crearEquipo(1);
        esperarEstado(borrar("/api/v1/admin/torneos/" + torneoId + "/inscripciones/" + otraInscripcion, admin), 204);
        inscribir(torneoId, pequeno, 3);
        MvcResult r = postear("/api/v1/admin/torneos/" + torneoId + "/fixture", admin, Map.of());
        esperarEstado(r, 400);
        assertThat((String) leer(r, "$.detail")).contains("(1/2)");
    }
}
