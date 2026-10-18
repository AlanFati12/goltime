package co.edu.usco.goltime.torneos.infraestructura;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import co.edu.usco.goltime.compartido.aplicacion.ContextoActual;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.EstadoInscripcion;
import co.edu.usco.goltime.torneos.aplicacion.ServicioSolicitudes;
import co.edu.usco.goltime.torneos.aplicacion.ServicioSolicitudes.DatosSolicitud;
import co.edu.usco.goltime.torneos.aplicacion.ServicioSolicitudes.JugadorSolicitado;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.RespuestaEquipo;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.RespuestaInscripcion;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.RespuestaJugador;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/** Solicitud de inscripción de equipos por clientes habilitados y su resolución por el administrador. */
final class ControladoresSolicitudes {

    private ControladoresSolicitudes() {
    }

    record SolicitudJugadorTorneo(@NotBlank @Size(max = 100) String nombres, @NotBlank @Size(max = 100) String apellidos, @Size(max = 30) String documento,
            @Size(max = 30) String posicion, @Min(0) @Max(99) Integer dorsal) {
    }

    record SolicitudInscripcionCliente(@NotBlank @Size(max = 100) String nombreEquipo, @Size(max = 20) String color, @Size(max = 30) String delegadoTelefono,
            @NotEmpty @Valid List<SolicitudJugadorTorneo> jugadores) {
    }

    record SolicitudRechazo(@NotBlank @Size(max = 500) String motivo) {
    }

    record RespuestaSolicitud(long inscripcionId, long torneoId, String torneo, String equipo, EstadoInscripcion estado, Instant creadaEn) {
    }

    record RespuestaMisSolicitudes(boolean habilitado, List<RespuestaSolicitud> solicitudes) {
    }

    record RespuestaResponsable(long id, String nombre, String correo, String telefono, boolean activo) {
    }

    record RespuestaDetalleSolicitud(long inscripcionId, EstadoInscripcion estado, RespuestaEquipo equipo, RespuestaResponsable responsable,
            List<RespuestaJugador> jugadores) {
    }

    @RestController
    @RequestMapping("/api/v1")
    @Tag(name = "Cliente: inscripción a torneos", description = "Solo clientes habilitados por el administrador")
    static class Cliente {

        private final ServicioSolicitudes solicitudes;
        private final ContextoActual contexto;

        Cliente(ServicioSolicitudes solicitudes, ContextoActual contexto) {
            this.solicitudes = solicitudes;
            this.contexto = contexto;
        }

        @GetMapping("/me/solicitudes-torneo")
        @Operation(summary = "Mis solicitudes de inscripción y si mi cuenta está habilitada para pedirlas")
        public RespuestaMisSolicitudes mias() {
            var r = solicitudes.mias(contexto.actor().orElseThrow().id());
            return new RespuestaMisSolicitudes(r.habilitado(), r.solicitudes().stream()
                    .map(s -> new RespuestaSolicitud(s.inscripcionId(), s.torneoId(), s.torneoNombre(), s.equipoNombre(), s.estado(), s.creadaEn())).toList());
        }

        @PostMapping("/torneos/{id}/solicitudes")
        @ResponseStatus(HttpStatus.CREATED)
        @Operation(summary = "Solicitar la inscripción de un equipo nuevo (con su plantilla)",
                description = "Requiere que el administrador haya habilitado la cuenta. Crea el equipo con el cliente como responsable; queda pendiente de aprobación.")
        public RespuestaSolicitud solicitar(@PathVariable long id, @Valid @RequestBody SolicitudInscripcionCliente s) {
            var i = solicitudes.solicitar(contexto.actor().orElseThrow().id(), id, new DatosSolicitud(s.nombreEquipo(), s.color(), s.delegadoTelefono(),
                    s.jugadores().stream().map(j -> new JugadorSolicitado(j.nombres(), j.apellidos(), j.documento(), j.posicion(), j.dorsal())).toList()));
            return mias(i.id());
        }

        @DeleteMapping("/me/solicitudes-torneo/{inscripcionId}")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        @Operation(summary = "Retirar una solicitud pendiente")
        public void retirar(@PathVariable long inscripcionId) {
            solicitudes.retirar(contexto.actor().orElseThrow().id(), inscripcionId);
        }

        private RespuestaSolicitud mias(long inscripcionId) {
            return mias().solicitudes().stream().filter(x -> x.inscripcionId() == inscripcionId).findFirst().orElseThrow();
        }
    }

    @RestController
    @RequestMapping("/api/v1/admin/torneos/{id}/solicitudes")
    @PreAuthorize("hasRole('ADMIN')")
    @Tag(name = "Admin: solicitudes de inscripción")
    static class Admin {

        private final ServicioSolicitudes solicitudes;

        Admin(ServicioSolicitudes solicitudes) {
            this.solicitudes = solicitudes;
        }

        @GetMapping("/{inscripcionId}")
        @Operation(summary = "Ver la solicitud antes de resolverla: equipo, responsable y plantilla")
        public RespuestaDetalleSolicitud detalle(@PathVariable long id, @PathVariable long inscripcionId) {
            var d = solicitudes.detalle(id, inscripcionId);
            var r = d.responsable();
            return new RespuestaDetalleSolicitud(d.inscripcion().id(), d.inscripcion().estado(), MapeadorTorneos.equipo(d.equipo()),
                    r == null ? null : new RespuestaResponsable(r.id(), r.nombre(), r.correo(), r.telefono(), r.activo()),
                    d.jugadores().stream().map(MapeadorTorneos::jugador).toList());
        }

        @PostMapping("/{inscripcionId}/aprobar")
        public RespuestaInscripcion aprobar(@PathVariable long id, @PathVariable long inscripcionId) {
            var i = solicitudes.aprobar(id, inscripcionId);
            return new RespuestaInscripcion(i.id(), i.equipoId(), null, null, i.semilla(), 0);
        }

        @PostMapping("/{inscripcionId}/rechazar")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        public void rechazar(@PathVariable long id, @PathVariable long inscripcionId, @Valid @RequestBody SolicitudRechazo s) {
            solicitudes.rechazar(id, inscripcionId, s.motivo());
        }
    }
}
