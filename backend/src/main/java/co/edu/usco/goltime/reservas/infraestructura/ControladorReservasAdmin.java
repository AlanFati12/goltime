package co.edu.usco.goltime.reservas.infraestructura;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.usco.goltime.compartido.aplicacion.ContextoActual;
import co.edu.usco.goltime.compartido.dominio.Actor;
import co.edu.usco.goltime.compartido.dominio.Pagina;
import co.edu.usco.goltime.compartido.dominio.SolicitudPagina;
import co.edu.usco.goltime.reservas.aplicacion.RepositorioReservas.FiltroReservasAdmin;
import co.edu.usco.goltime.reservas.aplicacion.ServicioReservas;
import co.edu.usco.goltime.reservas.dominio.EstadoReserva;
import co.edu.usco.goltime.reservas.infraestructura.DtosReservas.RespuestaReserva;
import co.edu.usco.goltime.reservas.infraestructura.DtosReservas.SolicitudMotivoObligatorio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/reservas")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: reservas", description = "Aprobación de solicitudes y gestión de reservas")
public class ControladorReservasAdmin {

    private final ServicioReservas reservas;
    private final ContextoActual contexto;

    public ControladorReservasAdmin(ServicioReservas reservas, ContextoActual contexto) {
        this.reservas = reservas;
        this.contexto = contexto;
    }

    @GetMapping
    @Operation(summary = "Listar reservas",
            description = "Las pendientes van primero, las que vencen antes arriba. Filtros: estado, sede y búsqueda por código, nombre o correo del cliente.")
    public Pagina<RespuestaReserva> listar(@RequestParam(required = false) EstadoReserva estado, @RequestParam(required = false) Long sedeId,
            @RequestParam(required = false) String busqueda, @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return reservas.listarAdmin(new FiltroReservasAdmin(estado, sedeId, busqueda), new SolicitudPagina(pagina, tamano))
                .mapear(MapeadorReservas::aRespuesta);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle con historial")
    public RespuestaReserva detalle(@PathVariable long id) {
        return MapeadorReservas.aRespuesta(reservas.detalle(id, admin()));
    }

    @PostMapping("/{id}/aprobar")
    @Operation(summary = "Aprobar una solicitud pendiente: pasa a CONFIRMADA y se notifica al cliente")
    public RespuestaReserva aprobar(@PathVariable long id) {
        return MapeadorReservas.aRespuesta(reservas.aprobar(id, admin().id()));
    }

    @PostMapping("/{id}/rechazar")
    @Operation(summary = "Rechazar una solicitud pendiente (motivo obligatorio): libera el horario y se notifica al cliente")
    public RespuestaReserva rechazar(@PathVariable long id, @Valid @RequestBody SolicitudMotivoObligatorio s) {
        return MapeadorReservas.aRespuesta(reservas.rechazar(id, admin().id(), s.motivo()));
    }

    @PostMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar una reserva (motivo obligatorio): se notifica al cliente")
    public RespuestaReserva cancelar(@PathVariable long id, @Valid @RequestBody SolicitudMotivoObligatorio s) {
        return MapeadorReservas.aRespuesta(reservas.cancelarPorAdmin(id, admin(), s.motivo()));
    }

    private Actor admin() {
        return contexto.actor().orElseThrow();
    }
}
