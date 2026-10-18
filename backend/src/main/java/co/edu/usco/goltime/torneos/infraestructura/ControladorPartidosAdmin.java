package co.edu.usco.goltime.torneos.infraestructura;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import co.edu.usco.goltime.compartido.aplicacion.ContextoActual;
import co.edu.usco.goltime.compartido.dominio.Actor;
import co.edu.usco.goltime.reservas.aplicacion.ServicioReservasTorneo.ConflictosHorario;
import co.edu.usco.goltime.torneos.aplicacion.ServicioConsultaTorneos;
import co.edu.usco.goltime.torneos.aplicacion.ServicioPartidos;
import co.edu.usco.goltime.torneos.aplicacion.ServicioPartidos.DatosEvento;
import co.edu.usco.goltime.torneos.aplicacion.ServicioPartidos.DatosProgramacion;
import co.edu.usco.goltime.torneos.aplicacion.ServicioPartidos.DatosResultado;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.RespuestaConflictoHorario;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.RespuestaDetallePartido;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.RespuestaPrevisualizacionPartido;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.SolicitudEvento;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.SolicitudProgramacion;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.SolicitudResultado;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.SolicitudWO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/partidos")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: partidos", description = "Programación con reserva de cancha, resultados, W.O. y eventos de partido")
public class ControladorPartidosAdmin {

    private final ServicioPartidos partidos;
    private final ServicioConsultaTorneos consultas;
    private final ContextoActual contexto;

    public ControladorPartidosAdmin(ServicioPartidos partidos, ServicioConsultaTorneos consultas, ContextoActual contexto) {
        this.partidos = partidos;
        this.consultas = consultas;
        this.contexto = contexto;
    }

    @GetMapping("/{id}")
    public RespuestaDetallePartido detalle(@PathVariable long id) {
        return MapeadorTorneos.detallePartido(consultas.partido(id, false));
    }

    @PostMapping("/{id}/programacion/previsualizar")
    @Operation(summary = "Previsualiza la programación: reservas de clientes que se cancelarían y su huella",
            description = "Si hay conflictos, el front muestra la lista y, al confirmar, envía la `huella` en `huellaConfirmada` (mismo flujo que los bloqueos). "
                    + "Chocar con otro partido o con un bloqueo es un error 409, no se cancela.")
    public RespuestaPrevisualizacionPartido previsualizar(@PathVariable long id, @Valid @RequestBody SolicitudProgramacion s) {
        ConflictosHorario c = partidos.previsualizarProgramacion(id, datos(s));
        return new RespuestaPrevisualizacionPartido(c.conflictos().stream().map(x -> new RespuestaConflictoHorario(x.reservaId(), x.codigo(), x.cliente(),
                x.canchaNombre(), x.inicio(), x.fin(), x.precioFinal(), x.conPago(), x.estadoPago(), x.estado())).toList(), c.huella(), c.requiereConfirmacion());
    }

    @PutMapping("/{id}/programacion")
    @Operation(summary = "Programar (o reprogramar) el partido: fecha, hora, cancha y árbitro",
            description = "Crea una reserva interna (origen TORNEO) que ocupa la cancha. Con reservas de clientes en conflicto exige `huellaConfirmada` (409 CONFLICTOS_SIN_CONFIRMAR).")
    public RespuestaDetallePartido programar(@PathVariable long id, @Valid @RequestBody SolicitudProgramacion s) {
        partidos.programar(actor(), id, datos(s), s.huellaConfirmada());
        return detalle(id);
    }

    @DeleteMapping("/{id}/programacion")
    @Operation(summary = "Quitar la programación (libera la cancha); con aplazar=true queda APLAZADO")
    public RespuestaDetallePartido desprogramar(@PathVariable long id, @RequestParam(defaultValue = "false") boolean aplazar) {
        partidos.desprogramar(actor(), id, aplazar);
        return detalle(id);
    }

    @PostMapping("/{id}/iniciar")
    public RespuestaDetallePartido iniciar(@PathVariable long id) {
        partidos.iniciar(id);
        return detalle(id);
    }

    @PutMapping("/{id}/resultado")
    @Operation(summary = "Registrar o corregir el resultado",
            description = "En eliminatoria un empate exige penales. El ganador avanza por el cuadro; al terminar la fase de grupos los clasificados ocupan las llaves.")
    public RespuestaDetallePartido resultado(@PathVariable long id, @Valid @RequestBody SolicitudResultado s) {
        partidos.registrarResultado(actor(), id, new DatosResultado(s.golesLocal(), s.golesVisitante(), s.penalesLocal(), s.penalesVisitante(), s.figuraJugadorId()));
        return detalle(id);
    }

    @PostMapping("/{id}/wo")
    @Operation(summary = "Dar el partido por W.O. (por defecto 3-0 para quien se presentó)")
    public RespuestaDetallePartido wo(@PathVariable long id, @Valid @RequestBody SolicitudWO s) {
        partidos.registrarWO(actor(), id, s.ganador(), s.motivo());
        return detalle(id);
    }

    @PostMapping("/{id}/reabrir")
    @Operation(summary = "Deshacer el resultado para volver a registrarlo")
    public RespuestaDetallePartido reabrir(@PathVariable long id) {
        partidos.reabrir(actor(), id);
        return detalle(id);
    }

    @PostMapping("/{id}/eventos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar un evento (gol, asistencia, tarjeta...)",
            description = "Los goles registrados por lado no pueden superar el marcador. La asistencia se liga a un gol del mismo equipo.")
    public RespuestaDetallePartido agregarEvento(@PathVariable long id, @Valid @RequestBody SolicitudEvento s) {
        partidos.agregarEvento(actor(), id, new DatosEvento(s.inscripcionId(), s.jugadorId(), s.tipo(), s.minuto(), s.eventoRelacionadoId(), s.observacion()));
        return detalle(id);
    }

    @DeleteMapping("/{id}/eventos/{eventoId}")
    public RespuestaDetallePartido eliminarEvento(@PathVariable long id, @PathVariable long eventoId) {
        partidos.eliminarEvento(actor(), id, eventoId);
        return detalle(id);
    }

    private Actor actor() {
        return contexto.actor().orElseThrow();
    }

    private static DatosProgramacion datos(SolicitudProgramacion s) {
        return new DatosProgramacion(s.canchaId(), s.inicio(), s.duracionMin(), s.arbitro());
    }
}
