package co.edu.usco.goltime.reservas.infraestructura;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.usco.goltime.compartido.aplicacion.ContextoActual;
import co.edu.usco.goltime.compartido.dominio.Actor;
import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.compartido.dominio.Pagina;
import co.edu.usco.goltime.compartido.dominio.SolicitudPagina;
import co.edu.usco.goltime.reservas.aplicacion.RepositorioReservas.PestanaReservas;
import co.edu.usco.goltime.reservas.aplicacion.ServicioReservas;
import co.edu.usco.goltime.reservas.aplicacion.Vistas.ResultadoSolicitud;
import co.edu.usco.goltime.reservas.infraestructura.DtosReservas.RespuestaPrevisualizacion;
import co.edu.usco.goltime.reservas.infraestructura.DtosReservas.RespuestaReserva;
import co.edu.usco.goltime.reservas.infraestructura.DtosReservas.SolicitudMotivo;
import co.edu.usco.goltime.reservas.infraestructura.DtosReservas.SolicitudReprogramacion;
import co.edu.usco.goltime.reservas.infraestructura.DtosReservas.SolicitudReserva;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Reservas del cliente autenticado. El cliente sale siempre del token (nunca de la petición) y las
 * reservas ajenas responden 404 como si no existieran.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Reservas", description = "Solicitar, consultar, reprogramar y cancelar reservas")
public class ControladorReservas {

    private final ServicioReservas reservas;
    private final ContextoActual contexto;

    public ControladorReservas(ServicioReservas reservas, ContextoActual contexto) {
        this.reservas = reservas;
        this.contexto = contexto;
    }

    @PostMapping("/reservas")
    @Operation(summary = "Solicitar una reserva",
            description = "Queda PENDIENTE hasta que el administrador la apruebe (o expire) y retiene el horario. "
                    + "Con la cabecera Idempotency-Key, repetir la petición devuelve la misma reserva (200) en vez de crear otra.")
    public ResponseEntity<RespuestaReserva> solicitar(@Valid @RequestBody SolicitudReserva s,
            @RequestHeader(value = "Idempotency-Key", required = false) String claveIdempotencia) {
        Actor actor = actor();
        ResultadoSolicitud resultado = reservas.solicitar(actor.id(), s.canchaId(), s.inicio(), s.fin(), s.nombreEquipo(), s.notas(), claveIdempotencia);
        return ResponseEntity.status(resultado.creada() ? HttpStatus.CREATED : HttpStatus.OK).body(MapeadorReservas.aRespuesta(resultado.vista()));
    }

    @GetMapping("/me/reservas")
    @Operation(summary = "Mis reservas por pestaña", description = "PROXIMAS (pendientes y confirmadas por jugar), HISTORIAL (jugadas) o CANCELADAS (canceladas, rechazadas y expiradas).")
    public Pagina<RespuestaReserva> mias(@RequestParam(defaultValue = "PROXIMAS") PestanaReservas pestana,
            @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamano) {
        return reservas.deCliente(actor().id(), pestana, new SolicitudPagina(pagina, tamano)).mapear(MapeadorReservas::aRespuesta);
    }

    @GetMapping("/reservas/{id}")
    @Operation(summary = "Detalle de una de mis reservas, con su historial de cambios")
    public RespuestaReserva detalle(@PathVariable long id) {
        return MapeadorReservas.aRespuesta(reservas.detalle(id, actor()));
    }

    @PostMapping("/reservas/{id}/cancelar")
    @Operation(summary = "Cancelar mi reserva",
            description = "Una pendiente se cancela sin penalidad. Una confirmada con menos de las horas mínimas de antelación queda como cancelación tardía.")
    public RespuestaReserva cancelar(@PathVariable long id, @Valid @RequestBody(required = false) SolicitudMotivo s) {
        return MapeadorReservas.aRespuesta(reservas.cancelar(id, actor(), s == null ? null : s.motivo()));
    }

    @PostMapping("/reservas/{id}/reprogramar/previsualizar")
    @Operation(summary = "Previsualiza reprogramar: precio nuevo, diferencia y si el horario está libre")
    public RespuestaPrevisualizacion previsualizarReprogramacion(@PathVariable long id, @Valid @RequestBody SolicitudReprogramacion s) {
        return MapeadorReservas.aRespuesta(reservas.previsualizarReprogramacion(id, actor(), s.canchaId(), s.inicio(), s.fin()));
    }

    @PatchMapping("/reservas/{id}/reprogramar")
    @Operation(summary = "Reprogramar mi reserva a otra fecha, hora o cancha",
            description = "Respeta el máximo de reprogramaciones y la antelación mínima. El precio se recalcula.")
    public RespuestaReserva reprogramar(@PathVariable long id, @Valid @RequestBody SolicitudReprogramacion s) {
        return MapeadorReservas.aRespuesta(reservas.reprogramar(id, actor(), s.canchaId(), s.inicio(), s.fin()));
    }

    private Actor actor() {
        return contexto.actor().orElseThrow(() -> new ExcepcionNegocio(CodigoError.NO_AUTENTICADO, "Debes iniciar sesión."));
    }
}
