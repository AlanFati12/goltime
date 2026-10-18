package co.edu.usco.goltime.estadisticas.infraestructura;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import co.edu.usco.goltime.estadisticas.aplicacion.Estadisticas.CeldaPico;
import co.edu.usco.goltime.estadisticas.aplicacion.Estadisticas.ClienteFrecuente;
import co.edu.usco.goltime.estadisticas.aplicacion.Estadisticas.Consultas;
import co.edu.usco.goltime.estadisticas.aplicacion.Estadisticas.FilaCancha;
import co.edu.usco.goltime.estadisticas.aplicacion.Estadisticas.Filtro;
import co.edu.usco.goltime.estadisticas.aplicacion.Estadisticas.PuntoSerie;
import co.edu.usco.goltime.estadisticas.aplicacion.Estadisticas.TotalesReservas;
import co.edu.usco.goltime.estadisticas.dominio.Agrupacion;

/**
 * Consultas agregadas en SQL. Las reservas del periodo son las que empiezan dentro de él (hora de Bogotá). Se leen directamente las
 * tablas de reservas, pagos y horarios: es un modelo de lectura que no modifica nada.
 */
@Repository
class ConsultasEstadisticasJdbc implements Consultas {

    private static final String BOGOTA = "'America/Bogota'";
    private static final String ESTADOS_OCUPAN = "('CONFIRMADA', 'COMPLETADA', 'NO_ASISTIO')";

    private final JdbcTemplate jdbc;

    ConsultasEstadisticasJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Condiciones de sede y cancha sobre los alias {@code c} (canchas) y {@code r} (reservas). */
    private static final String FILTRO_UBICACION = " AND (?::bigint IS NULL OR c.sede_id = ?) AND (?::bigint IS NULL OR r.cancha_id = ?)";

    private static OffsetDateTime utc(java.time.Instant i) {
        return OffsetDateTime.ofInstant(i, ZoneOffset.UTC);
    }

    /** Argumentos de "periodo + sede + cancha" en el orden que esperan las consultas sobre reservas. */
    private static List<Object> argumentos(Filtro f) {
        List<Object> args = new ArrayList<>();
        args.add(utc(f.periodo().inicio()));
        args.add(utc(f.periodo().fin()));
        args.add(f.sedeId());
        args.add(f.sedeId());
        args.add(f.canchaId());
        args.add(f.canchaId());
        return args;
    }

    private static final String BASE_RESERVAS = " FROM reservas r JOIN canchas c ON c.id = r.cancha_id WHERE r.inicio >= ? AND r.inicio < ?";

    @Override
    public TotalesReservas totales(Filtro f) {
        return jdbc.queryForObject("""
                SELECT count(*) AS total,
                       count(*) FILTER (WHERE r.estado = 'COMPLETADA') AS completadas,
                       count(*) FILTER (WHERE r.estado = 'CANCELADA') AS canceladas,
                       count(*) FILTER (WHERE r.estado = 'NO_ASISTIO') AS no_asistio,
                       coalesce(sum(r.precio_final) FILTER (WHERE r.estado = 'COMPLETADA'), 0) AS ingresos,
                       coalesce(sum(greatest(r.precio_final - r.monto_pagado, 0)) FILTER (WHERE r.estado = 'COMPLETADA'), 0) AS por_cobrar,
                       coalesce(sum(extract(epoch FROM (r.fin - r.inicio)) / 3600.0) FILTER (WHERE r.estado = 'COMPLETADA'), 0) AS horas,
                       count(DISTINCT coalesce('u' || r.cliente_id, 'm' || r.cliente_mostrador_id)) FILTER (WHERE r.estado = 'COMPLETADA') AS clientes"""
                + BASE_RESERVAS + " AND r.origen <> 'TORNEO'" + FILTRO_UBICACION, (rs, i) -> new TotalesReservas(rs.getInt("total"), rs.getInt("completadas"),
                        rs.getInt("canceladas"), rs.getInt("no_asistio"), rs.getLong("ingresos"), rs.getLong("por_cobrar"), rs.getDouble("horas"),
                        rs.getInt("clientes")), argumentos(f).toArray());
    }

    @Override
    public Map<String, Integer> reservasPorEstado(Filtro f) {
        Map<String, Integer> mapa = new LinkedHashMap<>();
        jdbc.query("SELECT r.estado, count(*) AS n" + BASE_RESERVAS + " AND r.origen <> 'TORNEO'" + FILTRO_UBICACION + " GROUP BY r.estado ORDER BY r.estado",
                rs -> {
                    mapa.put(rs.getString("estado"), rs.getInt("n"));
                }, argumentos(f).toArray());
        return mapa;
    }

