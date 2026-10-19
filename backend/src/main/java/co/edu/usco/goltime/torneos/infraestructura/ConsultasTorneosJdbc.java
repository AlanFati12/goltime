package co.edu.usco.goltime.torneos.infraestructura;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import co.edu.usco.goltime.torneos.aplicacion.ConsultasTorneos;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.EquipoPartido;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.EstadisticaJugadorTorneo;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.EstadisticasTorneo;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.FilaAsistidor;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.FilaGoleador;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.FilaTarjetas;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.JugadorConDorsal;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.PerfilEquipo;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.PerfilJugador;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.TorneoDeEquipo;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.VistaEvento;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.VistaPartido;
import co.edu.usco.goltime.torneos.dominio.EstadoPartido;
import co.edu.usco.goltime.torneos.dominio.EstadoTorneo;
import co.edu.usco.goltime.torneos.dominio.Lado;
import co.edu.usco.goltime.torneos.dominio.TipoEvento;
import co.edu.usco.goltime.torneos.dominio.TipoFase;

@Repository
class ConsultasTorneosJdbc implements ConsultasTorneos {

    private static final String SELECT_VISTA = """
            SELECT p.id, p.torneo_id, t.nombre AS torneo_nombre, p.fase_id, f.tipo AS fase_tipo, g.nombre AS grupo, p.ronda, p.numero_llave, p.nombre_ronda,
                   p.local_id, el.id AS local_equipo_id, el.nombre AS local_nombre, el.color AS local_color, p.etiqueta_local,
                   p.visitante_id, ev.id AS visitante_equipo_id, ev.nombre AS visitante_nombre, ev.color AS visitante_color, p.etiqueta_visitante,
                   p.cancha_id, c.nombre AS cancha_nombre, p.reserva_id, p.inicio, p.duracion_min, p.arbitro, p.estado, p.goles_local, p.goles_visitante,
                   p.penales_local, p.penales_visitante, p.ganador_id, p.es_bye, p.es_wo, p.figura_jugador_id,
                   jf.nombres || ' ' || jf.apellidos AS figura_nombre, p.siguiente_partido_id, p.lado_en_siguiente
            FROM partidos p
            JOIN torneos t ON t.id = p.torneo_id
            JOIN fases f ON f.id = p.fase_id
            LEFT JOIN grupos g ON g.id = p.grupo_id
            LEFT JOIN inscripciones_torneo il ON il.id = p.local_id
            LEFT JOIN equipos el ON el.id = il.equipo_id
            LEFT JOIN inscripciones_torneo iv ON iv.id = p.visitante_id
            LEFT JOIN equipos ev ON ev.id = iv.equipo_id
            LEFT JOIN canchas c ON c.id = p.cancha_id
            LEFT JOIN jugadores jf ON jf.id = p.figura_jugador_id
            """;

    private final JdbcTemplate jdbc;

    ConsultasTorneosJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<VistaPartido> partidos(long torneoId, FiltroPartidos f) {
        Long inscripcion = f.inscripcionId();
        return jdbc.query(SELECT_VISTA + """
                 WHERE p.torneo_id = ? AND (?::bigint IS NULL OR p.fase_id = ?) AND (?::bigint IS NULL OR p.local_id = ? OR p.visitante_id = ?)
                   AND (?::text IS NULL OR p.estado = ?)
                 ORDER BY f.orden, p.ronda, p.numero_llave, p.id""", (rs, i) -> vista(rs), torneoId, f.faseId(), f.faseId(), inscripcion, inscripcion,
                inscripcion, f.estado() == null ? null : f.estado().name(), f.estado() == null ? null : f.estado().name());
    }

    @Override
    public Optional<VistaPartido> partido(long partidoId) {
        return jdbc.query(SELECT_VISTA + " WHERE p.id = ?", (rs, i) -> vista(rs), partidoId).stream().findFirst();
    }

