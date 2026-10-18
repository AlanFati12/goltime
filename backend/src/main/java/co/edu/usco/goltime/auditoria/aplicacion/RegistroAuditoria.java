package co.edu.usco.goltime.auditoria.aplicacion;

import java.util.Map;

/**
 * Puerto para dejar constancia en la bitácora general ({@code audit_log}) de quién hizo qué.
 * El actor y la IP se toman del contexto de la petición; sin usuario autenticado el actor es SISTEMA.
 */
public interface RegistroAuditoria {

    void registrar(String accion, String entidad, Long entidadId, Map<String, Object> cambios, String motivo);
}
