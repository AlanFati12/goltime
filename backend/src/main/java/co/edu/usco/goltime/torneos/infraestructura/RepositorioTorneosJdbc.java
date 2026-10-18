package co.edu.usco.goltime.torneos.infraestructura;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.EstadoInscripcion;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.Inscripcion;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.InscripcionVista;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.JugadorConDorsal;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.ResumenTorneo;
import co.edu.usco.goltime.torneos.aplicacion.RepositorioTorneos;
import co.edu.usco.goltime.torneos.dominio.CriterioDesempate;
import co.edu.usco.goltime.torneos.dominio.EstadoTorneo;
import co.edu.usco.goltime.torneos.dominio.FormatoTorneo;
import co.edu.usco.goltime.torneos.dominio.ModoEmparejamiento;
import co.edu.usco.goltime.torneos.dominio.ReglasPuntuacion;
import co.edu.usco.goltime.torneos.dominio.Torneo;

@Repository
class RepositorioTorneosJdbc implements RepositorioTorneos {

    private final JdbcTemplate jdbc;

    RepositorioTorneosJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ----- Torneos -----

    @Override
    public Torneo guardar(Torneo t) {
        Object[] valores = {t.nombre(), t.descripcion(), t.sedeId(), t.jugadoresPorEquipo(), t.formato().name(), t.estado().name(), t.fechaInicio(),
                t.fechaFin(), t.cupoEquipos(), t.valorInscripcion(), t.premiosTexto(), t.reglamento(), t.modoEmparejamiento().name(), t.idaYVuelta(),
                t.tercerPuesto(), t.numGrupos(), t.clasificadosPorGrupo(), t.reglas().puntosVictoria(), t.reglas().puntosEmpate(),
                t.reglas().puntosDerrota(), criteriosAJson(t.reglas().criterios()), t.reglas().marcadorWoGoles(), t.campeonId(), t.subcampeonId(),
                t.terceroId()};
        if (t.id() == null) {
            Long id = jdbc.queryForObject("""
                    INSERT INTO torneos (nombre, descripcion, sede_id, jugadores_por_equipo, formato, estado, fecha_inicio, fecha_fin, cupo_equipos,
                        valor_inscripcion, premios_texto, reglamento, modo_emparejamiento, ida_y_vuelta, tercer_puesto, num_grupos,
                        clasificados_por_grupo, puntos_victoria, puntos_empate, puntos_derrota, criterios_desempate, marcador_wo_goles,
                        campeon_id, subcampeon_id, tercero_id)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?) RETURNING id""", Long.class, valores);
            return buscar(id).orElseThrow();
        }
        Object[] conId = Arrays.copyOf(valores, valores.length + 1);
        conId[valores.length] = t.id();
        jdbc.update("""
                UPDATE torneos SET nombre = ?, descripcion = ?, sede_id = ?, jugadores_por_equipo = ?, formato = ?, estado = ?, fecha_inicio = ?,
                    fecha_fin = ?, cupo_equipos = ?, valor_inscripcion = ?, premios_texto = ?, reglamento = ?, modo_emparejamiento = ?,
                    ida_y_vuelta = ?, tercer_puesto = ?, num_grupos = ?, clasificados_por_grupo = ?, puntos_victoria = ?, puntos_empate = ?,
                    puntos_derrota = ?, criterios_desempate = ?::jsonb, marcador_wo_goles = ?, campeon_id = ?, subcampeon_id = ?, tercero_id = ?,
                    version = version + 1, actualizado_en = now()
                WHERE id = ?""", conId);
        return buscar(t.id()).orElseThrow();
    }

    @Override
    public Optional<Torneo> buscar(long id) {
        return jdbc.query("SELECT * FROM torneos WHERE id = ?", (rs, i) -> torneo(rs), id).stream().findFirst();
    }

    @Override
    public Optional<Torneo> buscarBloqueando(long id) {
        return jdbc.query("SELECT * FROM torneos WHERE id = ? FOR UPDATE", (rs, i) -> torneo(rs), id).stream().findFirst();
    }

