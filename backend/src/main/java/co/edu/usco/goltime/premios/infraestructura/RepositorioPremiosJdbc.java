package co.edu.usco.goltime.premios.infraestructura;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import co.edu.usco.goltime.compartido.dominio.TipoDescuento;
import co.edu.usco.goltime.premios.aplicacion.RepositorioPremios;
import co.edu.usco.goltime.premios.dominio.EstadoPremio;
import co.edu.usco.goltime.premios.dominio.ReglaPremio;

@Repository
class RepositorioPremiosJdbc implements RepositorioPremios {

    private final JdbcTemplate jdbc;

    RepositorioPremiosJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static OffsetDateTime utc(Instant i) {
        return OffsetDateTime.ofInstant(i, ZoneOffset.UTC);
    }

    @Override
    public ReglaPremio guardarRegla(ReglaPremio r) {
        if (r.id() == null) {
            Long id = jdbc.queryForObject("""
                    INSERT INTO reglas_premio (nombre, descripcion, reservas_requeridas, tipo, valor, vigencia_dias, activa) VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id""",
                    Long.class, r.nombre(), r.descripcion(), r.reservasRequeridas(), r.tipo().name(), r.valor(), r.vigenciaDias(), r.activa());
            return regla(id).orElseThrow();
        }
        jdbc.update("""
                UPDATE reglas_premio SET nombre = ?, descripcion = ?, reservas_requeridas = ?, tipo = ?, valor = ?, vigencia_dias = ?, activa = ?,
                       actualizado_en = now() WHERE id = ?""", r.nombre(), r.descripcion(), r.reservasRequeridas(), r.tipo().name(), r.valor(), r.vigenciaDias(),
                r.activa(), r.id());
        return regla(r.id()).orElseThrow();
    }

    @Override
    public Optional<ReglaPremio> regla(long id) {
        return jdbc.query("SELECT * FROM reglas_premio WHERE id = ?", (rs, i) -> regla(rs), id).stream().findFirst();
    }

    @Override
    public List<ReglaPremio> reglas(boolean soloActivas) {
        return jdbc.query("SELECT * FROM reglas_premio" + (soloActivas ? " WHERE activa" : "") + " ORDER BY reservas_requeridas, id", (rs, i) -> regla(rs));
    }

    @Override
    public PremioOtorgado otorgar(long clienteId, ReglaPremio r, Instant venceEn) {
        Long id = jdbc.queryForObject("""
                INSERT INTO premios_cliente (cliente_id, regla_id, nombre, tipo, valor, vence_en) VALUES (?, ?, ?, ?, ?, ?) RETURNING id""", Long.class, clienteId,
                r.id(), r.nombre(), r.tipo().name(), r.valor(), utc(venceEn));
        return jdbc.query("SELECT * FROM premios_cliente WHERE id = ?", (rs, i) -> premio(rs), id).get(0);
    }

    @Override
    public int contarOtorgados(long clienteId, long reglaId) {
        Integer n = jdbc.queryForObject("SELECT count(*) FROM premios_cliente WHERE cliente_id = ? AND regla_id = ? AND estado <> 'REVOCADO'", Integer.class, clienteId,
                reglaId);
        return n == null ? 0 : n;
    }

    @Override
    public List<PremioOtorgado> disponibles(long clienteId, long reglaId) {
        return jdbc.query("SELECT * FROM premios_cliente WHERE cliente_id = ? AND regla_id = ? AND estado = 'DISPONIBLE' ORDER BY otorgado_en DESC, id DESC",
                (rs, i) -> premio(rs), clienteId, reglaId);
    }

    @Override
    public Optional<PremioOtorgado> buscar(long id) {
        return jdbc.query("SELECT * FROM premios_cliente WHERE id = ?", (rs, i) -> premio(rs), id).stream().findFirst();
    }

    @Override
    public Optional<PremioOtorgado> buscarBloqueando(long id) {
        return jdbc.query("SELECT * FROM premios_cliente WHERE id = ? FOR UPDATE", (rs, i) -> premio(rs), id).stream().findFirst();
    }

    @Override
    public void revocar(long id, String motivo) {
        jdbc.update("UPDATE premios_cliente SET estado = 'REVOCADO', motivo_revocacion = ? WHERE id = ? AND estado = 'DISPONIBLE'", motivo, id);
    }

    @Override
    public List<PremioOtorgado> deCliente(long clienteId) {
        return jdbc.query("SELECT * FROM premios_cliente WHERE cliente_id = ? AND estado <> 'REVOCADO' ORDER BY otorgado_en DESC, id DESC", (rs, i) -> premio(rs),
                clienteId);
    }

    @Override
    public List<PremioOtorgado> listar(Long clienteId, EstadoPremio estado, int limite) {
        return jdbc.query("""
                SELECT * FROM premios_cliente WHERE (?::bigint IS NULL OR cliente_id = ?) AND (?::text IS NULL OR estado = ?)
                ORDER BY otorgado_en DESC, id DESC LIMIT ?""", (rs, i) -> premio(rs), clienteId, clienteId, estado == null ? null : estado.name(),
                estado == null ? null : estado.name(), limite);
    }

    @Override
    public boolean marcarUsado(long id, long reservaId) {
        return jdbc.update("UPDATE premios_cliente SET estado = 'USADO', reserva_uso_id = ? WHERE id = ? AND estado = 'DISPONIBLE'", reservaId, id) == 1;
    }

    @Override
    public void liberarDeReserva(long reservaId, Instant ahora) {
        jdbc.update("""
                UPDATE premios_cliente SET estado = CASE WHEN vence_en > ? THEN 'DISPONIBLE' ELSE 'VENCIDO' END, reserva_uso_id = NULL
                WHERE reserva_uso_id = ? AND estado = 'USADO'""", utc(ahora), reservaId);
    }

    @Override
    public int vencer(Instant ahora) {
        return jdbc.update("UPDATE premios_cliente SET estado = 'VENCIDO' WHERE estado = 'DISPONIBLE' AND vence_en <= ?", utc(ahora));
    }

    private static ReglaPremio regla(ResultSet rs) throws SQLException {
        return new ReglaPremio(rs.getLong("id"), rs.getString("nombre"), rs.getString("descripcion"), rs.getInt("reservas_requeridas"),
                TipoDescuento.valueOf(rs.getString("tipo")), rs.getInt("valor"), rs.getInt("vigencia_dias"), rs.getBoolean("activa"));
    }

    private static PremioOtorgado premio(ResultSet rs) throws SQLException {
        return new PremioOtorgado(rs.getLong("id"), rs.getLong("cliente_id"), rs.getLong("regla_id"), rs.getString("nombre"),
                TipoDescuento.valueOf(rs.getString("tipo")), rs.getInt("valor"), EstadoPremio.valueOf(rs.getString("estado")),
                rs.getObject("otorgado_en", OffsetDateTime.class).toInstant(), rs.getObject("vence_en", OffsetDateTime.class).toInstant(),
                (Long) rs.getObject("reserva_uso_id"), rs.getString("motivo_revocacion"));
    }
}
