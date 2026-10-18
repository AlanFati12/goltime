package co.edu.usco.goltime.torneos.infraestructura;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import co.edu.usco.goltime.torneos.aplicacion.Modelos.EventoPartido;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.Fase;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.Grupo;
import co.edu.usco.goltime.torneos.aplicacion.RepositorioPartidos;
import co.edu.usco.goltime.torneos.dominio.EstadoPartido;
import co.edu.usco.goltime.torneos.dominio.Lado;
import co.edu.usco.goltime.torneos.dominio.Partido;
import co.edu.usco.goltime.torneos.dominio.TipoEvento;
import co.edu.usco.goltime.torneos.dominio.TipoFase;

@Repository
class RepositorioPartidosJdbc implements RepositorioPartidos {

    private static final String SELECT_PARTIDO = """
            SELECT p.*, f.tipo AS fase_tipo, g.nombre AS grupo_nombre
            FROM partidos p JOIN fases f ON f.id = p.fase_id LEFT JOIN grupos g ON g.id = p.grupo_id""";

    private final JdbcTemplate jdbc;

    RepositorioPartidosJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ----- Estructura -----

    @Override
    public void eliminarFixture(long torneoId) {
        jdbc.update("DELETE FROM eventos_partido WHERE partido_id IN (SELECT id FROM partidos WHERE torneo_id = ?)", torneoId);
        jdbc.update("DELETE FROM partidos WHERE torneo_id = ?", torneoId);
        jdbc.update("DELETE FROM grupo_equipos WHERE grupo_id IN (SELECT g.id FROM grupos g JOIN fases f ON f.id = g.fase_id WHERE f.torneo_id = ?)", torneoId);
        jdbc.update("DELETE FROM grupos WHERE fase_id IN (SELECT id FROM fases WHERE torneo_id = ?)", torneoId);
        jdbc.update("DELETE FROM fases WHERE torneo_id = ?", torneoId);
    }

    @Override
    public Fase insertarFase(long torneoId, TipoFase tipo, int orden, String nombre) {
        Long id = jdbc.queryForObject("INSERT INTO fases (torneo_id, tipo, orden, nombre) VALUES (?, ?, ?, ?) RETURNING id", Long.class, torneoId,
                tipo.name(), orden, nombre);
        return new Fase(id, torneoId, tipo, orden, nombre);
    }

    @Override
    public Grupo insertarGrupo(long faseId, String nombre, List<Long> inscripcionIds) {
        Long id = jdbc.queryForObject("INSERT INTO grupos (fase_id, nombre) VALUES (?, ?) RETURNING id", Long.class, faseId, nombre);
        for (long inscripcionId : inscripcionIds) {
            jdbc.update("INSERT INTO grupo_equipos (grupo_id, inscripcion_id) VALUES (?, ?)", id, inscripcionId);
        }
        return new Grupo(id, faseId, nombre, List.copyOf(inscripcionIds));
    }

    @Override
    public List<Fase> fases(long torneoId) {
        return jdbc.query("SELECT * FROM fases WHERE torneo_id = ? ORDER BY orden",
                (rs, i) -> new Fase(rs.getLong("id"), rs.getLong("torneo_id"), TipoFase.valueOf(rs.getString("tipo")), rs.getInt("orden"), rs.getString("nombre")),
                torneoId);
    }

    @Override
    public List<Grupo> grupos(long torneoId) {
        Map<Long, List<Long>> equipos = new HashMap<>();
        jdbc.query("""
                SELECT ge.grupo_id, ge.inscripcion_id FROM grupo_equipos ge JOIN grupos g ON g.id = ge.grupo_id JOIN fases f ON f.id = g.fase_id
                WHERE f.torneo_id = ? ORDER BY ge.grupo_id, ge.inscripcion_id""", rs -> {
                    equipos.computeIfAbsent(rs.getLong(1), k -> new java.util.ArrayList<>()).add(rs.getLong(2));
                }, torneoId);
        return jdbc.query("SELECT g.* FROM grupos g JOIN fases f ON f.id = g.fase_id WHERE f.torneo_id = ? ORDER BY f.orden, g.nombre",
                (rs, i) -> new Grupo(rs.getLong("id"), rs.getLong("fase_id"), rs.getString("nombre"), equipos.getOrDefault(rs.getLong("id"), List.of())),
                torneoId);
    }