    @Override
    public List<VistaEvento> eventos(long partidoId) {
        return jdbc.query("""
                SELECT e.*, eq.nombre AS equipo_nombre, j.nombres || ' ' || j.apellidos AS jugador_nombre, pt.dorsal
                FROM eventos_partido e
                JOIN inscripciones_torneo i ON i.id = e.inscripcion_id
                JOIN equipos eq ON eq.id = i.equipo_id
                JOIN jugadores j ON j.id = e.jugador_id
                LEFT JOIN plantillas_torneo pt ON pt.inscripcion_id = e.inscripcion_id AND pt.jugador_id = e.jugador_id
                WHERE e.partido_id = ? ORDER BY e.minuto NULLS LAST, e.id""", (rs, n) -> new VistaEvento(rs.getLong("id"), rs.getLong("partido_id"),
                rs.getLong("inscripcion_id"), rs.getString("equipo_nombre"), rs.getLong("jugador_id"), rs.getString("jugador_nombre"),
                (Integer) rs.getObject("dorsal"), TipoEvento.valueOf(rs.getString("tipo")), (Integer) rs.getObject("minuto"),
                (Long) rs.getObject("evento_relacionado_id"), rs.getString("observacion")), partidoId);
    }

    @Override
    public Map<Long, EquipoPartido> equiposInscritos(long torneoId) {
        Map<Long, EquipoPartido> equipos = new LinkedHashMap<>();
        jdbc.query("""
                SELECT i.id, e.id AS equipo_id, e.nombre, e.color FROM inscripciones_torneo i JOIN equipos e ON e.id = i.equipo_id
                WHERE i.torneo_id = ? ORDER BY i.id""", rs -> {
                    equipos.put(rs.getLong("id"), new EquipoPartido(rs.getLong("id"), rs.getLong("equipo_id"), rs.getString("nombre"), rs.getString("color"), null));
                }, torneoId);
        return equipos;
    }

    @Override
    public EstadisticasTorneo estadisticas(long torneoId, int limite) {
        List<FilaGoleador> goleadores = jdbc.query("""
                SELECT j.id AS jugador_id, j.nombres || ' ' || j.apellidos AS jugador, eq.id AS equipo_id, eq.nombre AS equipo,
                       count(*) AS goles, count(*) FILTER (WHERE e.tipo = 'GOL_PENAL') AS penales, count(DISTINCT e.partido_id) AS partidos
                FROM eventos_partido e
                JOIN partidos p ON p.id = e.partido_id
                JOIN jugadores j ON j.id = e.jugador_id
                JOIN inscripciones_torneo i ON i.id = e.inscripcion_id
                JOIN equipos eq ON eq.id = i.equipo_id
                WHERE p.torneo_id = ? AND e.tipo IN ('GOL', 'GOL_PENAL')
                GROUP BY j.id, j.apellidos, j.nombres, eq.id, eq.nombre
                ORDER BY goles DESC, penales ASC, j.apellidos, j.nombres LIMIT ?""", (rs, n) -> new FilaGoleador(rs.getLong("jugador_id"), rs.getString("jugador"),
                rs.getLong("equipo_id"), rs.getString("equipo"), rs.getInt("goles"), rs.getInt("penales"), rs.getInt("partidos")), torneoId, limite);
        List<FilaAsistidor> asistidores = jdbc.query("""
                SELECT j.id AS jugador_id, j.nombres || ' ' || j.apellidos AS jugador, eq.id AS equipo_id, eq.nombre AS equipo, count(*) AS asistencias
                FROM eventos_partido e
                JOIN partidos p ON p.id = e.partido_id
                JOIN jugadores j ON j.id = e.jugador_id
                JOIN inscripciones_torneo i ON i.id = e.inscripcion_id
                JOIN equipos eq ON eq.id = i.equipo_id
                WHERE p.torneo_id = ? AND e.tipo = 'ASISTENCIA'
                GROUP BY j.id, j.apellidos, j.nombres, eq.id, eq.nombre
                ORDER BY asistencias DESC, j.apellidos, j.nombres LIMIT ?""", (rs, n) -> new FilaAsistidor(rs.getLong("jugador_id"), rs.getString("jugador"),
                rs.getLong("equipo_id"), rs.getString("equipo"), rs.getInt("asistencias")), torneoId, limite);
        List<FilaTarjetas> tarjetas = jdbc.query("""
                SELECT j.id AS jugador_id, j.nombres || ' ' || j.apellidos AS jugador, eq.id AS equipo_id, eq.nombre AS equipo,
                       count(*) FILTER (WHERE e.tipo = 'AMARILLA') AS amarillas, count(*) FILTER (WHERE e.tipo = 'ROJA') AS rojas
                FROM eventos_partido e
                JOIN partidos p ON p.id = e.partido_id
                JOIN jugadores j ON j.id = e.jugador_id
                JOIN inscripciones_torneo i ON i.id = e.inscripcion_id
                JOIN equipos eq ON eq.id = i.equipo_id
                WHERE p.torneo_id = ? AND e.tipo IN ('AMARILLA', 'ROJA')
                GROUP BY j.id, j.apellidos, j.nombres, eq.id, eq.nombre
                ORDER BY rojas DESC, amarillas DESC, j.apellidos, j.nombres LIMIT ?""", (rs, n) -> new FilaTarjetas(rs.getLong("jugador_id"),
                rs.getString("jugador"), rs.getLong("equipo_id"), rs.getString("equipo"), rs.getInt("amarillas"), rs.getInt("rojas")), torneoId, limite);
        return new EstadisticasTorneo(goleadores, asistidores, tarjetas);
    }

