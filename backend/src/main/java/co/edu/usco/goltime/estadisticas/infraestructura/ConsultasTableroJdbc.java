package co.edu.usco.goltime.estadisticas.infraestructura;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import co.edu.usco.goltime.estadisticas.aplicacion.Tablero.Clientes;
import co.edu.usco.goltime.estadisticas.aplicacion.Tablero.Cobros;
import co.edu.usco.goltime.estadisticas.aplicacion.Tablero.Consultas;
import co.edu.usco.goltime.estadisticas.aplicacion.Tablero.DiaProximo;
import co.edu.usco.goltime.estadisticas.aplicacion.Tablero.EstadoCancha;
import co.edu.usco.goltime.estadisticas.aplicacion.Tablero.Hoy;
import co.edu.usco.goltime.estadisticas.aplicacion.Tablero.ReservaConSaldo;
import co.edu.usco.goltime.estadisticas.aplicacion.Tablero.Torneos;

/** Consultas del tablero del administrador. Modelo de lectura: no modifica nada. */
@Repository
class ConsultasTableroJdbc implements Consultas {

    private final JdbcTemplate jdbc;

    ConsultasTableroJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static OffsetDateTime utc(Instant i) {
        return OffsetDateTime.ofInstant(i, ZoneOffset.UTC);
    }

    private static Instant instante(Timestamp t) {
        return t == null ? null : t.toInstant();
    }

    @Override
    public Hoy hoy(Instant inicioDia, Instant finDia) {
        Hoy reservas = jdbc.queryForObject("""
                SELECT count(*) FILTER (WHERE estado = 'PENDIENTE' AND origen <> 'TORNEO') AS pendientes,
                       count(*) FILTER (WHERE estado = 'CONFIRMADA' AND origen <> 'TORNEO') AS confirmadas,
                       count(*) FILTER (WHERE estado = 'COMPLETADA' AND origen <> 'TORNEO') AS completadas,
                       count(*) FILTER (WHERE estado = 'NO_ASISTIO' AND origen <> 'TORNEO') AS no_asistio,
                       count(*) FILTER (WHERE estado = 'CANCELADA' AND origen <> 'TORNEO') AS canceladas,
                       count(*) FILTER (WHERE origen = 'TORNEO' AND estado IN ('CONFIRMADA', 'COMPLETADA')) AS torneo,
                       coalesce(sum(precio_final) FILTER (WHERE estado IN ('CONFIRMADA', 'COMPLETADA', 'NO_ASISTIO') AND origen <> 'TORNEO'), 0) AS valor,
                       coalesce(sum(extract(epoch FROM (fin - inicio)) / 3600.0) FILTER (WHERE estado IN ('CONFIRMADA', 'COMPLETADA', 'NO_ASISTIO')), 0) AS horas
                FROM reservas WHERE inicio >= ? AND inicio < ?""",
                (rs, n) -> new Hoy(rs.getInt("pendientes"), rs.getInt("confirmadas"), rs.getInt("completadas"), rs.getInt("no_asistio"),
                        rs.getInt("canceladas"), rs.getInt("torneo"), rs.getLong("valor"), 0, rs.getDouble("horas")),
                utc(inicioDia), utc(finDia));
        Long recaudado = jdbc.queryForObject("""
                SELECT coalesce(sum(CASE WHEN tipo = 'REEMBOLSO' THEN -monto ELSE monto END), 0) FROM pagos WHERE fecha >= ? AND fecha < ?""",
                Long.class, utc(inicioDia), utc(finDia));
        return new Hoy(reservas.pendientes(), reservas.confirmadas(), reservas.completadas(), reservas.noAsistio(), reservas.canceladas(),
                reservas.partidosTorneo(), reservas.valorReservado(), recaudado == null ? 0 : recaudado, reservas.horasReservadas());
    }

    @Override
    public int pendientesPorVencer(Instant ahora, Instant limite) {
        Integer n = jdbc.queryForObject("SELECT count(*) FROM reservas WHERE estado = 'PENDIENTE' AND expira_en >= ? AND expira_en < ?", Integer.class,
                utc(ahora), utc(limite));
        return n == null ? 0 : n;
    }

    private static final String CON_SALDO = " " + """
             FROM reservas r
             LEFT JOIN usuarios u ON u.id = r.cliente_id
             LEFT JOIN clientes_mostrador m ON m.id = r.cliente_mostrador_id
             WHERE r.estado IN ('CONFIRMADA', 'COMPLETADA', 'NO_ASISTIO') AND r.origen <> 'TORNEO' AND r.fin <= ?
               AND r.precio_final > r.monto_pagado AND r.estado_pago NOT IN ('REEMBOLSO_PENDIENTE', 'REEMBOLSADO')""";

    @Override
    public Cobros cobros(Instant ahora, int limite) {
        record Totales(int reservas, long saldo) {
        }
        Totales saldo = jdbc.queryForObject("SELECT count(*) AS n, coalesce(sum(r.precio_final - r.monto_pagado), 0) AS saldo" + CON_SALDO,
                (rs, i) -> new Totales(rs.getInt("n"), rs.getLong("saldo")), utc(ahora));
        Totales reembolsos = jdbc.queryForObject("""
                SELECT count(*) AS n, coalesce(sum(monto_pagado), 0) AS saldo FROM reservas WHERE estado_pago = 'REEMBOLSO_PENDIENTE'""",
                (rs, i) -> new Totales(rs.getInt("n"), rs.getLong("saldo")));
        List<ReservaConSaldo> mayores = jdbc.query("""
                SELECT r.id, r.codigo, coalesce(u.nombres || ' ' || u.apellidos, m.nombre) AS cliente, r.inicio,
                       r.precio_final - r.monto_pagado AS saldo""" + CON_SALDO + " ORDER BY saldo DESC, r.inicio LIMIT ?",
                (rs, i) -> new ReservaConSaldo(rs.getLong("id"), rs.getString("codigo"), rs.getString("cliente"), instante(rs.getTimestamp("inicio")),
                        rs.getLong("saldo")), utc(ahora), limite);
        return new Cobros(saldo.reservas(), saldo.saldo(), reembolsos.reservas(), reembolsos.saldo(), mayores);
    }

