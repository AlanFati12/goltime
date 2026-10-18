package co.edu.usco.goltime.reservas.infraestructura;

import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.reservas.aplicacion.Ocupacion;
import co.edu.usco.goltime.reservas.aplicacion.RepositorioOcupaciones;

/**
 * Ocupaciones en PostgreSQL. La restricción {@code ex_ocupaciones_sin_solape} (EXCLUDE USING gist) es la
 * garantía final contra reservas solapadas: si dos peticiones concurrentes quieren el mismo horario, la
 * base de datos deja pasar una y rechaza a la otra con el estado SQL 23P01.
 */
@Repository
class RepositorioOcupacionesJdbc implements RepositorioOcupaciones {

    private static final String EXCLUSION_VIOLADA = "23P01";

    private final JdbcTemplate jdbc;

    RepositorioOcupacionesJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void ocupar(long reservaId, Collection<Long> canchaIds, Instant inicio, Instant fin) {
        try {
            for (long canchaId : canchaIds) {
                jdbc.update("INSERT INTO ocupaciones_cancha (cancha_id, rango, tipo, reserva_id) VALUES (?, tstzrange(?, ?, '[)'), 'RESERVA', ?)",
                        canchaId, utc(inicio), utc(fin), reservaId);
            }
        } catch (DataIntegrityViolationException ex) {
            if (NestedExceptionUtils.getMostSpecificCause(ex) instanceof SQLException sql && EXCLUSION_VIOLADA.equals(sql.getSQLState())) {
                throw new ExcepcionNegocio(CodigoError.RESERVA_SOLAPADA, "Ese horario ya no está disponible. Elige otro.");
            }
            throw ex;
        }
    }

    @Override
    public void liberar(long reservaId) {
        jdbc.update("DELETE FROM ocupaciones_cancha WHERE reserva_id = ?", reservaId);
    }

    @Override
    public List<Ocupacion> entre(Collection<Long> canchaIds, Instant desde, Instant hasta) {
        List<Object> parametros = new ArrayList<>(canchaIds);
        parametros.add(utc(desde));
        parametros.add(utc(hasta));
        return jdbc.query("SELECT cancha_id, lower(rango) AS inicio, upper(rango) AS fin, tipo FROM ocupaciones_cancha WHERE cancha_id IN ("
                + marcadores(canchaIds.size()) + ") AND rango && tstzrange(?, ?, '[)')",
                (rs, i) -> new Ocupacion(rs.getLong("cancha_id"), rs.getObject("inicio", OffsetDateTime.class).toInstant(),
                        rs.getObject("fin", OffsetDateTime.class).toInstant(), rs.getString("tipo")), parametros.toArray());
    }

    @Override
    public boolean hayOcupacion(Collection<Long> canchaIds, Instant inicio, Instant fin, Long ignorarReservaId) {
        List<Object> parametros = new ArrayList<>(canchaIds);
        parametros.add(utc(inicio));
        parametros.add(utc(fin));
        String sql = "SELECT count(*) FROM ocupaciones_cancha WHERE cancha_id IN (" + marcadores(canchaIds.size()) + ") AND rango && tstzrange(?, ?, '[)')";
        if (ignorarReservaId != null) {
            sql += " AND (reserva_id IS NULL OR reserva_id <> ?)";
            parametros.add(ignorarReservaId);
        }
        Long cantidad = jdbc.queryForObject(sql, Long.class, parametros.toArray());
        return cantidad != null && cantidad > 0;
    }

    @Override
    public List<Long> reservasFuturasActivasEnCanchas(Collection<Long> canchaIds, Instant ahora) {
        if (canchaIds.isEmpty()) {
            return List.of();
        }
        List<Object> parametros = new ArrayList<>(canchaIds);
        parametros.add(utc(ahora));
        return jdbc.queryForList("SELECT DISTINCT r.id FROM ocupaciones_cancha o JOIN reservas r ON r.id = o.reserva_id "
                + "WHERE o.cancha_id IN (" + marcadores(canchaIds.size()) + ") AND o.tipo = 'RESERVA' "
                + "AND r.estado IN ('PENDIENTE', 'CONFIRMADA') AND r.fin > ? ORDER BY r.id", Long.class, parametros.toArray());
    }

    @Override
    public List<Long> reservasFuturasActivasEnSede(long sedeId, Instant ahora) {
        return jdbc.queryForList("SELECT DISTINCT r.id FROM ocupaciones_cancha o JOIN reservas r ON r.id = o.reserva_id "
                + "WHERE o.cancha_id IN (SELECT id FROM canchas WHERE sede_id = ?) AND o.tipo = 'RESERVA' "
                + "AND r.estado IN ('PENDIENTE', 'CONFIRMADA') AND r.fin > ? ORDER BY r.id", Long.class, sedeId, utc(ahora));
    }

    private static OffsetDateTime utc(Instant instante) {
        return OffsetDateTime.ofInstant(instante, ZoneOffset.UTC);
    }

    private static String marcadores(int cantidad) {
        return String.join(", ", java.util.Collections.nCopies(cantidad, "?"));
    }
}
