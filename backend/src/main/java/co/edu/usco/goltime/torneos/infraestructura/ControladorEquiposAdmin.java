package co.edu.usco.goltime.torneos.infraestructura;

import java.util.List;

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

import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.torneos.aplicacion.ServicioEquipos;
import co.edu.usco.goltime.torneos.aplicacion.ServicioEquipos.DatosEquipo;
import co.edu.usco.goltime.torneos.aplicacion.ServicioEquipos.DatosJugador;
import co.edu.usco.goltime.torneos.aplicacion.ServicioEquipos.DetalleEquipo;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.RespuestaDetalleEquipo;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.RespuestaJugador;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.RespuestaResumenEquipo;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.SolicitudActivo;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.SolicitudDorsal;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.SolicitudEquipo;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.SolicitudJugador;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.SolicitudJugadorEnEquipo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: equipos y jugadores", description = "Equipos, jugadores y plantillas habituales, reutilizables entre torneos")
public class ControladorEquiposAdmin {

    private final ServicioEquipos equipos;

    public ControladorEquiposAdmin(ServicioEquipos equipos) {
        this.equipos = equipos;
    }

    @GetMapping("/equipos")
    @Operation(summary = "Listar equipos")
    public List<RespuestaResumenEquipo> listar(@RequestParam(required = false) String busqueda, @RequestParam(defaultValue = "false") boolean incluirInactivos) {
        return equipos.listar(busqueda, incluirInactivos).stream()
                .map(r -> new RespuestaResumenEquipo(MapeadorTorneos.equipo(r.equipo()), r.jugadores(), r.torneos())).toList();
    }

    @PostMapping("/equipos")
    @ResponseStatus(HttpStatus.CREATED)
    public RespuestaDetalleEquipo crear(@Valid @RequestBody SolicitudEquipo s) {
        return aRespuesta(equipos.crear(datos(s)));
    }

    @GetMapping("/equipos/{id}")
    public RespuestaDetalleEquipo detalle(@PathVariable long id) {
        return aRespuesta(equipos.detalle(id));
    }

    @PutMapping("/equipos/{id}")
    public RespuestaDetalleEquipo actualizar(@PathVariable long id, @Valid @RequestBody SolicitudEquipo s) {
        return aRespuesta(equipos.actualizar(id, datos(s)));
    }

    @PostMapping("/equipos/{id}/estado")
    @Operation(summary = "Activar o desactivar un equipo (no se borra: conserva su historia)")
    public RespuestaDetalleEquipo estado(@PathVariable long id, @Valid @RequestBody SolicitudActivo s) {
        return aRespuesta(equipos.cambiarActivo(id, s.activo()));
    }

    @PostMapping("/equipos/{id}/jugadores")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Agregar un jugador a la plantilla del equipo",
            description = "Con `jugadorId` agrega un jugador que ya existe (de otro equipo o torneo); sin él crea uno nuevo con nombres y apellidos.")
    public RespuestaDetalleEquipo agregarJugador(@PathVariable long id, @Valid @RequestBody SolicitudJugadorEnEquipo s) {
        if (s.jugadorId() != null) {
            return aRespuesta(equipos.agregarJugadorExistente(id, s.jugadorId(), s.dorsal()));
        }
        if (s.nombres() == null || s.nombres().isBlank() || s.apellidos() == null || s.apellidos().isBlank()) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Indica los nombres y apellidos del jugador, o el jugador existente.");
        }
        return aRespuesta(equipos.agregarJugadorNuevo(id, new DatosJugador(s.nombres(), s.apellidos(), s.documento(), s.posicion(), s.dorsal())));
    }

    @PutMapping("/equipos/{id}/jugadores/{jugadorId}/dorsal")
    public RespuestaDetalleEquipo dorsal(@PathVariable long id, @PathVariable long jugadorId, @Valid @RequestBody SolicitudDorsal s) {
        return aRespuesta(equipos.cambiarDorsal(id, jugadorId, s.dorsal()));
    }

    @DeleteMapping("/equipos/{id}/jugadores/{jugadorId}")
    @Operation(summary = "Sacar al jugador de la plantilla habitual (no afecta a los torneos donde ya está inscrito)")
    public RespuestaDetalleEquipo quitarJugador(@PathVariable long id, @PathVariable long jugadorId) {
        return aRespuesta(equipos.quitarJugador(id, jugadorId));
    }

    @GetMapping("/jugadores")
    @Operation(summary = "Buscar jugadores por nombre o documento (para reutilizarlos en otro equipo)")
    public List<RespuestaJugador> buscarJugadores(@RequestParam String texto) {
        return equipos.buscarJugadores(texto).stream().map(j -> new RespuestaJugador(j.id(), j.nombres(), j.apellidos(), j.nombreCompleto(), j.documento(),
                j.posicion(), null)).toList();
    }

    @PutMapping("/jugadores/{id}")
    @Operation(summary = "Corregir los datos de un jugador")
    public RespuestaJugador actualizarJugador(@PathVariable long id, @Valid @RequestBody SolicitudJugador s) {
        var j = equipos.actualizarJugador(id, new DatosJugador(s.nombres(), s.apellidos(), s.documento(), s.posicion(), null));
        return new RespuestaJugador(j.id(), j.nombres(), j.apellidos(), j.nombreCompleto(), j.documento(), j.posicion(), null);
    }

    private static DatosEquipo datos(SolicitudEquipo s) {
        return new DatosEquipo(s.nombre(), s.color(), s.delegadoNombre(), s.delegadoTelefono(), s.usuarioResponsableId());
    }

    private static RespuestaDetalleEquipo aRespuesta(DetalleEquipo d) {
        return new RespuestaDetalleEquipo(MapeadorTorneos.equipo(d.equipo()), d.plantilla().stream().map(MapeadorTorneos::jugador).toList());
    }
}
