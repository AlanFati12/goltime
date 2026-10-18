package co.edu.usco.goltime.torneos.infraestructura;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import co.edu.usco.goltime.torneos.aplicacion.Modelos.Equipo;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.Jugador;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.JugadorConDorsal;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.ResumenEquipo;
import co.edu.usco.goltime.torneos.aplicacion.RepositorioEquipos;

@Repository
class RepositorioEquiposJdbc implements RepositorioEquipos {

    private final JdbcTemplate jdbc;

    RepositorioEquiposJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Equipo guardar(Equipo e) {
        if (e.id() == null) {
            Long id = jdbc.queryForObject("""
                    INSERT INTO equipos (nombre, color, delegado_nombre, delegado_telefono, usuario_responsable_id, activo)
                    VALUES (?, ?, ?, ?, ?, ?) RETURNING id""", Long.class, e.nombre(), e.color(), e.delegadoNombre(), e.delegadoTelefono(),
                    e.usuarioResponsableId(), e.activo());
            return new Equipo(id, e.nombre(), e.color(), e.delegadoNombre(), e.delegadoTelefono(), e.usuarioResponsableId(), e.activo());
        }
        jdbc.update("""
                UPDATE equipos SET nombre = ?, color = ?, delegado_nombre = ?, delegado_telefono = ?, usuario_responsable_id = ?, activo = ?,
                       actualizado_en = now() WHERE id = ?""", e.nombre(), e.color(), e.delegadoNombre(), e.delegadoTelefono(), e.usuarioResponsableId(),
                e.activo(), e.id());
        return e;
    }

    @Override
    public Optional<Equipo> buscar(long id) {
        return jdbc.query("SELECT * FROM equipos WHERE id = ?", (rs, i) -> equipo(rs), id).stream().findFirst();
    }

    @Override
    public boolean existeNombre(String nombre, Long ignorarId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM equipos WHERE lower(nombre) = lower(?) AND (?::bigint IS NULL OR id <> ?))",
                Boolean.class, nombre, ignorarId, ignorarId));
    }

    @Override
    public List<ResumenEquipo> listar(String busqueda, boolean incluirInactivos) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT e.*,
                       (SELECT count(*) FROM equipo_jugadores ej WHERE ej.equipo_id = e.id AND ej.activo) AS jugadores,
                       (SELECT count(*) FROM inscripciones_torneo i WHERE i.equipo_id = e.id AND i.estado = 'APROBADA') AS torneos
                FROM equipos e WHERE TRUE""");
        if (!incluirInactivos) {
            sql.append(" AND e.activo");
        }
        if (busqueda != null && !busqueda.isBlank()) {
            sql.append(" AND lower(e.nombre) LIKE ?");
            args.add("%" + escaparLike(busqueda.trim().toLowerCase()) + "%");
        }
        sql.append(" ORDER BY lower(e.nombre)");
        return jdbc.query(sql.toString(), (rs, i) -> new ResumenEquipo(equipo(rs), rs.getInt("jugadores"), rs.getInt("torneos")), args.toArray());
    }

    @Override
    public Jugador guardarJugador(Jugador j) {
        if (j.id() == null) {
            Long id = jdbc.queryForObject("""
                    INSERT INTO jugadores (nombres, apellidos, documento, posicion, usuario_id, activo) VALUES (?, ?, ?, ?, ?, ?) RETURNING id""",
                    Long.class, j.nombres(), j.apellidos(), j.documento(), j.posicion(), j.usuarioId(), j.activo());
            return new Jugador(id, j.nombres(), j.apellidos(), j.documento(), j.posicion(), j.usuarioId(), j.activo());
        }
        jdbc.update("""
                UPDATE jugadores SET nombres = ?, apellidos = ?, documento = ?, posicion = ?, usuario_id = ?, activo = ?, actualizado_en = now()
                WHERE id = ?""", j.nombres(), j.apellidos(), j.documento(), j.posicion(), j.usuarioId(), j.activo(), j.id());
        return j;
    }

    @Override
    public Optional<Jugador> buscarJugador(long id) {
        return jdbc.query("SELECT * FROM jugadores WHERE id = ?", (rs, i) -> jugador(rs), id).stream().findFirst();
    }

    @Override
    public List<Jugador> buscarJugadores(String texto, int limite) {
        if (texto == null || texto.isBlank()) {
            return List.of();
        }
        String t = texto.trim().toLowerCase();
        return jdbc.query("""
                SELECT * FROM jugadores
                WHERE activo AND (lower(nombres || ' ' || apellidos) LIKE ? OR documento = ?)
                ORDER BY apellidos, nombres LIMIT ?""", (rs, i) -> jugador(rs), "%" + escaparLike(t) + "%", texto.trim(), limite);
    }

    @Override
    public List<JugadorConDorsal> plantilla(long equipoId) {
        return jdbc.query("""
                SELECT j.*, ej.dorsal FROM equipo_jugadores ej JOIN jugadores j ON j.id = ej.jugador_id
                WHERE ej.equipo_id = ? AND ej.activo ORDER BY ej.dorsal NULLS LAST, j.apellidos, j.nombres""",
                (rs, i) -> new JugadorConDorsal(jugador(rs), (Integer) rs.getObject("dorsal")), equipoId);
    }

    @Override
    public boolean enPlantilla(long equipoId, long jugadorId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM equipo_jugadores WHERE equipo_id = ? AND jugador_id = ? AND activo)",
                Boolean.class, equipoId, jugadorId));
    }

    @Override
    public void agregarAPlantilla(long equipoId, long jugadorId, Integer dorsal) {
        jdbc.update("""
                INSERT INTO equipo_jugadores (equipo_id, jugador_id, dorsal, activo) VALUES (?, ?, ?, TRUE)
                ON CONFLICT (equipo_id, jugador_id) DO UPDATE SET dorsal = EXCLUDED.dorsal, activo = TRUE""", equipoId, jugadorId, dorsal);
    }

    @Override
    public void actualizarDorsal(long equipoId, long jugadorId, Integer dorsal) {
        jdbc.update("UPDATE equipo_jugadores SET dorsal = ? WHERE equipo_id = ? AND jugador_id = ?", dorsal, equipoId, jugadorId);
    }

    @Override
    public void quitarDePlantilla(long equipoId, long jugadorId) {
        jdbc.update("UPDATE equipo_jugadores SET activo = FALSE, dorsal = NULL WHERE equipo_id = ? AND jugador_id = ?", equipoId, jugadorId);
    }

    @Override
    public boolean dorsalEnUso(long equipoId, int dorsal, Long ignorarJugadorId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM equipo_jugadores WHERE equipo_id = ? AND activo AND dorsal = ? AND (?::bigint IS NULL OR jugador_id <> ?))""",
                Boolean.class, equipoId, dorsal, ignorarJugadorId, ignorarJugadorId));
    }

    // ----- Mapeo -----

    static Equipo equipo(ResultSet rs) throws SQLException {
        return new Equipo(rs.getLong("id"), rs.getString("nombre"), rs.getString("color"), rs.getString("delegado_nombre"),
                rs.getString("delegado_telefono"), (Long) rs.getObject("usuario_responsable_id"), rs.getBoolean("activo"));
    }

    static Jugador jugador(ResultSet rs) throws SQLException {
        return new Jugador(rs.getLong("id"), rs.getString("nombres"), rs.getString("apellidos"), rs.getString("documento"), rs.getString("posicion"),
                (Long) rs.getObject("usuario_id"), rs.getBoolean("activo"));
    }

    static String escaparLike(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