    @Override
    public List<ResumenTorneo> listar(EstadoTorneo estado, boolean soloPublicos) {
        return jdbc.query("""
                SELECT t.*, s.nombre AS sede_nombre, c.nombre AS campeon_nombre,
                       (SELECT count(*) FROM inscripciones_torneo i WHERE i.torneo_id = t.id AND i.estado = 'APROBADA') AS equipos
                FROM torneos t JOIN sedes s ON s.id = t.sede_id LEFT JOIN equipos c ON c.id = t.campeon_id
                WHERE (?::text IS NULL OR t.estado = ?) AND (NOT ? OR t.estado <> 'BORRADOR')
                ORDER BY CASE t.estado WHEN 'EN_CURSO' THEN 0 WHEN 'INSCRIPCIONES' THEN 1 WHEN 'BORRADOR' THEN 2 WHEN 'FINALIZADO' THEN 3 ELSE 4 END,
                         t.fecha_inicio NULLS LAST, t.id DESC""",
                (rs, i) -> new ResumenTorneo(torneo(rs), rs.getString("sede_nombre"), rs.getInt("equipos"), rs.getString("campeon_nombre")),
                estado == null ? null : estado.name(), estado == null ? null : estado.name(), soloPublicos);
    }

    @Override
    public Set<Long> canchas(long torneoId) {
        return new TreeSet<>(jdbc.queryForList("SELECT cancha_id FROM torneo_canchas WHERE torneo_id = ?", Long.class, torneoId));
    }

    @Override
    public void reemplazarCanchas(long torneoId, Set<Long> canchaIds) {
        jdbc.update("DELETE FROM torneo_canchas WHERE torneo_id = ?", torneoId);
        for (long canchaId : canchaIds) {
            jdbc.update("INSERT INTO torneo_canchas (torneo_id, cancha_id) VALUES (?, ?)", torneoId, canchaId);
        }
    }

    // ----- Inscripciones -----

    @Override
    public Inscripcion insertarInscripcion(long torneoId, long equipoId, EstadoInscripcion estado, Integer semilla) {
        Long id = jdbc.queryForObject("INSERT INTO inscripciones_torneo (torneo_id, equipo_id, estado, semilla) VALUES (?, ?, ?, ?) RETURNING id", Long.class,
                torneoId, equipoId, estado.name(), semilla);
        return new Inscripcion(id, torneoId, equipoId, estado, semilla);
    }

    @Override
    public Optional<Inscripcion> buscarInscripcion(long id) {
        return jdbc.query("SELECT * FROM inscripciones_torneo WHERE id = ?", (rs, i) -> inscripcion(rs), id).stream().findFirst();
    }

    @Override
    public Optional<Inscripcion> buscarInscripcion(long torneoId, long equipoId) {
        return jdbc.query("SELECT * FROM inscripciones_torneo WHERE torneo_id = ? AND equipo_id = ?", (rs, i) -> inscripcion(rs), torneoId, equipoId)
                .stream().findFirst();
    }

    @Override
    public List<InscripcionVista> inscripciones(long torneoId) {
        return jdbc.query("""
                SELECT i.*, e.nombre, e.color, (SELECT count(*) FROM plantillas_torneo p WHERE p.inscripcion_id = i.id) AS jugadores
                FROM inscripciones_torneo i JOIN equipos e ON e.id = i.equipo_id
                WHERE i.torneo_id = ? ORDER BY i.semilla NULLS LAST, i.id""",
                (rs, n) -> new InscripcionVista(inscripcion(rs), rs.getString("nombre"), rs.getString("color"), rs.getInt("jugadores")), torneoId);
    }

    @Override
    public void actualizarInscripcion(long id, EstadoInscripcion estado, Integer semilla) {
        jdbc.update("UPDATE inscripciones_torneo SET estado = ?, semilla = ? WHERE id = ?", estado.name(), semilla, id);
    }

    @Override
    public void eliminarInscripcion(long id) {
        jdbc.update("DELETE FROM plantillas_torneo WHERE inscripcion_id = ?", id);
        jdbc.update("DELETE FROM inscripciones_torneo WHERE id = ?", id);
    }

    @Override
    public int contarAprobadas(long torneoId) {
        Integer n = jdbc.queryForObject("SELECT count(*) FROM inscripciones_torneo WHERE torneo_id = ? AND estado = 'APROBADA'", Integer.class, torneoId);
        return n == null ? 0 : n;
    }

    // ----- Plantillas por torneo -----

