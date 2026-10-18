package co.edu.usco.goltime.reservas.infraestructura;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import co.edu.usco.goltime.reservas.aplicacion.RepositorioSeries;

@Repository
class RepositorioSeriesJdbc implements RepositorioSeries {

    private static final String SELECT = """
            SELECT s.id, s.cancha_id, c.nombre AS cancha, coalesce(u.nombres || ' ' || u.apellidos, m.nombre) AS cliente, s.dia_semana, s.minuto_inicio,
                   s.duracion_min, s.semanas_pedidas, s.creada_en,
                   (SELECT count(*) FROM reservas r WHERE r.serie_id = s.id AND r.estado IN ('PENDIENTE', 'CONFIRMADA')) AS activas,
                   (SELECT count(*) FROM reservas r WHERE r.serie_id = s.id AND r.estado IN ('PENDIENTE', 'CONFIRMADA') AND r.inicio > ?) AS futuras
            FROM series_reservas s
            JOIN canchas c ON c.id = s.cancha_id
            LEFT JOIN usuarios u ON u.id = s.cliente_id
            LEFT JOIN clientes_mostrador m ON m.id = s.cliente_mostrador_id
            """;

    private final JdbcTemplate jdbc;

    RepositorioSeriesJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static OffsetDateTime utc(Instant i) {
        return OffsetDateTime.ofInstant(i, ZoneOffset.UTC);
    }

    @Override
    public long crear(long canchaId, int diaSemana, int minutoInicio, int duracionMin, int semanas, String nombreEquipo, Long creadaPor) {
        return jdbc.queryForObject("""
                INSERT INTO series_reservas (cancha_id, dia_semana, minuto_inicio, duracion_min, semanas_pedidas, nombre_equipo, creada_por)
                VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id""", Long.class, canchaId, diaSemana, minutoInicio, duracionMin, semanas, nombreEquipo, creadaPor);
    }

    @Override
    public void asignarCliente(long serieId, Long clienteId, Long mostradorId) {
        jdbc.update("UPDATE series_reservas SET cliente_id = ?, cliente_mostrador_id = ? WHERE id = ?", clienteId, mostradorId, serieId);
    }

    @Override
    public void asignarReserva(long reservaId, long serieId) {
        jdbc.update("UPDATE reservas SET serie_id = ?, origen = 'RECURRENTE' WHERE id = ?", serieId, reservaId);
    }

    @Override
    public Long mostradorDeReserva(long reservaId) {
        return jdbc.queryForObject("SELECT cliente_mostrador_id FROM reservas WHERE id = ?", Long.class, reservaId);
    }

    @Override
    public Optional<ResumenSerie> buscar(long id) {
        return jdbc.query(SELECT + " WHERE s.id = ?", (rs, i) -> resumen(rs), utc(Instant.now()), id).stream().findFirst();
    }

    @Override
    public List<ResumenSerie> listar(Instant ahora) {
        return jdbc.query(SELECT + " ORDER BY s.creada_en DESC, s.id DESC LIMIT 200", (rs, i) -> resumen(rs), utc(ahora));
    }

    @Override
    public List<Long> idsFuturasActivas(long serieId, Instant ahora) {
        return jdbc.queryForList("SELECT id FROM reservas WHERE serie_id = ? AND estado IN ('PENDIENTE', 'CONFIRMADA') AND inicio > ? ORDER BY inicio", Long.class, serieId,
                utc(ahora));
    }

    private static ResumenSerie resumen(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new ResumenSerie(rs.getLong("id"), rs.getLong("cancha_id"), rs.getString("cancha"), rs.getString("cliente"), rs.getInt("dia_semana"),
                rs.getInt("minuto_inicio"), rs.getInt("duracion_min"), rs.getInt("semanas_pedidas"), rs.getInt("activas"), rs.getInt("futuras"),
                rs.getObject("creada_en", OffsetDateTime.class).toInstant());
    }
}
