package co.edu.usco.goltime.torneos.infraestructura;

import java.util.List;
import java.util.Set;

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
import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.torneos.aplicacion.ConsultasTorneos.FiltroPartidos;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.EstadisticasTorneo;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.JugadorConDorsal;
import co.edu.usco.goltime.torneos.aplicacion.ServicioConsultaTorneos;
import co.edu.usco.goltime.torneos.aplicacion.ServicioEquipos;
import co.edu.usco.goltime.torneos.aplicacion.ServicioFixture;
import co.edu.usco.goltime.torneos.aplicacion.ServicioFixture.ResumenFixture;
import co.edu.usco.goltime.torneos.aplicacion.ServicioTorneos;
import co.edu.usco.goltime.torneos.aplicacion.ServicioTorneos.DatosTorneo;
import co.edu.usco.goltime.torneos.dominio.EstadoPartido;
import co.edu.usco.goltime.torneos.dominio.EstadoTorneo;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.RespuestaDetalleTorneo;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.RespuestaInscripcion;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.RespuestaJugador;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.RespuestaPartido;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.RespuestaResumenFixture;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.RespuestaTabla;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.RespuestaTorneo;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.SolicitudDorsal;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.SolicitudEstadoTorneo;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.SolicitudFixture;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.SolicitudInscripcion;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.SolicitudJugadorEnEquipo;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.SolicitudSemilla;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.SolicitudTorneo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/torneos")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: torneos", description = "Torneos, inscripción de equipos, plantillas y fixture")
public class ControladorTorneosAdmin {

    private final ServicioTorneos torneos;
    private final ServicioFixture fixture;
    private final ServicioConsultaTorneos consultas;
    private final ContextoActual contexto;

    public ControladorTorneosAdmin(ServicioTorneos torneos, ServicioFixture fixture, ServicioConsultaTorneos consultas, ContextoActual contexto) {
        this.torneos = torneos;
        this.fixture = fixture;
        this.consultas = consultas;
        this.contexto = contexto;
    }

