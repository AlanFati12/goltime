package co.edu.usco.goltime.sedes.aplicacion;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Canchas favoritas del cliente (acceso rápido para volver a reservarlas). Idempotente: marcar dos veces no falla. */
@Service
public class ServicioFavoritos {

    public record Favorita(long canchaId, String cancha, String modalidad, long sedeId, String sede, int precioBaseHora, boolean activa) {
    }

    private final JdbcTemplate jdbc;
    private final ServicioCanchas canchas;

    public ServicioFavoritos(JdbcTemplate jdbc, ServicioCanchas canchas) {
        this.jdbc = jdbc;
        this.canchas = canchas;
    }

    @Transactional
    public void marcar(long usuarioId, long canchaId) {
        canchas.detallePublico(canchaId); // solo canchas visibles al público
        jdbc.update("INSERT INTO favoritos (usuario_id, cancha_id) VALUES (?, ?) ON CONFLICT DO NOTHING", usuarioId, canchaId);
    }

    @Transactional
    public void quitar(long usuarioId, long canchaId) {
        jdbc.update("DELETE FROM favoritos WHERE usuario_id = ? AND cancha_id = ?", usuarioId, canchaId);
    }

    @Transactional(readOnly = true)
    public List<Favorita> deUsuario(long usuarioId) {
        return jdbc.query("""
                SELECT c.id, c.nombre, c.modalidad, s.id AS sede_id, s.nombre AS sede, c.precio_base_hora, (c.estado = 'ACTIVA' AND s.activa) AS activa
                FROM favoritos f JOIN canchas c ON c.id = f.cancha_id JOIN sedes s ON s.id = c.sede_id
                WHERE f.usuario_id = ? ORDER BY f.creado_en DESC""", (rs, i) -> new Favorita(rs.getLong("id"), rs.getString("nombre"), rs.getString("modalidad"),
                rs.getLong("sede_id"), rs.getString("sede"), rs.getInt("precio_base_hora"), rs.getBoolean("activa")), usuarioId);
    }

    @Transactional(readOnly = true)
    public boolean esFavorita(long usuarioId, long canchaId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM favoritos WHERE usuario_id = ? AND cancha_id = ?)", Boolean.class, usuarioId, canchaId));
    }
}