    // ----- Partidos -----

    @Override
    public Partido insertar(Partido p) {
        Long id = jdbc.queryForObject("""
                INSERT INTO partidos (torneo_id, fase_id, grupo_id, ronda, numero_llave, nombre_ronda, local_id, visitante_id, etiqueta_local,
                    etiqueta_visitante, ref_local, ref_visitante, lado_en_siguiente, lado_perdedor, estado, es_bye, ganador_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id""", Long.class, p.torneoId(), p.faseId(), p.grupoId(), p.ronda(),
                p.numeroLlave(), p.nombreRonda(), p.localId(), p.visitanteId(), p.etiquetaLocal(), p.etiquetaVisitante(), p.refLocal(), p.refVisitante(),
                nombre(p.ladoSiguiente()), nombre(p.ladoPerdedor()), p.estado().name(), p.bye(), p.ganadorId());
        p.asignarId(id);
        return p;
    }

    @Override
    public void guardar(Partido p) {
        jdbc.update("""
                UPDATE partidos SET local_id = ?, visitante_id = ?, siguiente_partido_id = ?, siguiente_perdedor_id = ?, cancha_id = ?, reserva_id = ?,
                    inicio = ?, duracion_min = ?, arbitro = ?, estado = ?, goles_local = ?, goles_visitante = ?, penales_local = ?, penales_visitante = ?,
                    ganador_id = ?, es_wo = ?, figura_jugador_id = ?, version = version + 1
                WHERE id = ?""", p.localId(), p.visitanteId(), p.siguienteId(), p.siguientePerdedorId(), p.canchaId(), p.reservaId(),
                p.inicio() == null ? null : OffsetDateTime.ofInstant(p.inicio(), ZoneOffset.UTC), p.duracionMin(), p.arbitro(), p.estado().name(),
                p.golesLocal(), p.golesVisitante(), p.penalesLocal(), p.penalesVisitante(), p.ganadorId(), p.wo(), p.figuraJugadorId(), p.id());
    }

    @Override
    public Optional<Partido> buscar(long id) {
        return jdbc.query(SELECT_PARTIDO + " WHERE p.id = ?", (rs, i) -> partido(rs), id).stream().findFirst();
    }

    @Override
    public Optional<Partido> buscarBloqueando(long id) {
        jdbc.queryForList("SELECT id FROM partidos WHERE id = ? FOR UPDATE", Long.class, id);
        return buscar(id);
    }

    @Override
    public List<Partido> deTorneo(long torneoId) {
        return jdbc.query(SELECT_PARTIDO + " WHERE p.torneo_id = ? ORDER BY f.orden, p.ronda, p.numero_llave, p.id", (rs, i) -> partido(rs), torneoId);
    }