    @GetMapping
    @Operation(summary = "Listar torneos (incluye borradores)")
    public List<RespuestaTorneo> listar(@RequestParam(required = false) EstadoTorneo estado) {
        return consultas.listar(estado, false).stream().map(MapeadorTorneos::resumen).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RespuestaDetalleTorneo crear(@Valid @RequestBody SolicitudTorneo s) {
        return detalle(torneos.crear(datos(s)).id());
    }

    @GetMapping("/{id}")
    public RespuestaDetalleTorneo detalle(@PathVariable long id) {
        return MapeadorTorneos.detalle(consultas.detalle(id, false));
    }

    @PutMapping("/{id}")
    public RespuestaDetalleTorneo actualizar(@PathVariable long id, @Valid @RequestBody SolicitudTorneo s) {
        torneos.actualizar(id, datos(s));
        return detalle(id);
    }

    @PostMapping("/{id}/estado")
    @Operation(summary = "Cambiar el estado del torneo",
            description = "BORRADOR → INSCRIPCIONES → EN_CURSO → FINALIZADO. CANCELADO exige motivo y libera las canchas de los partidos pendientes. "
                    + "Al finalizar se registran campeón, subcampeón y tercero.")
    public RespuestaDetalleTorneo estado(@PathVariable long id, @Valid @RequestBody SolicitudEstadoTorneo s) {
        torneos.cambiarEstado(actor(), id, s.estado(), s.motivo());
        return detalle(id);
    }

    // ----- Inscripciones -----

    @PostMapping("/{id}/inscripciones")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Inscribir un equipo (copia su plantilla habitual al torneo)")
    public RespuestaInscripcion inscribir(@PathVariable long id, @Valid @RequestBody SolicitudInscripcion s) {
        return MapeadorTorneos.inscripcion(torneos.inscribirEquipo(actor(), id, s.equipoId(), s.semilla()));
    }

    @PutMapping("/{id}/inscripciones/{inscripcionId}")
    @Operation(summary = "Definir la cabeza de serie (semilla) de un equipo")
    public RespuestaInscripcion semilla(@PathVariable long id, @PathVariable long inscripcionId, @Valid @RequestBody SolicitudSemilla s) {
        return MapeadorTorneos.inscripcion(torneos.actualizarSemilla(id, inscripcionId, s.semilla()));
    }

    @DeleteMapping("/{id}/inscripciones/{inscripcionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Retirar un equipo (solo antes de generar el fixture)")
    public void retirar(@PathVariable long id, @PathVariable long inscripcionId) {
        torneos.retirarEquipo(id, inscripcionId);
    }

    // ----- Plantillas del torneo -----

    @GetMapping("/{id}/inscripciones/{inscripcionId}/jugadores")
    public List<RespuestaJugador> plantilla(@PathVariable long id, @PathVariable long inscripcionId) {
        return aRespuesta(torneos.plantilla(id, inscripcionId));
    }

    @PostMapping("/{id}/inscripciones/{inscripcionId}/jugadores")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Agregar un jugador a la plantilla del equipo en este torneo",
            description = "Con `jugadorId` suma un jugador existente; sin él crea uno nuevo (queda también en la plantilla habitual del equipo). Un jugador no puede estar en dos equipos del mismo torneo.")
    public List<RespuestaJugador> agregarJugador(@PathVariable long id, @PathVariable long inscripcionId, @Valid @RequestBody SolicitudJugadorEnEquipo s) {
        if (s.jugadorId() != null) {
            return aRespuesta(torneos.agregarJugador(id, inscripcionId, s.jugadorId(), s.dorsal()));
        }
        if (s.nombres() == null || s.nombres().isBlank() || s.apellidos() == null || s.apellidos().isBlank()) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Indica los nombres y apellidos del jugador, o el jugador existente.");
        }
        return aRespuesta(torneos.agregarJugadorNuevo(id, inscripcionId, new ServicioEquipos.DatosJugador(s.nombres(), s.apellidos(), s.documento(),
                s.posicion(), s.dorsal())));
    }

    @PutMapping("/{id}/inscripciones/{inscripcionId}/jugadores/{jugadorId}/dorsal")
    public List<RespuestaJugador> dorsal(@PathVariable long id, @PathVariable long inscripcionId, @PathVariable long jugadorId,
            @Valid @RequestBody SolicitudDorsal s) {
        return aRespuesta(torneos.cambiarDorsal(id, inscripcionId, jugadorId, s.dorsal()));
    }

    @DeleteMapping("/{id}/inscripciones/{inscripcionId}/jugadores/{jugadorId}")
    public List<RespuestaJugador> quitarJugador(@PathVariable long id, @PathVariable long inscripcionId, @PathVariable long jugadorId) {
        return aRespuesta(torneos.quitarJugador(id, inscripcionId, jugadorId));
    }

    // ----- Fixture y lectura -----

    @PostMapping("/{id}/fixture")
    @Operation(summary = "Generar (o regenerar) el fixture según el formato",
            description = "Se puede regenerar mientras no se haya jugado ningún partido; libera las canchas de los partidos descartados. "
                    + "Con modo MANUAL, `ordenManual` lista las inscripciones en el orden de siembra.")
    public RespuestaResumenFixture generarFixture(@PathVariable long id, @RequestBody(required = false) SolicitudFixture s) {
        ResumenFixture r = fixture.generar(actor(), id, s == null ? null : s.ordenManual());
        return new RespuestaResumenFixture(r.fases(), r.grupos(), r.partidos(), r.descansos());
    }

    @DeleteMapping("/{id}/fixture")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Descartar el fixture (solo si no se jugó ningún partido)")
    public void descartarFixture(@PathVariable long id) {
        fixture.descartar(actor(), id);
    }

    @GetMapping("/{id}/partidos")
    public List<RespuestaPartido> partidos(@PathVariable long id, @RequestParam(required = false) Long faseId, @RequestParam(required = false) Long inscripcionId,
            @RequestParam(required = false) EstadoPartido estado) {
        return consultas.partidos(id, new FiltroPartidos(faseId, inscripcionId, estado), false).stream().map(MapeadorTorneos::partido).toList();
    }

    @GetMapping("/{id}/posiciones")
    public List<RespuestaTabla> posiciones(@PathVariable long id) {
        return MapeadorTorneos.tablas(consultas.posiciones(id, false), consultas.equipos(id, false));
    }

    @GetMapping("/{id}/estadisticas")
    public EstadisticasTorneo estadisticas(@PathVariable long id) {
        return consultas.estadisticas(id, false);
    }

    // ----- Interno -----

    private Actor actor() {
        return contexto.actor().orElseThrow();
    }

    private static List<RespuestaJugador> aRespuesta(List<JugadorConDorsal> plantilla) {
        return plantilla.stream().map(MapeadorTorneos::jugador).toList();
    }

    private static DatosTorneo datos(SolicitudTorneo s) {
        return new DatosTorneo(s.nombre(), s.descripcion(), s.sedeId(), s.jugadoresPorEquipo(), s.formato(), s.fechaInicio(), s.fechaFin(), s.cupoEquipos(),
                s.valorInscripcion() == null ? 0 : s.valorInscripcion(), s.premiosTexto(), s.reglamento(), s.modoEmparejamiento(),
                Boolean.TRUE.equals(s.idaYVuelta()), Boolean.TRUE.equals(s.tercerPuesto()), s.numGrupos(), s.clasificadosPorGrupo(),
                s.puntosVictoria() == null ? 3 : s.puntosVictoria(), s.puntosEmpate() == null ? 1 : s.puntosEmpate(),
                s.puntosDerrota() == null ? 0 : s.puntosDerrota(), s.criteriosDesempate(), s.marcadorWoGoles() == null ? 3 : s.marcadorWoGoles(),
                s.canchaIds() == null ? Set.of() : s.canchaIds());
    }
}
