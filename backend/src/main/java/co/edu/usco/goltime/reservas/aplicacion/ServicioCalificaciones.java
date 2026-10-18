package co.edu.usco.goltime.reservas.aplicacion;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.reservas.dominio.EstadoReserva;
import co.edu.usco.goltime.reservas.dominio.Reserva;

/**
 * Calificaciones de canchas: el cliente califica de 1 a 5 estrellas una reserva suya ya COMPLETADA (una sola por reserva, que puede
 * corregir). Públicamente solo cuentan las de reservas que siguen completadas y se muestra el nombre de pila con la inicial del apellido.
 */
@Service
public class ServicioCalificaciones {

    public record Calificacion(long id, long reservaId, int estrellas, String comentario, Instant creadaEn, Instant actualizadaEn) {
    }

    public record EstadoCalificacion(boolean puedeCalificar, String motivo, Calificacion calificacion) {
    }

    public record Opinion(int estrellas, String comentario, String autor, Instant fecha) {
    }

    public record ResumenCalificaciones(double promedio, int total, int[] distribucion, List<Opinion> ultimas) {
    }

    private final JdbcTemplate jdbc;
    private final RepositorioReservas reservas;

    public ServicioCalificaciones(JdbcTemplate jdbc, RepositorioReservas reservas) {
        this.jdbc = jdbc;
        this.reservas = reservas;
    }

    @Transactional(readOnly = true)
    public EstadoCalificacion estadoDe(long clienteId, long reservaId) {
        Reserva r = propia(clienteId, reservaId);
        Optional<Calificacion> existente = buscar(reservaId);
        if (r.estado() != EstadoReserva.COMPLETADA) {
            return new EstadoCalificacion(false, "Podrás calificarla cuando la reserva se complete.", existente.orElse(null));
        }
        return new EstadoCalificacion(true, null, existente.orElse(null));
    }

    /** Crea o corrige la calificación de la reserva. */
    @Transactional
    public Calificacion calificar(long clienteId, long reservaId, int estrellas, String comentario) {
        Reserva r = propia(clienteId, reservaId);
        if (r.estado() != EstadoReserva.COMPLETADA) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "Solo puedes calificar reservas ya jugadas.");
        }
        if (estrellas < 1 || estrellas > 5) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "La calificación va de 1 a 5 estrellas.");
        }
        String texto = comentario == null || comentario.isBlank() ? null : comentario.trim();
        if (texto != null && texto.length() > 500) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "El comentario no puede superar 500 caracteres.");
        }
        jdbc.update("""
                INSERT INTO calificaciones (reserva_id, cliente_id, cancha_id, estrellas, comentario) VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (reserva_id) DO UPDATE SET estrellas = EXCLUDED.estrellas, comentario = EXCLUDED.comentario, actualizada_en = now()""", reservaId, clienteId,
                r.canchaId(), estrellas, texto);
        return buscar(reservaId).orElseThrow();
    }

    @Transactional(readOnly = true)
    public ResumenCalificaciones deCancha(long canchaId) {
        int[] distribucion = new int[5];
        jdbc.query("""
                SELECT c.estrellas, count(*) AS n FROM calificaciones c JOIN reservas r ON r.id = c.reserva_id
                WHERE c.cancha_id = ? AND r.estado = 'COMPLETADA' GROUP BY c.estrellas""", rs -> {
                    distribucion[rs.getInt("estrellas") - 1] = rs.getInt("n");
                }, canchaId);
        int total = 0;
        int suma = 0;
        for (int i = 0; i < 5; i++) {
            total += distribucion[i];
            suma += distribucion[i] * (i + 1);
        }
        List<Opinion> ultimas = jdbc.query("""
                SELECT c.estrellas, c.comentario, c.creada_en, split_part(u.nombres, ' ', 1) || ' ' || left(u.apellidos, 1) || '.' AS autor
                FROM calificaciones c JOIN reservas r ON r.id = c.reserva_id JOIN usuarios u ON u.id = c.cliente_id
                WHERE c.cancha_id = ? AND r.estado = 'COMPLETADA' AND c.comentario IS NOT NULL ORDER BY c.creada_en DESC LIMIT 10""",
                (rs, i) -> new Opinion(rs.getInt("estrellas"), rs.getString("comentario"), rs.getString("autor"), rs.getObject("creada_en", OffsetDateTime.class).toInstant()), canchaId);
        return new ResumenCalificaciones(total == 0 ? 0 : Math.round(10.0 * suma / total) / 10.0, total, distribucion, ultimas);
    }

    private Reserva propia(long clienteId, long reservaId) {
        return reservas.buscarPorId(reservaId).filter(r -> r.clienteId() != null && r.clienteId() == clienteId)
                .orElseThrow(() -> new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "La reserva no existe."));
    }

    private Optional<Calificacion> buscar(long reservaId) {
        return jdbc.query("SELECT * FROM calificaciones WHERE reserva_id = ?", (rs, i) -> new Calificacion(rs.getLong("id"), rs.getLong("reserva_id"), rs.getInt("estrellas"),
                rs.getString("comentario"), rs.getObject("creada_en", OffsetDateTime.class).toInstant(), rs.getObject("actualizada_en", OffsetDateTime.class).toInstant()),
                reservaId).stream().findFirst();
    }
}