    @Override
    public boolean hayPartidosJugados(long torneoId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM partidos WHERE torneo_id = ? AND estado IN ('FINALIZADO', 'WO'))",
                Boolean.class, torneoId));
    }

    @Override
    public boolean equipoOcupado(long torneoId, Collection<Long> inscripcionIds, Instant inicio, Instant fin, Long ignorarPartidoId) {
        if (inscripcionIds.isEmpty()) {
            return false;
        }
        String marcadores = inscripcionIds.stream().map(i -> "?").collect(Collectors.joining(", "));
        List<Object> args = new java.util.ArrayList<>();
        args.add(torneoId);
        args.addAll(inscripcionIds);
        args.addAll(inscripcionIds);
        args.add(OffsetDateTime.ofInstant(fin, ZoneOffset.UTC));
        args.add(OffsetDateTime.ofInstant(inicio, ZoneOffset.UTC));
        args.add(ignorarPartidoId);
        args.add(ignorarPartidoId);
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM partidos p
                    WHERE p.torneo_id = ? AND p.inicio IS NOT NULL AND p.duracion_min IS NOT NULL
                      AND (p.local_id IN (""" + marcadores + ") OR p.visitante_id IN (" + marcadores + """
                    ))
                      AND p.inicio < ? AND p.inicio + make_interval(mins => p.duracion_min) > ?
                      AND (?::bigint IS NULL OR p.id <> ?))""", Boolean.class, args.toArray()));
    }

    // ----- Eventos -----

    @Override
    public EventoPartido insertarEvento(EventoPartido e) {
        Long id = jdbc.queryForObject("""
                INSERT INTO eventos_partido (partido_id, inscripcion_id, jugador_id, tipo, minuto, evento_relacionado_id, observacion)
                VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id""", Long.class, e.partidoId(), e.inscripcionId(), e.jugadorId(), e.tipo().name(), e.minuto(),
                e.eventoRelacionadoId(), e.observacion());
        return new EventoPartido(id, e.partidoId(), e.inscripcionId(), e.jugadorId(), e.tipo(), e.minuto(), e.eventoRelacionadoId(), e.observacion());
    }

    @Override
    public Optional<EventoPartido> buscarEvento(long id) {
        return jdbc.query("SELECT * FROM eventos_partido WHERE id = ?", (rs, i) -> evento(rs), id).stream().findFirst();
    }

    @Override
    public void eliminarEvento(long id) {
        jdbc.update("DELETE FROM eventos_partido WHERE id = ?", id);
    }

    @Override
    public List<EventoPartido> eventosDe(long partidoId) {
        return jdbc.query("SELECT * FROM eventos_partido WHERE partido_id = ? ORDER BY minuto NULLS LAST, id", (rs, i) -> evento(rs), partidoId);
    }

    @Override
    public Map<Long, Integer> puntosDisciplina(long torneoId) {
        Map<Long, Integer> puntos = new LinkedHashMap<>();
        jdbc.query("""
                SELECT e.inscripcion_id, sum(CASE e.tipo WHEN 'AMARILLA' THEN 1 WHEN 'ROJA' THEN 3 ELSE 0 END) AS puntos
                FROM eventos_partido e JOIN partidos p ON p.id = e.partido_id
                WHERE p.torneo_id = ? AND e.tipo IN ('AMARILLA', 'ROJA') GROUP BY e.inscripcion_id""",
                rs -> {
                    puntos.put(rs.getLong(1), rs.getInt(2));
                }, torneoId);
        return puntos;
    }

    // ----- Mapeo -----

    private static EventoPartido evento(ResultSet rs) throws SQLException {
        return new EventoPartido(rs.getLong("id"), rs.getLong("partido_id"), rs.getLong("inscripcion_id"), rs.getLong("jugador_id"),
                TipoEvento.valueOf(rs.getString("tipo")), (Integer) rs.getObject("minuto"), (Long) rs.getObject("evento_relacionado_id"), rs.getString("observacion"));
    }

    private static Partido partido(ResultSet rs) throws SQLException {
        OffsetDateTime inicio = rs.getObject("inicio", OffsetDateTime.class);
        return Partido.reconstruir(rs.getLong("id"), rs.getLong("torneo_id"), rs.getLong("fase_id"), TipoFase.valueOf(rs.getString("fase_tipo")),
                (Long) rs.getObject("grupo_id"), rs.getString("grupo_nombre"), rs.getInt("ronda"), rs.getInt("numero_llave"), rs.getString("nombre_ronda"),
                (Long) rs.getObject("local_id"), (Long) rs.getObject("visitante_id"), rs.getString("etiqueta_local"), rs.getString("etiqueta_visitante"),
                rs.getString("ref_local"), rs.getString("ref_visitante"), (Long) rs.getObject("siguiente_partido_id"), lado(rs.getString("lado_en_siguiente")),
                (Long) rs.getObject("siguiente_perdedor_id"), lado(rs.getString("lado_perdedor")), (Long) rs.getObject("cancha_id"),
                (Long) rs.getObject("reserva_id"), inicio == null ? null : inicio.toInstant(), (Integer) rs.getObject("duracion_min"), rs.getString("arbitro"),
                EstadoPartido.valueOf(rs.getString("estado")), (Integer) rs.getObject("goles_local"), (Integer) rs.getObject("goles_visitante"),
                (Integer) rs.getObject("penales_local"), (Integer) rs.getObject("penales_visitante"), (Long) rs.getObject("ganador_id"),
                rs.getBoolean("es_bye"), rs.getBoolean("es_wo"), (Long) rs.getObject("figura_jugador_id"));
    }

    private static Lado lado(String valor) {
        return valor == null ? null : Lado.valueOf(valor);
    }

    private static String nombre(Lado lado) {
        return lado == null ? null : lado.name();
    }
}
