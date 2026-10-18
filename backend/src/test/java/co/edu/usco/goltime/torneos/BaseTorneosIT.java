package co.edu.usco.goltime.torneos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import co.edu.usco.goltime.reservas.BaseReservasIT;

/** Ayudantes de las pruebas de torneos: equipos con plantilla, torneos, inscripción, fixture y resultados. */
public abstract class BaseTorneosIT extends BaseReservasIT {

    protected String admin;
    protected long sedeId;
    protected long canchaId;

    @BeforeEach
    void prepararBase() throws Exception {
        admin = ingresarAdmin().token();
        sedeId = crearSede(admin);
        canchaId = crearCancha(admin, sedeId, "Cancha 1");
    }

    protected MvcResult poner(String url, String token, Object cuerpo) throws Exception {
        return enviarJson(put(url), token, cuerpo);
    }

    protected MvcResult borrar(String url, String token) throws Exception {
        MockHttpServletRequestBuilder peticion = delete(url);
        if (token != null) {
            peticion.header("Authorization", "Bearer " + token);
        }
        return mvc.perform(peticion).andReturn();
    }

    protected static String documentoUnico() {
        return String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits() % 1_000_000_000L)) + System.nanoTime() % 1000;
    }

    // ----- Equipos -----

    /** Crea un equipo con {@code jugadores} jugadores de dorsales 1..n. */
    protected long crearEquipo(String nombre, int jugadores) throws Exception {
        MvcResult r = postear("/api/v1/admin/equipos", admin, Map.of("nombre", nombre, "color", "#112233"));
        esperarEstado(r, 201);
        long equipoId = ((Number) leer(r, "$.equipo.id")).longValue();
        for (int i = 1; i <= jugadores; i++) {
            Map<String, Object> m = new HashMap<>();
            m.put("nombres", "Jugador" + i);
            m.put("apellidos", nombre.replace(' ', '_') + i);
            m.put("documento", documentoUnico());
            m.put("dorsal", i);
            esperarEstado(postear("/api/v1/admin/equipos/" + equipoId + "/jugadores", admin, m), 201);
        }
        return equipoId;
    }

    protected long crearEquipo(int jugadores) throws Exception {
        return crearEquipo(nombre("Equipo"), jugadores);
    }

    // ----- Torneos -----

    protected Map<String, Object> cuerpoTorneo(String formato) {
        Map<String, Object> m = new HashMap<>();
        m.put("nombre", nombre("Torneo"));
        m.put("sedeId", sedeId);
        m.put("jugadoresPorEquipo", 2);
        m.put("formato", formato);
        m.put("modoEmparejamiento", "SEMBRADO");
        return m;
    }

    protected long crearTorneo(Map<String, Object> cuerpo) throws Exception {
        MvcResult r = postear("/api/v1/admin/torneos", admin, cuerpo);
        esperarEstado(r, 201);
        return ((Number) leer(r, "$.torneo.id")).longValue();
    }

    /** Inscribe el equipo con la semilla indicada y devuelve el id de la inscripción. */
    protected long inscribir(long torneoId, long equipoId, Integer semilla) throws Exception {
        Map<String, Object> m = new HashMap<>();
        m.put("equipoId", equipoId);
        m.put("semilla", semilla);
        MvcResult r = postear("/api/v1/admin/torneos/" + torneoId + "/inscripciones", admin, m);
        esperarEstado(r, 201);
        return id(r);
    }

    /** Crea un torneo del formato con {@code n} equipos de 2 jugadores, sembrados 1..n; devuelve [torneoId, ins1, ins2...]. */
    protected List<Long> torneoConEquipos(Map<String, Object> cuerpo, int n) throws Exception {
        long torneoId = crearTorneo(cuerpo);
        List<Long> resultado = new ArrayList<>();
        resultado.add(torneoId);
        for (int i = 1; i <= n; i++) {
            resultado.add(inscribir(torneoId, crearEquipo(2), i));
        }
        return resultado;
    }

    protected void generarFixture(long torneoId) throws Exception {
        esperarEstado(postear("/api/v1/admin/torneos/" + torneoId + "/fixture", admin, Map.of()), 200);
    }

    protected MvcResult estado(long torneoId, String estado, String motivo) throws Exception {
        Map<String, Object> m = new HashMap<>();
        m.put("estado", estado);
        m.put("motivo", motivo);
        return postear("/api/v1/admin/torneos/" + torneoId + "/estado", admin, m);
    }

    protected void iniciar(long torneoId) throws Exception {
        esperarEstado(estado(torneoId, "EN_CURSO", null), 200);
    }

    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> partidos(long torneoId) throws Exception {
        MvcResult r = obtener("/api/v1/admin/torneos/" + torneoId + "/partidos", admin);
        esperarEstado(r, 200);
        return leer(r, "$");
    }

    protected static long inscripcionLocal(Map<String, Object> partido) {
        return lado(partido, "local");
    }

    protected static long inscripcionVisitante(Map<String, Object> partido) {
        return lado(partido, "visitante");
    }

    @SuppressWarnings("unchecked")
    private static long lado(Map<String, Object> partido, String clave) {
        Map<String, Object> l = (Map<String, Object>) partido.get(clave);
        return ((Number) l.get("inscripcionId")).longValue();
    }

    protected static long idPartido(Map<String, Object> partido) {
        return ((Number) partido.get("id")).longValue();
    }

    protected MvcResult resultadoCrudo(long partidoId, int golesLocal, int golesVisitante, Integer penalesLocal, Integer penalesVisitante) throws Exception {
        Map<String, Object> m = new HashMap<>();
        m.put("golesLocal", golesLocal);
        m.put("golesVisitante", golesVisitante);
        m.put("penalesLocal", penalesLocal);
        m.put("penalesVisitante", penalesVisitante);
        return poner("/api/v1/admin/partidos/" + partidoId + "/resultado", admin, m);
    }

    protected void resultado(long partidoId, int golesLocal, int golesVisitante) throws Exception {
        esperarEstado(resultadoCrudo(partidoId, golesLocal, golesVisitante, null, null), 200);
    }

    /** Gana 1-0 el equipo de menor id de inscripción (resultado determinista para armar tablas y cuadros). */
    protected void ganaElDeMenorId(Map<String, Object> partido) throws Exception {
        boolean ganaLocal = inscripcionLocal(partido) < inscripcionVisitante(partido);
        resultado(idPartido(partido), ganaLocal ? 1 : 0, ganaLocal ? 0 : 1);
    }

    protected Map<String, Object> partidoDe(long torneoId, String nombreRonda, int numeroLlave) throws Exception {
        return partidos(torneoId).stream().filter(p -> nombreRonda.equals(p.get("nombreRonda")) && ((Number) p.get("numeroLlave")).intValue() == numeroLlave)
                .findFirst().orElseThrow();
    }

    protected List<Long> jugadoresDe(long torneoId, long inscripcionId) throws Exception {
        MvcResult r = obtener("/api/v1/admin/torneos/" + torneoId + "/inscripciones/" + inscripcionId + "/jugadores", admin);
        esperarEstado(r, 200);
        List<Number> ids = leer(r, "$[*].id");
        return ids.stream().map(Number::longValue).toList();
    }
}
