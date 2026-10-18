package co.edu.usco.goltime.reservas.infraestructura;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import co.edu.usco.goltime.reservas.aplicacion.RepositorioPagos;
import co.edu.usco.goltime.reservas.dominio.MetodoPago;
import co.edu.usco.goltime.reservas.dominio.Pago;
import co.edu.usco.goltime.reservas.dominio.TipoPago;

@Repository
class RepositorioPagosJdbc implements RepositorioPagos {

    private final JdbcTemplate jdbc;

    RepositorioPagosJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Pago registrar(Pago p) {
        KeyHolder llave = new GeneratedKeyHolder();
        jdbc.update(con -> {
            var ps = con.prepareStatement("INSERT INTO pagos (reserva_id, monto, metodo, referencia, tipo, registrado_por, fecha) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    new String[] {"id"});
            ps.setLong(1, p.reservaId());
            ps.setInt(2, p.monto());
            ps.setString(3, p.metodo().name());
            ps.setString(4, p.referencia());
            ps.setString(5, p.tipo().name());
            ps.setLong(6, p.registradoPor());
            ps.setObject(7, OffsetDateTime.ofInstant(p.fecha(), ZoneOffset.UTC));
            return ps;
        }, llave);
        return new Pago(llave.getKey().longValue(), p.reservaId(), p.monto(), p.metodo(), p.referencia(), p.tipo(), p.registradoPor(), p.fecha());
    }

    @Override
    public List<PagoRegistrado> deReserva(long reservaId) {
        return jdbc.query("""
                SELECT p.*, u.nombres || ' ' || u.apellidos AS registrado_por_nombre
                FROM pagos p LEFT JOIN usuarios u ON u.id = p.registrado_por
                WHERE p.reserva_id = ? ORDER BY p.fecha, p.id""", (rs, i) -> new PagoRegistrado(
                new Pago(rs.getLong("id"), rs.getLong("reserva_id"), rs.getInt("monto"), MetodoPago.valueOf(rs.getString("metodo")),
                        rs.getString("referencia"), TipoPago.valueOf(rs.getString("tipo")), rs.getLong("registrado_por"),
                        rs.getTimestamp("fecha").toInstant()), rs.getString("registrado_por_nombre")), reservaId);
    }
}
