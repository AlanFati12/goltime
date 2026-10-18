package co.edu.usco.goltime.promociones.infraestructura;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import co.edu.usco.goltime.compartido.dominio.TipoDescuento;
import co.edu.usco.goltime.promociones.aplicacion.RepositorioPromociones;
import co.edu.usco.goltime.promociones.dominio.Promocion;

@Repository
class RepositorioPromocionesJdbc implements RepositorioPromociones {

    private final JdbcTemplate jdbc;

    RepositorioPromocionesJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Promocion guardar(Promocion p) {
        Short[] dias = p.diasSemana().stream().sorted().map(Integer::shortValue).toArray(Short[]::new);
        if (p.id() == null) {
            Long id = jdbc.execute((java.sql.Connection con) -> {
                try (var ps = con.prepareStatement("""
                        INSERT INTO promociones (nombre, descripcion, codigo, tipo, valor, fecha_desde, fecha_hasta, dias_semana, minuto_inicio, minuto_fin,
                            sede_id, cancha_id, usos_maximos, usos_por_cliente, solo_primera_reserva, publica, activa)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id""")) {
                    parametros(con, ps, p, dias);
                    try (var rs = ps.executeQuery()) {
                        rs.next();
                        return rs.getLong(1);
                    }
                }
            });
            return buscar(id).orElseThrow();
        }
        jdbc.execute((java.sql.Connection con) -> {
            try (var ps = con.prepareStatement("""
                    UPDATE promociones SET nombre = ?, descripcion = ?, codigo = ?, tipo = ?, valor = ?, fecha_desde = ?, fecha_hasta = ?, dias_semana = ?,
                        minuto_inicio = ?, minuto_fin = ?, sede_id = ?, cancha_id = ?, usos_maximos = ?, usos_por_cliente = ?, solo_primera_reserva = ?,
                        publica = ?, activa = ?, actualizado_en = now() WHERE id = ?""")) {
                parametros(con, ps, p, dias);
                ps.setLong(18, p.id());
                return ps.executeUpdate();
            }
        });
        return buscar(p.id()).orElseThrow();
    }

    private static void parametros(java.sql.Connection con, java.sql.PreparedStatement ps, Promocion p, Short[] dias) throws SQLException {
        ps.setString(1, p.nombre());
        ps.setString(2, p.descripcion());
        ps.setString(3, p.codigo());
        ps.setString(4, p.tipo().name());
        ps.setInt(5, p.valor());
        ps.setObject(6, p.fechaDesde());
        ps.setObject(7, p.fechaHasta());
        ps.setArray(8, con.createArrayOf("smallint", dias));
        ps.setObject(9, p.minutoInicio());
        ps.setObject(10, p.minutoFin());
        ps.setObject(11, p.sedeId());
        ps.setObject(12, p.canchaId());
        ps.setObject(13, p.usosMaximos());
        ps.setObject(14, p.usosPorCliente());
        ps.setBoolean(15, p.soloPrimeraReserva());
        ps.setBoolean(16, p.publica());
        ps.setBoolean(17, p.activa());
    }

    @Override
    public Optional<Promocion> buscar(long id) {
        return jdbc.query("SELECT * FROM promociones WHERE id = ?", (rs, i) -> promocion(rs), id).stream().findFirst();
    }

    @Override
    public Optional<Promocion> buscarPorCodigo(String codigo) {
        return jdbc.query("SELECT * FROM promociones WHERE lower(codigo) = lower(?)", (rs, i) -> promocion(rs), codigo).stream().findFirst();
    }