    @Override
    public long recaudado(Filtro f) {
        Long total = jdbc.queryForObject("""
                SELECT coalesce(sum(CASE WHEN p.tipo = 'REEMBOLSO' THEN -p.monto ELSE p.monto END), 0)
                FROM pagos p JOIN reservas r ON r.id = p.reserva_id JOIN canchas c ON c.id = r.cancha_id
                WHERE p.fecha >= ? AND p.fecha < ?""" + FILTRO_UBICACION, Long.class, argumentos(f).toArray());
        return total == null ? 0 : total;
    }

    @Override
    public int partidosDeTorneo(Filtro f) {
        Integer n = jdbc.queryForObject("SELECT count(*)" + BASE_RESERVAS + " AND r.origen = 'TORNEO' AND r.estado IN ('CONFIRMADA', 'COMPLETADA')"
                + FILTRO_UBICACION, Integer.class, argumentos(f).toArray());
        return n == null ? 0 : n;
    }

    @Override
    public Map<Long, Double> minutosOcupados(Filtro f) {
        Map<Long, Double> mapa = new LinkedHashMap<>();
        OffsetDateTime inicio = utc(f.periodo().inicio());
        OffsetDateTime fin = utc(f.periodo().fin());
        jdbc.query("""
                SELECT o.cancha_id, sum(extract(epoch FROM upper(o.rango * tstzrange(?, ?, '[)')) - lower(o.rango * tstzrange(?, ?, '[)'))) / 60.0) AS minutos
                FROM ocupaciones_cancha o
                JOIN reservas r ON r.id = o.reserva_id
                JOIN canchas c ON c.id = o.cancha_id
                WHERE o.tipo = 'RESERVA' AND r.estado IN""" + ESTADOS_OCUPAN + """
                  AND c.estado <> 'INACTIVA' AND o.rango && tstzrange(?, ?, '[)')""" + " AND (?::bigint IS NULL OR c.sede_id = ?) AND (?::bigint IS NULL OR o.cancha_id = ?)"
                + " GROUP BY o.cancha_id", rs -> {
                    mapa.put(rs.getLong("cancha_id"), rs.getDouble("minutos"));
                }, inicio, fin, inicio, fin, inicio, fin, f.sedeId(), f.sedeId(), f.canchaId(), f.canchaId());
        return mapa;
    }

    @Override
    public Map<Long, Double> minutosDisponibles(Filtro f) {
        Map<Long, Double> mapa = new LinkedHashMap<>();
        jdbc.query("""
                SELECT c.id AS cancha_id,
                       sum(CASE WHEN coalesce(ch.cerrado, sh.cerrado, TRUE) THEN 0
                                ELSE coalesce(ch.minuto_cierre, sh.minuto_cierre) - coalesce(ch.minuto_apertura, sh.minuto_apertura) END) AS minutos
                FROM canchas c
                CROSS JOIN generate_series(?::date, ?::date, interval '1 day') AS d(dia)
                LEFT JOIN cancha_horarios ch ON ch.cancha_id = c.id AND ch.dia_semana = extract(isodow FROM d.dia)::int
                LEFT JOIN sede_horarios sh ON sh.sede_id = c.sede_id AND sh.dia_semana = extract(isodow FROM d.dia)::int
                WHERE c.estado <> 'INACTIVA' AND (?::bigint IS NULL OR c.sede_id = ?) AND (?::bigint IS NULL OR c.id = ?)
                GROUP BY c.id""", rs -> {
                    mapa.put(rs.getLong("cancha_id"), rs.getDouble("minutos"));
                }, Date.valueOf(f.periodo().desde()), Date.valueOf(f.periodo().hasta()), f.sedeId(), f.sedeId(), f.canchaId(), f.canchaId());
        return mapa;
    }

    @Override
    public List<PuntoSerie> serie(Filtro f, Agrupacion agrupacion) {
        List<Object> args = new ArrayList<>();
        args.add(agrupacion.truncado());
        args.addAll(argumentos(f));
        return jdbc.query("""
                SELECT date_trunc(?, r.inicio AT TIME ZONE""" + BOGOTA + """
                ) AS cubeta,
                       coalesce(sum(r.precio_final) FILTER (WHERE r.estado = 'COMPLETADA'), 0) AS ingresos,
                       count(*) FILTER (WHERE r.estado = 'COMPLETADA') AS completadas,
                       count(*) FILTER (WHERE r.estado = 'CANCELADA') AS canceladas""" + BASE_RESERVAS + " AND r.origen <> 'TORNEO'" + FILTRO_UBICACION
                + " GROUP BY 1 ORDER BY 1", (rs, i) -> {
                    Timestamp cubeta = rs.getTimestamp("cubeta");
                    return new PuntoSerie(cubeta.toLocalDateTime().toLocalDate(), rs.getLong("ingresos"), rs.getInt("completadas"), rs.getInt("canceladas"));
                }, args.toArray());
    }

