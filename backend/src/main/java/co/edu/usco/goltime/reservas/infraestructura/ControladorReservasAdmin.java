package co.edu.usco.goltime.reservas.infraestructura;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import co.edu.usco.goltime.compartido.aplicacion.ContextoActual;
import co.edu.usco.goltime.compartido.dominio.Actor;
import co.edu.usco.goltime.compartido.dominio.Pagina;
import co.edu.usco.goltime.compartido.dominio.SolicitudPagina;
import co.edu.usco.goltime.compartido.dominio.ZonaHoraria;
import co.edu.usco.goltime.reservas.aplicacion.RepositorioReservas.FiltroReservasAdmin;
import co.edu.usco.goltime.reservas.aplicacion.ServicioReservas;
import co.edu.usco.goltime.reservas.aplicacion.ServicioReservasAdmin;
import co.edu.usco.goltime.reservas.aplicacion.ServicioReservasAdmin.DatosReservaAdmin;
import co.edu.usco.goltime.reservas.dominio.EstadoPago;
import co.edu.usco.goltime.reservas.dominio.EstadoReserva;
import co.edu.usco.goltime.reservas.dominio.OrigenReserva;
import co.edu.usco.goltime.reservas.infraestructura.DtosReservas.RespuestaMostrador;
import co.edu.usco.goltime.reservas.infraestructura.DtosReservas.RespuestaPago;
import co.edu.usco.goltime.reservas.infraestructura.DtosReservas.RespuestaPrevisualizacion;
import co.edu.usco.goltime.reservas.infraestructura.DtosReservas.RespuestaReserva;
import co.edu.usco.goltime.reservas.infraestructura.DtosReservas.SolicitudEstadoReserva;
import co.edu.usco.goltime.reservas.infraestructura.DtosReservas.SolicitudMotivoObligatorio;
import co.edu.usco.goltime.reservas.infraestructura.DtosReservas.SolicitudPago;
import co.edu.usco.goltime.reservas.infraestructura.DtosReservas.SolicitudReembolso;
import co.edu.usco.goltime.reservas.infraestructura.DtosReservas.SolicitudReprogramacionAdmin;
import co.edu.usco.goltime.reservas.infraestructura.DtosReservas.SolicitudReservaAdmin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: reservas", description = "Aprobación, creación a nombre de clientes, pagos y gestión de reservas")
public class ControladorReservasAdmin {

    private final ServicioReservas reservas;
    private final ServicioReservasAdmin admin;
    private final ContextoActual contexto;

    public ControladorReservasAdmin(ServicioReservas reservas, ServicioReservasAdmin admin, ContextoActual contexto) {
        this.reservas = reservas;
        this.admin = admin;
        this.contexto = contexto;
    }

    // ----- Listado y detalle -----

    @GetMapping("/reservas")
    @Operation(summary = "Listar reservas",
            description = "Las pendientes van primero, las que vencen antes arriba. Filtros: estado, sede, cancha, estado de pago, origen, rango de fechas "
                    + "(del inicio de la reserva, hora de Bogotá) y búsqueda por código, nombre o correo del cliente.")
    public Pagina<RespuestaReserva> listar(@RequestParam(required = false) EstadoReserva estado, @RequestParam(required = false) Long sedeId,
            @RequestParam(required = false) Long canchaId, @RequestParam(required = false) EstadoPago estadoPago,
            @RequestParam(required = false) OrigenReserva origen,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String busqueda, @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return reservas.listarAdmin(filtro(estado, sedeId, canchaId, estadoPago, origen, desde, hasta, busqueda), new SolicitudPagina(pagina, tamano))
                .mapear(MapeadorReservas::aRespuesta);
    }

