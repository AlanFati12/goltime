package co.edu.usco.goltime.autenticacion.infraestructura;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import co.edu.usco.goltime.autenticacion.aplicacion.RepositorioVerificacionesTelefono;
import co.edu.usco.goltime.autenticacion.dominio.VerificacionTelefono;

@Repository
class RepositorioVerificacionesTelefonoJdbc implements RepositorioVerificacionesTelefono {

    private final JdbcTemplate jdbc;

    RepositorioVerificacionesTelefonoJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static OffsetDateTime utc(Instant i) {
        return i == null ? null : OffsetDateTime.ofInstant(i, ZoneOffset.UTC);
    }

    @Override
    public VerificacionTelefono guardar(VerificacionTelefono v) {
        if (v.id() == null) {
            Long id = jdbc.queryForObject("""
                    INSERT INTO verificaciones_telefono (usuario_id, telefono, codigo_hash, intentos, expira_en, usado_en)
                    VALUES (?, ?, ?, ?, ?, ?) RETURNING id""", Long.class,
                    v.usuarioId(), v.telefono(), v.codigoHash(), v.intentos(), utc(v.expiraEn()), utc(v.usadoEn()));
            return new VerificacionTelefono(id, v.usuarioId(), v.telefono(), v.codigoHash(), v.intentos(), v.expiraEn(), v.usadoEn());
        }
        jdbc.update("UPDATE verificaciones_telefono SET intentos = ?, usado_en = ? WHERE id = ?", v.intentos(), utc(v.usadoEn()), v.id());
        return v;
    }

    @Override
    public List<VerificacionTelefono> pendientesDe(String telefono) {
        return jdbc.query("SELECT * FROM verificaciones_telefono WHERE telefono = ? AND usado_en IS NULL ORDER BY id",
                (rs, i) -> verificacion(rs), telefono);
    }

    @Override
    public int contarCreadasDesde(String telefono, Instant desde) {
        Integer n = jdbc.queryForObject("SELECT count(*) FROM verificaciones_telefono WHERE telefono = ? AND creado_en >= ?", Integer.class,
                telefono, utc(desde));
        return n == null ? 0 : n;
    }

    @Override
    public void anularPendientes(long usuarioId, String telefono, Instant ahora) {
        // Se marcan como usados con intentos agotados: así no cuentan como verificación exitosa en ninguna consulta.
        jdbc.update("UPDATE verificaciones_telefono SET usado_en = ?, intentos = ? WHERE usuario_id = ? AND telefono = ? AND usado_en IS NULL",
                utc(ahora), VerificacionTelefono.MAX_INTENTOS, usuarioId, telefono);
    }

    private static VerificacionTelefono verificacion(ResultSet rs) throws SQLException {
        OffsetDateTime usado = rs.getObject("usado_en", OffsetDateTime.class);
        return new VerificacionTelefono(rs.getLong("id"), rs.getLong("usuario_id"), rs.getString("telefono"), rs.getString("codigo_hash"),
                rs.getInt("intentos"), rs.getObject("expira_en", OffsetDateTime.class).toInstant(), usado == null ? null : usado.toInstant());
    }
}