    @Override
    public List<FilaCancha> porCancha(Filtro f) {
        List<Object> args = new ArrayList<>();
        args.add(utc(f.periodo().inicio()));
        args.add(utc(f.periodo().fin()));
        args.add(f.sedeId());
        args.add(f.sedeId());
        args.add(f.canchaId());
        args.add(f.canchaId());
        return jdbc.query("""
                SELECT c.id, c.nombre, s.nombre AS sede,
                       count(r.id) FILTER (WHERE r.estado = 'COMPLETADA') AS completadas,
                       coalesce(sum(extract(epoch FROM (r.fin - r.inicio)) / 3600.0) FILTER (WHERE r.estado = 'COMPLETADA'), 0) AS horas,
                       coalesce(sum(r.precio_final) FILTER (WHERE r.estado = 'COMPLETADA'), 0) AS ingresos
                FROM canchas c
                JOIN sedes s ON s.id = c.sede_id
                LEFT JOIN reservas r ON r.cancha_id = c.id AND r.inicio >= ? AND r.inicio < ? AND r.origen <> 'TORNEO'
                WHERE c.estado <> 'INACTIVA' AND (?::bigint IS NULL OR c.sede_id = ?) AND (?::bigint IS NULL OR c.id = ?)
                GROUP BY c.id, c.nombre, s.nombre, c.orden
                ORDER BY ingresos DESC, c.orden, c.nombre""", (rs, i) -> new FilaCancha(rs.getLong("id"), rs.getString("nombre"), rs.getString("sede"),
                        rs.getInt("completadas"), rs.getDouble("horas"), rs.getLong("ingresos"), 0), args.toArray());
    }

    @Override
    public List<CeldaPico> horasPico(Filtro f) {
        return jdbc.query("""
                SELECT extract(isodow FROM h.hora)::int AS dia, extract(hour FROM h.hora)::int AS hora, count(*) AS reservas
                FROM reservas r
                JOIN canchas c ON c.id = r.cancha_id
                CROSS JOIN LATERAL generate_series(date_trunc('hour', r.inicio AT TIME ZONE""" + BOGOTA + "), (r.fin AT TIME ZONE " + BOGOTA + """
                ) - interval '1 second', interval '1 hour') AS h(hora)
                WHERE r.inicio >= ? AND r.inicio < ? AND r.estado IN""" + ESTADOS_OCUPAN + " AND r.origen <> 'TORNEO'" + FILTRO_UBICACION
                + " GROUP BY 1, 2 ORDER BY 1, 2", (rs, i) -> new CeldaPico(rs.getInt("dia"), rs.getInt("hora"), rs.getInt("reservas")), argumentos(f).toArray());
    }

    @Override
    public List<ClienteFrecuente> clientesFrecuentes(Filtro f, int limite) {
        List<Object> args = argumentos(f);
        args.add(limite);
        return jdbc.query("""
                SELECT coalesce(u.nombres || ' ' || u.apellidos, m.nombre) AS nombre, coalesce(u.correo, m.telefono) AS contacto,
                       (r.cliente_id IS NULL) AS mostrador,
                       count(*) FILTER (WHERE r.estado = 'COMPLETADA') AS completadas,
                       count(*) FILTER (WHERE r.estado = 'CANCELADA') AS canceladas,
                       count(*) FILTER (WHERE r.estado = 'NO_ASISTIO') AS no_asistio,
                       coalesce(sum(r.precio_final) FILTER (WHERE r.estado = 'COMPLETADA'), 0) AS gasto,
                       max(r.inicio) AS ultima
                FROM reservas r
                JOIN canchas c ON c.id = r.cancha_id
                LEFT JOIN usuarios u ON u.id = r.cliente_id
                LEFT JOIN clientes_mostrador m ON m.id = r.cliente_mostrador_id
                WHERE r.inicio >= ? AND r.inicio < ? AND r.origen <> 'TORNEO'""" + FILTRO_UBICACION + """
                GROUP BY r.cliente_id, r.cliente_mostrador_id, u.nombres, u.apellidos, u.correo, m.nombre, m.telefono
                HAVING count(*) FILTER (WHERE r.estado = 'COMPLETADA') > 0
                ORDER BY completadas DESC, gasto DESC, nombre LIMIT ?""", (rs, i) -> new ClienteFrecuente(rs.getString("nombre"), rs.getString("contacto"),
                        rs.getBoolean("mostrador"), rs.getInt("completadas"), rs.getInt("canceladas"), rs.getInt("no_asistio"), rs.getLong("gasto"),
                        rs.getTimestamp("ultima").toInstant()), args.toArray());
    }
}
