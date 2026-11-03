package co.edu.usco.goltime.autenticacion.infraestructura;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import co.edu.usco.goltime.autenticacion.aplicacion.RepositorioCodigosVinculacion;

@Repository
class RepositorioCodigosVinculacionJdbc implements RepositorioCodigosVinculacion {

    private final JdbcTemplate jdbc;

    RepositorioCodigosVinculacionJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static OffsetDateTime utc(Instant i) {
        return OffsetDateTime.ofInstant(i, ZoneOffset.UTC);
    }

    @Override
    public boolean guardar(long usuarioId, String codigoHash, Instant expiraEn) {
        // ON CONFLICT y no capturar la excepción: en PostgreSQL un error aborta toda la transacción.
        return jdbc.update("INSERT INTO codigos_vinculacion_whatsapp (usuario_id, codigo_hash, expira_en) VALUES (?, ?, ?) "
                + "ON CONFLICT (codigo_hash) DO NOTHING", usuarioId, codigoHash, utc(expiraEn)) == 1;
    }

    @Override
    public Optional<Long> usuarioDeVigente(String codigoHash, Instant ahora) {
        return jdbc.queryForList("SELECT usuario_id FROM codigos_vinculacion_whatsapp WHERE codigo_hash = ? AND usado_en IS NULL AND expira_en > ?",
                Long.class, codigoHash, utc(ahora)).stream().findFirst();
    }

    @Override
    public void marcarUsado(String codigoHash, Instant ahora) {
        jdbc.update("UPDATE codigos_vinculacion_whatsapp SET usado_en = ? WHERE codigo_hash = ?", utc(ahora), codigoHash);
    }

    @Override
    public void anularDeUsuario(long usuarioId, Instant ahora) {
        jdbc.update("UPDATE codigos_vinculacion_whatsapp SET usado_en = ? WHERE usuario_id = ? AND usado_en IS NULL", utc(ahora), usuarioId);
    }

    @Override
    public int contarGeneradosDesde(long usuarioId, Instant desde) {
        Integer n = jdbc.queryForObject("SELECT count(*) FROM codigos_vinculacion_whatsapp WHERE usuario_id = ? AND creado_en >= ?", Integer.class,
                usuarioId, utc(desde));
        return n == null ? 0 : n;
    }

    @Override
    public void registrarIntentoFallido(String remitente) {
        jdbc.update("INSERT INTO intentos_vinculacion_whatsapp (remitente) VALUES (?)", remitente);
    }

    @Override
    public int contarIntentosFallidosDesde(String remitente, Instant desde) {
        Integer n = jdbc.queryForObject("SELECT count(*) FROM intentos_vinculacion_whatsapp WHERE remitente = ? AND creado_en >= ?", Integer.class,
                remitente, utc(desde));
        return n == null ? 0 : n;
    }
}
