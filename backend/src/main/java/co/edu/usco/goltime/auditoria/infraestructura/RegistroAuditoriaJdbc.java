package co.edu.usco.goltime.auditoria.infraestructura;

import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import co.edu.usco.goltime.auditoria.aplicacion.RegistroAuditoria;
import co.edu.usco.goltime.compartido.aplicacion.ContextoActual;
import co.edu.usco.goltime.compartido.dominio.Actor;

/**
 * Escribe en {@code audit_log} dentro de la transacción del caso de uso: si la operación de
 * negocio se revierte, la entrada de auditoría también.
 */
@Component
class RegistroAuditoriaJdbc implements RegistroAuditoria {

    private final JdbcTemplate jdbc;
    private final ContextoActual contexto;
    private final ObjectMapper mapeador;

    RegistroAuditoriaJdbc(JdbcTemplate jdbc, ContextoActual contexto, ObjectMapper mapeador) {
        this.jdbc = jdbc;
        this.contexto = contexto;
        this.mapeador = mapeador;
    }

    @Override
    public void registrar(String accion, String entidad, Long entidadId, Map<String, Object> cambios, String motivo) {
        Actor actor = contexto.actor().orElse(Actor.sistema());
        jdbc.update("""
                INSERT INTO audit_log (actor_id, actor_rol, accion, entidad, entidad_id, cambios, motivo, ip)
                VALUES (?, ?, ?, ?, ?, ?::jsonb, ?, ?)
                """,
                actor.id(), actor.rol().name(), accion, entidad, entidadId, aJson(cambios), motivo,
                contexto.ip().orElse(null));
    }

    private String aJson(Map<String, Object> cambios) {
        try {
            return cambios == null ? null : mapeador.writeValueAsString(cambios);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo serializar el detalle de auditoría", ex);
        }
    }
}