    @Override
    public Optional<PerfilEquipo> perfilEquipo(long equipoId) {
        List<TorneoDeEquipo> torneos = jdbc.query("""
                SELECT t.id, t.nombre, t.estado, i.id AS inscripcion_id FROM inscripciones_torneo i JOIN torneos t ON t.id = i.torneo_id
                WHERE i.equipo_id = ? AND i.estado = 'APROBADA' AND t.estado <> 'BORRADOR' ORDER BY t.id DESC""",
                (rs, n) -> new TorneoDeEquipo(rs.getLong("id"), rs.getString("nombre"), EstadoTorneo.valueOf(rs.getString("estado")), rs.getLong("inscripcion_id")),
                equipoId);
        if (torneos.isEmpty()) {
            return Optional.empty();
        }
        var equipo = jdbc.query("SELECT * FROM equipos WHERE id = ?", (rs, n) -> RepositorioEquiposJdbc.equipo(rs), equipoId).stream().findFirst();
        if (equipo.isEmpty()) {
            return Optional.empty();
        }
        List<JugadorConDorsal> plantilla = jdbc.query("""
                SELECT j.*, ej.dorsal FROM equipo_jugadores ej JOIN jugadores j ON j.id = ej.jugador_id
                WHERE ej.equipo_id = ? AND ej.activo ORDER BY ej.dorsal NULLS LAST, j.apellidos, j.nombres""",
                (rs, n) -> new JugadorConDorsal(RepositorioEquiposJdbc.jugador(rs), (Integer) rs.getObject("dorsal")), equipoId);
        List<VistaPartido> partidos = jdbc.query(SELECT_VISTA + """
                 WHERE t.estado <> 'BORRADOR' AND (il.equipo_id = ? OR iv.equipo_id = ?) AND NOT p.es_bye
                 ORDER BY p.inicio DESC NULLS LAST, p.id DESC LIMIT 15""", (rs, n) -> vista(rs), equipoId, equipoId);
        return Optional.of(new PerfilEquipo(equipo.get(), plantilla, torneos, partidos));
    }

