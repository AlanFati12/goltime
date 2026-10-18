package co.edu.usco.goltime.auditoria.aplicacion;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import co.edu.usco.goltime.compartido.dominio.Pagina;
import co.edu.usco.goltime.compartido.dominio.Rol;
import co.edu.usco.goltime.compartido.dominio.SolicitudPagina;

/** Puerto de lectura de la bitácora general de auditoría (quién hizo qué sobre cualquier entidad). */
public interface ConsultaAuditoria {

    Pagina<RegistroAuditado> buscar(FiltroAuditoria filtro, SolicitudPagina pagina);

    /** Entidades y acciones distintas que existen en la bitácora, para llenar los filtros. */
    List<String> entidades();

    List<String> acciones();

    /** Filtros opcionales; los nulos no filtran. */
    record FiltroAuditoria(String entidad, Long entidadId, String accion, Long actorId, Instant desde, Instant hasta) {
    }

    record RegistroAuditado(Long id, Instant fecha, Long actorId, String actorNombre, Rol actorRol, String accion, String entidad,
            Long entidadId, Map<String, Object> cambios, String motivo, String ip) {
    }
}
