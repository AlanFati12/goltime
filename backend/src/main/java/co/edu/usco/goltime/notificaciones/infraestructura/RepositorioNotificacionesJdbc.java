package co.edu.usco.goltime.notificaciones.infraestructura;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import co.edu.usco.goltime.compartido.dominio.Pagina;
import co.edu.usco.goltime.compartido.dominio.SolicitudPagina;
import co.edu.usco.goltime.notificaciones.aplicacion.RepositorioNotificaciones;
import co.edu.usco.goltime.notificaciones.dominio.Notificacion;

@Repository
class RepositorioNotificacionesJdbc implements RepositorioNotificaciones {

    private static final RowMapper<Notificacion> MAPEADOR = (rs, i) -> new Notificacion(rs.getLong("id"),
            rs.getLong("usuario_id"), rs.getString("tipo"), rs.getString("titulo"), rs.getString("mensaje"),
            rs.getBoolean("leida"), rs.getString("entidad"), (Long) rs.getObject("entidad_id"),
            rs.getTimestamp("creado_en").toInstant());

    private final JdbcTemplate jdbc;

    RepositorioNotificacionesJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void guardar(Notificacion n) {
        jdbc.update("INSERT INTO notificaciones (usuario_id, tipo, titulo, mensaje, entidad, entidad_id, creado_en) VALUES (?, ?, ?, ?, ?, ?, ?)",
                n.usuarioId(), n.tipo(), n.titulo(), n.mensaje(), n.entidad(), n.entidadId(), Timestamp.from(n.creadoEn()));
    }

    @Override
    public Pagina<Notificacion> listar(long usuarioId, boolean soloNoLeidas, SolicitudPagina pagina) {
        String filtro = soloNoLeidas ? " AND NOT leida" : "";
        long total = jdbc.queryForObject("SELECT count(*) FROM notificaciones WHERE usuario_id = ?" + filtro, Long.class, usuarioId);
        List<Notificacion> filas = jdbc.query("SELECT * FROM notificaciones WHERE usuario_id = ?" + filtro
                + " ORDER BY creado_en DESC, id DESC LIMIT ? OFFSET ?", MAPEADOR, usuarioId, pagina.tamano(),
                (long) pagina.pagina() * pagina.tamano());
        return new Pagina<>(filas, pagina.pagina(), pagina.tamano(), total, (int) Math.ceil(total / (double) pagina.tamano()));
    }

    @Override
    public long contarNoLeidas(long usuarioId) {
        return jdbc.queryForObject("SELECT count(*) FROM notificaciones WHERE usuario_id = ? AND NOT leida", Long.class, usuarioId);
    }

    @Override
    public boolean marcarLeida(long usuarioId, long notificacionId) {
        return jdbc.update("UPDATE notificaciones SET leida = TRUE WHERE id = ? AND usuario_id = ?", notificacionId, usuarioId) > 0;
    }

    @Override
    public int marcarTodasLeidas(long usuarioId) {
        return jdbc.update("UPDATE notificaciones SET leida = TRUE WHERE usuario_id = ? AND NOT leida", usuarioId);
    }
}
