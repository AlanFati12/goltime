package co.edu.usco.goltime.reservas.infraestructura;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import co.edu.usco.goltime.reservas.aplicacion.RepositorioAgenda;

@Repository
class RepositorioAgendaJdbc implements RepositorioAgenda {

    private final JdbcTemplate jdbc;

    RepositorioAgendaJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<ElementoAgenda> elementos(long sedeId, Instant desde, Instant hasta) {
        return jdbc.query("""
                SELECT o.cancha_id, lower(o.rango) AS inicio, upper(o.rango) AS fin, o.tipo,
                       r.id AS reserva_id, r.codigo, r.estado, coalesce(u.nombres || ' ' || u.apellidos, m.nombre) AS cliente,
                       r.nombre_equipo, r.precio_final, r.estado_pago, r.origen,
                       CASE WHEN r.cancha_id <> o.cancha_id THEN cr.nombre END AS cancha_reserva,
                       b.id AS bloqueo_id,
                       CASE WHEN b.id IS NULL THEN NULL WHEN b.motivo_detalle IS NULL THEN b.motivo_tipo ELSE b.motivo_tipo || ': ' || b.motivo_detalle END AS motivo
                FROM ocupaciones_cancha o
                LEFT JOIN reservas r ON r.id = o.reserva_id
                LEFT JOIN canchas cr ON cr.id = r.cancha_id
                LEFT JOIN usuarios u ON u.id = r.cliente_id
                LEFT JOIN clientes_mostrador m ON m.id = r.cliente_mostrador_id
                LEFT JOIN bloqueos b ON b.id = o.bloqueo_id
                WHERE o.cancha_id IN (SELECT id FROM canchas WHERE sede_id = ?)
                  AND o.rango && tstzrange(?, ?, '[)')
                ORDER BY lower(o.rango), o.cancha_id""",
                (rs, i) -> new ElementoAgenda(rs.getString("tipo"), rs.getLong("cancha_id"), rs.getObject("inicio", OffsetDateTime.class).toInstant(),
                        rs.getObject("fin", OffsetDateTime.class).toInstant(), (Long) rs.getObject("reserva_id"), rs.getString("codigo"),
                        rs.getString("estado"), rs.getString("cliente"), rs.getString("nombre_equipo"), (Integer) rs.getObject("precio_final"),
                        rs.getString("estado_pago"), rs.getString("origen"), rs.getString("cancha_reserva"), (Long) rs.getObject("bloqueo_id"),
                        rs.getString("motivo")),
                sedeId, OffsetDateTime.ofInstant(desde, ZoneOffset.UTC), OffsetDateTime.ofInstant(hasta, ZoneOffset.UTC));
    }
}