    @Override
    public List<DiaProximo> proximosDias(LocalDate desde, int dias) {
        return jdbc.query("""
                SELECT d::date AS fecha,
                       count(r.id) FILTER (WHERE r.estado IN ('PENDIENTE', 'CONFIRMADA')) AS reservas,
                       count(r.id) FILTER (WHERE r.estado = 'PENDIENTE') AS pendientes,
                       coalesce(sum(extract(epoch FROM (r.fin - r.inicio)) / 3600.0) FILTER (WHERE r.estado = 'CONFIRMADA'), 0) AS horas
                FROM generate_series(?::date, ?::date, interval '1 day') AS d
                LEFT JOIN reservas r ON (r.inicio AT TIME ZONE 'America/Bogota')::date = d::date AND r.origen <> 'TORNEO'
                GROUP BY d ORDER BY d""",
                (rs, i) -> new DiaProximo(rs.getDate("fecha").toLocalDate(), rs.getInt("reservas"), rs.getInt("pendientes"), rs.getDouble("horas")),
                java.sql.Date.valueOf(desde), java.sql.Date.valueOf(desde.plusDays(dias - 1L)));
    }

    @Override
    public List<EstadoCancha> canchas(Instant ahora, Instant inicioDia, Instant finDia) {
        return jdbc.query("""
                SELECT c.id, c.nombre, s.nombre AS sede, c.estado,
                       (SELECT max(r.fin) FROM reservas r WHERE r.cancha_id = c.id AND r.estado IN ('CONFIRMADA', 'COMPLETADA')
                           AND r.inicio <= ? AND r.fin > ?) AS ocupada_hasta,
                       (SELECT min(r.inicio) FROM reservas r WHERE r.cancha_id = c.id AND r.estado IN ('PENDIENTE', 'CONFIRMADA')
                           AND r.inicio > ?) AS proximo,
                       (SELECT count(*) FROM reservas r WHERE r.cancha_id = c.id AND r.estado IN ('CONFIRMADA', 'COMPLETADA', 'NO_ASISTIO')
                           AND r.inicio >= ? AND r.inicio < ?) AS hoy
                FROM canchas c JOIN sedes s ON s.id = c.sede_id
                WHERE s.activa AND c.estado <> 'INACTIVA'
                ORDER BY s.nombre, c.nombre""",
                (rs, i) -> {
                    Instant hasta = instante(rs.getTimestamp("ocupada_hasta"));
                    return new EstadoCancha(rs.getLong("id"), rs.getString("nombre"), rs.getString("sede"), rs.getString("estado"), hasta != null, hasta,
                            instante(rs.getTimestamp("proximo")), rs.getInt("hoy"));
                }, utc(ahora), utc(ahora), utc(ahora), utc(inicioDia), utc(finDia));
    }

    @Override
    public Torneos torneos(Instant ahora) {
        return jdbc.queryForObject("""
                SELECT (SELECT count(*) FROM torneos WHERE estado = 'EN_CURSO') AS en_curso,
                       (SELECT count(*) FROM torneos WHERE estado = 'INSCRIPCIONES') AS inscripciones,
                       (SELECT count(*) FROM inscripciones_torneo i JOIN torneos t ON t.id = i.torneo_id
                           WHERE i.estado = 'SOLICITADA' AND t.estado IN ('BORRADOR', 'INSCRIPCIONES')) AS solicitudes,
                       (SELECT count(*) FROM partidos p JOIN torneos t ON t.id = p.torneo_id
                           WHERE t.estado = 'EN_CURSO' AND p.inicio IS NULL AND p.estado IN ('PENDIENTE', 'APLAZADO')
                             AND p.local_id IS NOT NULL AND p.visitante_id IS NOT NULL) AS sin_programar,
                       (SELECT count(*) FROM partidos p JOIN torneos t ON t.id = p.torneo_id
                           WHERE t.estado = 'EN_CURSO' AND p.inicio < ? AND p.estado IN ('PROGRAMADO', 'EN_JUEGO')) AS sin_resultado""",
                (rs, i) -> new Torneos(rs.getInt("en_curso"), rs.getInt("inscripciones"), rs.getInt("solicitudes"), rs.getInt("sin_programar"),
                        rs.getInt("sin_resultado")), utc(ahora));
    }

    @Override
    public Clientes clientes(Instant haceUnaSemana, Instant inicioMes) {
        return jdbc.queryForObject("""
                SELECT count(*) FILTER (WHERE activo) AS activos,
                       count(*) FILTER (WHERE creado_en >= ?) AS semana,
                       count(*) FILTER (WHERE creado_en >= ?) AS mes
                FROM usuarios WHERE rol = 'CLIENTE'""",
                (rs, i) -> new Clientes(rs.getInt("activos"), rs.getInt("semana"), rs.getInt("mes")), utc(haceUnaSemana), utc(inicioMes));
    }
}