    @Override
    public void copiarPlantilla(long inscripcionId, long torneoId, long equipoId) {
        List<String> choques = jdbc.queryForList("""
                SELECT j.nombres || ' ' || j.apellidos FROM equipo_jugadores ej
                JOIN jugadores j ON j.id = ej.jugador_id
                JOIN plantillas_torneo p ON p.jugador_id = ej.jugador_id AND p.torneo_id = ?
                WHERE ej.equipo_id = ? AND ej.activo AND j.activo ORDER BY 1""", String.class, torneoId, equipoId);
        if (!choques.isEmpty()) {
            throw new ExcepcionNegocio(CodigoError.YA_EXISTE, "Estos jugadores ya juegan con otro equipo del torneo: " + String.join(", ", choques) + ".");
        }
        jdbc.update("""
                INSERT INTO plantillas_torneo (inscripcion_id, torneo_id, jugador_id, dorsal)
                SELECT ?, ?, ej.jugador_id, ej.dorsal FROM equipo_jugadores ej JOIN jugadores j ON j.id = ej.jugador_id
                WHERE ej.equipo_id = ? AND ej.activo AND j.activo""", inscripcionId, torneoId, equipoId);
    }

    @Override
    public List<JugadorConDorsal> plantillaDe(long inscripcionId) {
        return jdbc.query("""
                SELECT j.*, p.dorsal FROM plantillas_torneo p JOIN jugadores j ON j.id = p.jugador_id
                WHERE p.inscripcion_id = ? ORDER BY p.dorsal NULLS LAST, j.apellidos, j.nombres""",
                (rs, i) -> new JugadorConDorsal(RepositorioEquiposJdbc.jugador(rs), (Integer) rs.getObject("dorsal")), inscripcionId);
    }

    @Override
    public int contarJugadores(long inscripcionId) {
        Integer n = jdbc.queryForObject("SELECT count(*) FROM plantillas_torneo WHERE inscripcion_id = ?", Integer.class, inscripcionId);
        return n == null ? 0 : n;
    }