    @GetMapping(value = "/reservas/exportar", produces = "text/csv")
    @Operation(summary = "Exportar a CSV el listado filtrado (hasta 5.000 reservas)")
    public ResponseEntity<byte[]> exportar(@RequestParam(required = false) EstadoReserva estado, @RequestParam(required = false) Long sedeId,
            @RequestParam(required = false) Long canchaId, @RequestParam(required = false) EstadoPago estadoPago,
            @RequestParam(required = false) OrigenReserva origen,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String busqueda) {
        byte[] csv = ExportadorCsv.aCsv(admin.paraExportar(filtro(estado, sedeId, canchaId, estadoPago, origen, desde, hasta, busqueda)));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("reservas-goltime.csv").build().toString())
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(csv);
    }

    @GetMapping("/reservas/{id}")
    @Operation(summary = "Detalle con historial")
    public RespuestaReserva detalle(@PathVariable long id) {
        return MapeadorReservas.aRespuesta(reservas.detalle(id, actor()));
    }

    // ----- Crear -----

    @PostMapping("/reservas")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear una reserva a nombre de un cliente registrado o de mostrador",
            description = "Nace CONFIRMADA (sin aprobación). Sin antelación mínima ni límite de reservas por cliente; sí respeta horario, bloques y anti-solapamiento.")
    public RespuestaReserva crear(@Valid @RequestBody SolicitudReservaAdmin s) {
        var m = s.mostrador();
        return MapeadorReservas.aRespuesta(admin.crear(actor(), new DatosReservaAdmin(s.canchaId(), s.inicio(), s.fin(), s.clienteId(),
                m == null ? null : m.nombre(), m == null ? null : m.telefono(), s.nombreEquipo(), s.notas())));
    }

    @GetMapping("/clientes-mostrador")
    @Operation(summary = "Buscar clientes de mostrador por nombre o teléfono")
    public List<RespuestaMostrador> mostrador(@RequestParam(required = false) String busqueda) {
        return admin.buscarMostrador(busqueda).stream().map(c -> new RespuestaMostrador(c.id(), c.nombre(), c.telefono())).toList();
    }

    // ----- Aprobación y cancelación -----

    @PostMapping("/reservas/{id}/aprobar")
    @Operation(summary = "Aprobar una solicitud pendiente: pasa a CONFIRMADA y se notifica al cliente")
    public RespuestaReserva aprobar(@PathVariable long id) {
        return MapeadorReservas.aRespuesta(reservas.aprobar(id, actor().id()));
    }

    @PostMapping("/reservas/{id}/rechazar")
    @Operation(summary = "Rechazar una solicitud pendiente (motivo obligatorio): libera el horario y se notifica al cliente")
    public RespuestaReserva rechazar(@PathVariable long id, @Valid @RequestBody SolicitudMotivoObligatorio s) {
        return MapeadorReservas.aRespuesta(reservas.rechazar(id, actor().id(), s.motivo()));
    }

    @PostMapping("/reservas/{id}/cancelar")
    @Operation(summary = "Cancelar una reserva (motivo obligatorio): se notifica al cliente y, si había pagos, queda un reembolso pendiente")
    public RespuestaReserva cancelar(@PathVariable long id, @Valid @RequestBody SolicitudMotivoObligatorio s) {
        return MapeadorReservas.aRespuesta(reservas.cancelarPorAdmin(id, actor(), s.motivo()));
    }

    // ----- Reprogramar y estado -----

    @PostMapping("/reservas/{id}/reprogramar/previsualizar")
    @Operation(summary = "Previsualiza reprogramar (sin límites de cliente): precio nuevo, diferencia y si el horario está libre")
    public RespuestaPrevisualizacion previsualizarReprogramacion(@PathVariable long id, @Valid @RequestBody SolicitudReprogramacionAdmin s) {
        return MapeadorReservas.aRespuesta(reservas.previsualizarReprogramacion(id, actor(), s.canchaId(), s.inicio(), s.fin()));
    }

    @PatchMapping("/reservas/{id}/reprogramar")
    @Operation(summary = "Reprogramar una reserva a otra fecha, hora o cancha (motivo obligatorio): se notifica al cliente")
    public RespuestaReserva reprogramar(@PathVariable long id, @Valid @RequestBody SolicitudReprogramacionAdmin s) {
        return MapeadorReservas.aRespuesta(reservas.reprogramar(id, actor(), s.canchaId(), s.inicio(), s.fin(), s.motivo()));
    }

    @PatchMapping("/reservas/{id}/estado")
    @Operation(summary = "Marcar como COMPLETADA (ya terminó) o NO_ASISTIO (ya empezó; también corrige una completada por error)")
    public RespuestaReserva estado(@PathVariable long id, @Valid @RequestBody SolicitudEstadoReserva s) {
        return MapeadorReservas.aRespuesta(admin.cambiarEstado(id, actor(), s.estado()));
    }

    // ----- Pagos -----

    @GetMapping("/reservas/{id}/pagos")
    @Operation(summary = "Pagos, abonos y reembolsos registrados")
    public List<RespuestaPago> pagos(@PathVariable long id) {
        return admin.pagosDe(id).stream().map(p -> new RespuestaPago(p.pago().id(), p.pago().monto(), p.pago().metodo(), p.pago().referencia(),
                p.pago().tipo(), p.registradoPorNombre(), p.pago().fecha())).toList();
    }

    @PostMapping("/reservas/{id}/pagos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar un pago o abono",
            description = "Solo reservas confirmadas o ya jugadas, sin pasarse del valor. Queda ABONADO o PAGADO según el saldo.")
    public RespuestaReserva pagar(@PathVariable long id, @Valid @RequestBody SolicitudPago s) {
        return MapeadorReservas.aRespuesta(admin.registrarPago(id, actor(), s.monto(), s.metodo(), s.referencia()));
    }

    @PostMapping("/reservas/{id}/reembolsos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar un reembolso",
            description = "Solo con reembolso pendiente (reserva cancelada, rechazada o expirada con dinero pagado). `cerrar` lo da por saldado aunque se retenga una parte.")
    public RespuestaReserva reembolsar(@PathVariable long id, @Valid @RequestBody SolicitudReembolso s) {
        return MapeadorReservas.aRespuesta(admin.registrarReembolso(id, actor(), s.monto(), s.metodo(), s.referencia(), s.cerrar()));
    }

    // ----- Apoyo -----

    private Actor actor() {
        return contexto.actor().orElseThrow();
    }

    private static FiltroReservasAdmin filtro(EstadoReserva estado, Long sedeId, Long canchaId, EstadoPago estadoPago, OrigenReserva origen,
            LocalDate desde, LocalDate hasta, String busqueda) {
        Instant inicio = desde == null ? null : ZonaHoraria.inicioDelDia(desde);
        Instant fin = hasta == null ? null : ZonaHoraria.inicioDelDia(hasta.plusDays(1));
        return new FiltroReservasAdmin(estado, sedeId, canchaId, estadoPago, origen, inicio, fin, busqueda);
    }
}