    @Override
    public Optional<PerfilJugador> perfilJugador(long jugadorId) {
        List<EstadisticaJugadorTorneo> torneos = jdbc.query("""
                SELECT t.id, t.nombre, eq.nombre AS equipo,
                       count(*) FILTER (WHERE e.tipo IN ('GOL', 'GOL_PENAL')) AS goles, count(*) FILTER (WHERE e.tipo = 'ASISTENCIA') AS asistencias,
                       count(*) FILTER (WHERE e.tipo = 'AMARILLA') AS amarillas, count(*) FILTER (WHERE e.tipo = 'ROJA') AS rojas
                FROM plantillas_torneo pt
                JOIN torneos t ON t.id = pt.torneo_id
                JOIN inscripciones_torneo i ON i.id = pt.inscripcion_id
                JOIN equipos eq ON eq.id = i.equipo_id
                LEFT JOIN partidos pa ON pa.torneo_id = t.id
                LEFT JOIN eventos_partido e ON e.partido_id = pa.id AND e.jugador_id = pt.jugador_id
                WHERE pt.jugador_id = ? AND t.estado <> 'BORRADOR'
                GROUP BY t.id, t.nombre, eq.nombre ORDER BY t.id DESC""", (rs, n) -> new EstadisticaJugadorTorneo(rs.getLong("id"), rs.getString("nombre"),
                rs.getString("equipo"), rs.getInt("goles"), rs.getInt("asistencias"), rs.getInt("amarillas"), rs.getInt("rojas")), jugadorId);
        if (torneos.isEmpty()) {
            return Optional.empty();
        }
        return jdbc.query("SELECT * FROM jugadores WHERE id = ?", (rs, n) -> new PerfilJugador(rs.getLong("id"),
                rs.getString("nombres") + " " + rs.getString("apellidos"), rs.getString("posicion"), torneos), jugadorId).stream().findFirst();
    }

    @Override
    public List<VistaPartido> proximosPartidos(int limite) {
        return jdbc.query(SELECT_VISTA + """
                 WHERE t.estado IN ('INSCRIPCIONES', 'EN_CURSO') AND p.estado = 'PROGRAMADO' AND p.inicio >= now()
                 ORDER BY p.inicio LIMIT ?""", (rs, i) -> vista(rs), limite);
    }

    // ----- Mapeo -----

    private static VistaPartido vista(ResultSet rs) throws SQLException {
        OffsetDateTime inicio = rs.getObject("inicio", OffsetDateTime.class);
        String lado = rs.getString("lado_en_siguiente");
        return new VistaPartido(rs.getLong("id"), rs.getLong("torneo_id"), rs.getString("torneo_nombre"), rs.getLong("fase_id"),
                TipoFase.valueOf(rs.getString("fase_tipo")), rs.getString("grupo"), rs.getInt("ronda"), rs.getInt("numero_llave"), rs.getString("nombre_ronda"),
                equipo(rs, "local", "etiqueta_local"), equipo(rs, "visitante", "etiqueta_visitante"), (Long) rs.getObject("cancha_id"),
                rs.getString("cancha_nombre"), (Long) rs.getObject("reserva_id"), inicio == null ? null : inicio.toInstant(),
                (Integer) rs.getObject("duracion_min"), rs.getString("arbitro"), EstadoPartido.valueOf(rs.getString("estado")),
                (Integer) rs.getObject("goles_local"), (Integer) rs.getObject("goles_visitante"), (Integer) rs.getObject("penales_local"),
                (Integer) rs.getObject("penales_visitante"), (Long) rs.getObject("ganador_id"), rs.getBoolean("es_bye"), rs.getBoolean("es_wo"),
                (Long) rs.getObject("figura_jugador_id"), rs.getString("figura_nombre"), (Long) rs.getObject("siguiente_partido_id"),
                lado == null ? null : Lado.valueOf(lado));
    }

    private static EquipoPartido equipo(ResultSet rs, String lado, String etiqueta) throws SQLException {
        Long inscripcionId = (Long) rs.getObject(lado + "_id");
        if (inscripcionId == null) {
            String texto = rs.getString(etiqueta);
            return texto == null ? null : new EquipoPartido(null, null, null, null, texto);
        }
        return new EquipoPartido(inscripcionId, (Long) rs.getObject(lado + "_equipo_id"), rs.getString(lado + "_nombre"), rs.getString(lado + "_color"),
                rs.getString(etiqueta));
    }
}
