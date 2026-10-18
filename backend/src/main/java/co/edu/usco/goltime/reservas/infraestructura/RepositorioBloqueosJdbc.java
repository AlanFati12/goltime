package co.edu.usco.goltime.reservas.infraestructura;

import java.sql.Array;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import co.edu.usco.goltime.reservas.aplicacion.RepositorioBloqueos;
import co.edu.usco.goltime.reservas.dominio.Bloqueo;
import co.edu.usco.goltime.reservas.dominio.MotivoBloqueo;

@Repository
class RepositorioBloqueosJdbc implements RepositorioBloqueos {

    private final JdbcTemplate jdbc;

    RepositorioBloqueosJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Bloqueo guardar(Bloqueo b) {
        Object[] dias = b.diasSemana().isEmpty() ? null : b.diasSemana().stream().map(d -> (Object) (int) d.getValue()).sorted().toArray();
        Long id = b.id();
        if (id == null) {
            id = jdbc.query(con -> {
                var ps = con.prepareStatement("""
                        INSERT INTO bloqueos (sede_id, tipo, inicio, fin, todo_el_dia, dias_semana, minuto_inicio, minuto_fin, fecha_desde,
                            fecha_hasta, motivo_tipo, motivo_detalle, activo)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id""");
                rellenar(ps, con.createArrayOf("smallint", dias), b);
                return ps;
            }, rs -> rs.next() ? rs.getLong(1) : null);
        } else {
            long idActual = id;
            jdbc.update(con -> {
                var ps = con.prepareStatement("""
                        UPDATE bloqueos SET sede_id = ?, tipo = ?, inicio = ?, fin = ?, todo_el_dia = ?, dias_semana = ?, minuto_inicio = ?,
                            minuto_fin = ?, fecha_desde = ?, fecha_hasta = ?, motivo_tipo = ?, motivo_detalle = ?, activo = ?,
                            version = version + 1, actualizado_en = now() WHERE id = ?""");
                rellenar(ps, con.createArrayOf("smallint", dias), b);
                ps.setLong(14, idActual);
                return ps;
            });
        }
        jdbc.update("DELETE FROM bloqueo_canchas WHERE bloqueo_id = ?", id);
        for (long canchaId : b.canchaIds()) {
            jdbc.update("INSERT INTO bloqueo_canchas (bloqueo_id, cancha_id) VALUES (?, ?)", id, canchaId);
        }
        return buscarPorId(id).orElseThrow();
    }

    private static void rellenar(java.sql.PreparedStatement ps, Array dias, Bloqueo b) throws SQLException {
        ps.setLong(1, b.sedeId());
        ps.setString(2, b.tipo().name());
        ps.setObject(3, b.inicio() == null ? null : OffsetDateTime.ofInstant(b.inicio(), ZoneOffset.UTC));
        ps.setObject(4, b.fin() == null ? null : OffsetDateTime.ofInstant(b.fin(), ZoneOffset.UTC));
        ps.setBoolean(5, b.todoElDia());
        if (b.diasSemana().isEmpty()) {
            ps.setNull(6, java.sql.Types.ARRAY);
        } else {
            ps.setArray(6, dias);
        }
        if (b.tipo() == Bloqueo.Tipo.RECURRENTE) {
            ps.setInt(7, b.minutoInicio());
            ps.setInt(8, b.minutoFin());
        } else {
            ps.setNull(7, java.sql.Types.INTEGER);
            ps.setNull(8, java.sql.Types.INTEGER);
        }
        ps.setObject(9, b.fechaDesde());
        ps.setObject(10, b.fechaHasta());
        ps.setString(11, b.motivoTipo().name());
        ps.setString(12, b.motivoDetalle());
        ps.setBoolean(13, b.activo());
    }

    @Override
    public Optional<Bloqueo> buscarPorId(long id) {
        return jdbc.query("SELECT * FROM bloqueos WHERE id = ?", (rs, i) -> mapear(rs), id).stream().findFirst();
    }

    @Override
    public List<Bloqueo> listar(Long sedeId, boolean soloActivos) {
        StringBuilder sql = new StringBuilder("SELECT * FROM bloqueos WHERE 1 = 1");
        List<Object> parametros = new ArrayList<>();
        if (sedeId != null) {
            sql.append(" AND sede_id = ?");
            parametros.add(sedeId);
        }
        if (soloActivos) {
            sql.append(" AND activo");
        }
        sql.append(" ORDER BY activo DESC, coalesce(inicio, fecha_desde::timestamptz) DESC, id DESC");
        return jdbc.query(sql.toString(), (rs, i) -> mapear(rs), parametros.toArray());
    }

    private Bloqueo mapear(java.sql.ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        Set<Long> canchas = new HashSet<>(jdbc.queryForList("SELECT cancha_id FROM bloqueo_canchas WHERE bloqueo_id = ?", Long.class, id));
        Set<DayOfWeek> dias = EnumSet.noneOf(DayOfWeek.class);
        Array arreglo = rs.getArray("dias_semana");
        if (arreglo != null) {
            for (Object o : (Object[]) arreglo.getArray()) {
                dias.add(DayOfWeek.of(((Number) o).intValue()));
            }
        }
        OffsetDateTime inicio = rs.getObject("inicio", OffsetDateTime.class);
        OffsetDateTime fin = rs.getObject("fin", OffsetDateTime.class);
        return Bloqueo.reconstruir(id, rs.getLong("sede_id"), canchas, Bloqueo.Tipo.valueOf(rs.getString("tipo")),
                inicio == null ? null : inicio.toInstant(), fin == null ? null : fin.toInstant(), rs.getBoolean("todo_el_dia"), dias,
                rs.getInt("minuto_inicio"), rs.getInt("minuto_fin"), rs.getObject("fecha_desde", java.time.LocalDate.class),
                rs.getObject("fecha_hasta", java.time.LocalDate.class), MotivoBloqueo.valueOf(rs.getString("motivo_tipo")),
                rs.getString("motivo_detalle"), rs.getBoolean("activo"));
    }
}
