package co.edu.usco.goltime.auditoria.infraestructura;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.usco.goltime.auditoria.aplicacion.ConsultaAuditoria.FiltroAuditoria;
import co.edu.usco.goltime.auditoria.aplicacion.ConsultaAuditoria.RegistroAuditado;
import co.edu.usco.goltime.auditoria.aplicacion.ServicioConsultaAuditoria;
import co.edu.usco.goltime.compartido.dominio.Pagina;
import co.edu.usco.goltime.compartido.dominio.SolicitudPagina;
import co.edu.usco.goltime.compartido.dominio.ZonaHoraria;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/admin/auditoria")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: auditoría", description = "Bitácora de quién hizo qué")
public class ControladorAuditoria {

    private final ServicioConsultaAuditoria auditoria;

    public ControladorAuditoria(ServicioConsultaAuditoria auditoria) {
        this.auditoria = auditoria;
    }

    @GetMapping
    @Operation(summary = "Bitácora de auditoría, de la más reciente a la más antigua",
            description = "Filtros por entidad, id de la entidad, acción, actor y rango de fechas (hora de Bogotá).")
    public Pagina<RegistroAuditado> listar(@RequestParam(required = false) String entidad, @RequestParam(required = false) Long entidadId,
            @RequestParam(required = false) String accion, @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "25") int tamano) {
        return auditoria.buscar(new FiltroAuditoria(entidad, entidadId, accion, actorId,
                desde == null ? null : ZonaHoraria.inicioDelDia(desde), hasta == null ? null : ZonaHoraria.inicioDelDia(hasta.plusDays(1))),
                new SolicitudPagina(pagina, tamano));
    }

    @GetMapping("/filtros")
    @Operation(summary = "Entidades y acciones que existen, para llenar los filtros")
    public RespuestaFiltros filtros() {
        return new RespuestaFiltros(auditoria.entidades(), auditoria.acciones());
    }

    public record RespuestaFiltros(List<String> entidades, List<String> acciones) {
    }
}