    @Override
    public boolean existeCodigo(String codigo, Long ignorarId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM promociones WHERE lower(codigo) = lower(?) AND (?::bigint IS NULL OR id <> ?))",
                Boolean.class, codigo, ignorarId, ignorarId));
    }

    @Override
    public List<Promocion> listar(Boolean activa) {
        return jdbc.query("SELECT * FROM promociones WHERE (?::boolean IS NULL OR activa = ?) ORDER BY activa DESC, fecha_hasta DESC, id DESC",
                (rs, i) -> promocion(rs), activa, activa);
    }

    @Override
    public List<Promocion> automaticasVigentes(LocalDate fecha) {
        return jdbc.query("SELECT * FROM promociones WHERE activa AND codigo IS NULL AND fecha_desde <= ? AND fecha_hasta >= ? ORDER BY id", (rs, i) -> promocion(rs),
                fecha, fecha);
    }

    @Override
    public List<Promocion> publicasVigentes(LocalDate hoy) {
        return jdbc.query("SELECT * FROM promociones WHERE activa AND publica AND fecha_hasta >= ? ORDER BY fecha_desde, id", (rs, i) -> promocion(rs), hoy);
    }

    @Override
    public void bloquear(long promocionId) {
        jdbc.queryForList("SELECT id FROM promociones WHERE id = ? FOR UPDATE", Long.class, promocionId);
    }

    @Override
    public int usosActivos(long promocionId) {
        Integer n = jdbc.queryForObject("SELECT count(*) FROM usos_promocion WHERE promocion_id = ? AND estado = 'ACTIVO'", Integer.class, promocionId);
        return n == null ? 0 : n;
    }

    @Override
    public int usosActivosDeCliente(long promocionId, long clienteId) {
        Integer n = jdbc.queryForObject("SELECT count(*) FROM usos_promocion WHERE promocion_id = ? AND cliente_id = ? AND estado = 'ACTIVO'", Integer.class, promocionId,
                clienteId);
        return n == null ? 0 : n;
    }

    @Override
    public void registrarUso(long promocionId, long reservaId, long clienteId, int monto) {
        jdbc.update("INSERT INTO usos_promocion (promocion_id, reserva_id, cliente_id, monto) VALUES (?, ?, ?, ?)", promocionId, reservaId, clienteId, monto);
    }

    @Override
    public void liberarUsosDeReserva(long reservaId) {
        jdbc.update("UPDATE usos_promocion SET estado = 'LIBERADO' WHERE reserva_id = ? AND estado = 'ACTIVO'", reservaId);
    }

    @Override
    public ResumenUsos resumenUsos(long promocionId) {
        return jdbc.queryForObject("SELECT count(*) AS n, coalesce(sum(monto), 0) AS total FROM usos_promocion WHERE promocion_id = ? AND estado = 'ACTIVO'",
                (rs, i) -> new ResumenUsos(rs.getInt("n"), rs.getLong("total")), promocionId);
    }

    @Override
    public List<Uso> ultimosUsos(long promocionId, int limite) {
        return jdbc.query("SELECT * FROM usos_promocion WHERE promocion_id = ? ORDER BY creado_en DESC, id DESC LIMIT ?",
                (rs, i) -> new Uso(rs.getLong("promocion_id"), rs.getLong("reserva_id"), rs.getLong("cliente_id"), rs.getInt("monto"), rs.getString("estado"),
                        rs.getObject("creado_en", OffsetDateTime.class).toInstant()), promocionId, limite);
    }

    private static Promocion promocion(ResultSet rs) throws SQLException {
        Set<Integer> dias = new HashSet<>();
        Array arreglo = rs.getArray("dias_semana");
        if (arreglo != null) {
            for (Object d : (Object[]) arreglo.getArray()) {
                dias.add(((Number) d).intValue());
            }
        }
        return new Promocion(rs.getLong("id"), rs.getString("nombre"), rs.getString("descripcion"), rs.getString("codigo"), TipoDescuento.valueOf(rs.getString("tipo")),
                rs.getInt("valor"), rs.getObject("fecha_desde", LocalDate.class), rs.getObject("fecha_hasta", LocalDate.class), dias, (Integer) rs.getObject("minuto_inicio"),
                (Integer) rs.getObject("minuto_fin"), (Long) rs.getObject("sede_id"), (Long) rs.getObject("cancha_id"), (Integer) rs.getObject("usos_maximos"),
                (Integer) rs.getObject("usos_por_cliente"), rs.getBoolean("solo_primera_reserva"), rs.getBoolean("publica"), rs.getBoolean("activa"));
    }
}