    @Override
    public boolean jugadorEnInscripcion(long inscripcionId, long jugadorId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM plantillas_torneo WHERE inscripcion_id = ? AND jugador_id = ?)",
                Boolean.class, inscripcionId, jugadorId));
    }

    @Override
    public Optional<Long> inscripcionDeJugador(long torneoId, long jugadorId) {
        return jdbc.queryForList("SELECT inscripcion_id FROM plantillas_torneo WHERE torneo_id = ? AND jugador_id = ?", Long.class, torneoId, jugadorId)
                .stream().findFirst();
    }

    @Override
    public void agregarJugador(long inscripcionId, long torneoId, long jugadorId, Integer dorsal) {
        jdbc.update("INSERT INTO plantillas_torneo (inscripcion_id, torneo_id, jugador_id, dorsal) VALUES (?, ?, ?, ?)", inscripcionId, torneoId, jugadorId,
                dorsal);
    }

    @Override
    public void quitarJugador(long inscripcionId, long jugadorId) {
        jdbc.update("DELETE FROM plantillas_torneo WHERE inscripcion_id = ? AND jugador_id = ?", inscripcionId, jugadorId);
    }

    @Override
    public boolean jugadorTieneEventos(long torneoId, long jugadorId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM eventos_partido e JOIN partidos p ON p.id = e.partido_id WHERE p.torneo_id = ? AND e.jugador_id = ?)
                    OR EXISTS (SELECT 1 FROM partidos p WHERE p.torneo_id = ? AND p.figura_jugador_id = ?)""", Boolean.class, torneoId, jugadorId, torneoId,
                jugadorId));
    }

    @Override
    public boolean dorsalEnUso(long inscripcionId, int dorsal, Long ignorarJugadorId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM plantillas_torneo WHERE inscripcion_id = ? AND dorsal = ? AND (?::bigint IS NULL OR jugador_id <> ?))""",
                Boolean.class, inscripcionId, dorsal, ignorarJugadorId, ignorarJugadorId));
    }

    @Override
    public void actualizarDorsal(long inscripcionId, long jugadorId, Integer dorsal) {
        jdbc.update("UPDATE plantillas_torneo SET dorsal = ? WHERE inscripcion_id = ? AND jugador_id = ?", dorsal, inscripcionId, jugadorId);
    }

    @Override
    public List<SolicitudDeCliente> solicitudesDe(long usuarioId) {
        return jdbc.query("""
                SELECT i.id, i.torneo_id, t.nombre AS torneo, e.nombre AS equipo, i.estado, i.creado_en
                FROM inscripciones_torneo i JOIN equipos e ON e.id = i.equipo_id JOIN torneos t ON t.id = i.torneo_id
                WHERE e.usuario_responsable_id = ? ORDER BY i.creado_en DESC, i.id DESC""",
                (rs, n) -> new SolicitudDeCliente(rs.getLong("id"), rs.getLong("torneo_id"), rs.getString("torneo"), rs.getString("equipo"),
                        EstadoInscripcion.valueOf(rs.getString("estado")), rs.getObject("creado_en", java.time.OffsetDateTime.class).toInstant()), usuarioId);
    }

    @Override
    public void vaciarPlantilla(long inscripcionId) {
        jdbc.update("DELETE FROM plantillas_torneo WHERE inscripcion_id = ?", inscripcionId);
    }

    @Override
    public List<InscripcionVista> solicitadas(long torneoId) {
        return inscripciones(torneoId).stream().filter(i -> i.inscripcion().estado() == EstadoInscripcion.SOLICITADA).toList();
    }

    @Override
    public List<Long> responsablesDeInscripciones(Collection<Long> inscripcionIds) {
        if (inscripcionIds.isEmpty()) {
            return List.of();
        }
        String marcadores = inscripcionIds.stream().map(i -> "?").collect(Collectors.joining(", "));
        return jdbc.queryForList("""
                SELECT DISTINCT e.usuario_responsable_id FROM inscripciones_torneo i JOIN equipos e ON e.id = i.equipo_id
                WHERE e.usuario_responsable_id IS NOT NULL AND i.id IN (""" + marcadores + ")", Long.class, inscripcionIds.toArray());
    }

    // ----- Mapeo -----

    private static Inscripcion inscripcion(ResultSet rs) throws SQLException {
        return new Inscripcion(rs.getLong("id"), rs.getLong("torneo_id"), rs.getLong("equipo_id"), EstadoInscripcion.valueOf(rs.getString("estado")),
                (Integer) rs.getObject("semilla"));
    }

    private static Torneo torneo(ResultSet rs) throws SQLException {
        ReglasPuntuacion reglas = new ReglasPuntuacion(rs.getInt("puntos_victoria"), rs.getInt("puntos_empate"), rs.getInt("puntos_derrota"),
                criteriosDeJson(rs.getString("criterios_desempate")), rs.getInt("marcador_wo_goles"));
        return new Torneo(rs.getLong("id"), rs.getString("nombre"), rs.getString("descripcion"), rs.getLong("sede_id"), rs.getInt("jugadores_por_equipo"),
                FormatoTorneo.valueOf(rs.getString("formato")), EstadoTorneo.valueOf(rs.getString("estado")),
                rs.getObject("fecha_inicio", LocalDate.class), rs.getObject("fecha_fin", LocalDate.class), (Integer) rs.getObject("cupo_equipos"),
                rs.getInt("valor_inscripcion"), rs.getString("premios_texto"), rs.getString("reglamento"),
                ModoEmparejamiento.valueOf(rs.getString("modo_emparejamiento")), rs.getBoolean("ida_y_vuelta"), rs.getBoolean("tercer_puesto"),
                (Integer) rs.getObject("num_grupos"), (Integer) rs.getObject("clasificados_por_grupo"), reglas, (Long) rs.getObject("campeon_id"),
                (Long) rs.getObject("subcampeon_id"), (Long) rs.getObject("tercero_id"));
    }

    static String criteriosAJson(List<CriterioDesempate> criterios) {
        return criterios.stream().map(c -> "\"" + c.name() + "\"").collect(Collectors.joining(",", "[", "]"));
    }

    static List<CriterioDesempate> criteriosDeJson(String json) {
        List<CriterioDesempate> lista = new ArrayList<>();
        for (String parte : json.replaceAll("[\\[\\]\"\\s]", "").split(",")) {
            if (!parte.isEmpty()) {
                lista.add(CriterioDesempate.valueOf(parte));
            }
        }
        return lista;
    }
}
