package co.edu.usco.goltime.reservas.infraestructura;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import co.edu.usco.goltime.compartido.dominio.Rol;
import co.edu.usco.goltime.reservas.aplicacion.EntradaHistorial;
import co.edu.usco.goltime.reservas.aplicacion.RepositorioHistorial;

@Repository
class RepositorioHistorialJdbc implements RepositorioHistorial {

    private static final TypeReference<Map<String, Object>> MAPA = new TypeReference<>() { };

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapeador;

    RepositorioHistorialJdbc(JdbcTemplate jdbc, ObjectMapper mapeador) {
        this.jdbc = jdbc;
        this.mapeador = mapeador;
    }

    @Override
    public void registrar(EntradaHistorial e) {
        jdbc.update("INSERT INTO reserva_historial (reserva_id, accion, actor_id, actor_rol, motivo, datos_anteriores, datos_nuevos, fecha) "
                + "VALUES (?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?)", e.reservaId(), e.accion(), e.actorId(), e.actorRol().name(),
                e.motivo(), aJson(e.datosAnteriores()), aJson(e.datosNuevos()), Timestamp.from(e.fecha()));
    }

    @Override
    public List<EntradaHistorial> deReserva(long reservaId) {
        return jdbc.query("""
                SELECT h.*, u.nombres || ' ' || u.apellidos AS actor_nombre
                FROM reserva_historial h LEFT JOIN usuarios u ON u.id = h.actor_id
                WHERE h.reserva_id = ? ORDER BY h.fecha, h.id""", (rs, i) -> new EntradaHistorial(rs.getLong("id"), reservaId,
                rs.getString("accion"), (Long) rs.getObject("actor_id"), rs.getString("actor_nombre"), Rol.valueOf(rs.getString("actor_rol")),
                rs.getString("motivo"), deJson(rs.getString("datos_anteriores")), deJson(rs.getString("datos_nuevos")),
                rs.getTimestamp("fecha").toInstant()), reservaId);
    }

    private String aJson(Map<String, Object> datos) {
        try {
            return datos == null ? null : mapeador.writeValueAsString(datos);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo serializar el historial", ex);
        }
    }

    private Map<String, Object> deJson(String json) {
        try {
            return json == null ? null : mapeador.readValue(json, MAPA);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo leer el historial", ex);
        }
    }
}
