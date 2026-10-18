package co.edu.usco.goltime.reservas.infraestructura;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.usco.goltime.compartido.aplicacion.ContextoActual;
import co.edu.usco.goltime.reservas.aplicacion.ServicioCalificaciones;
import co.edu.usco.goltime.reservas.aplicacion.ServicioCalificaciones.Calificacion;
import co.edu.usco.goltime.reservas.aplicacion.ServicioCalificaciones.EstadoCalificacion;
import co.edu.usco.goltime.reservas.aplicacion.ServicioCalificaciones.ResumenCalificaciones;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Calificaciones", description = "Calificación de canchas por reservas jugadas")
public class ControladorCalificaciones {

    public record SolicitudCalificacion(@NotNull @Min(1) @Max(5) Integer estrellas, @Size(max = 500) String comentario) {
    }

    private final ServicioCalificaciones calificaciones;
    private final ContextoActual contexto;

    public ControladorCalificaciones(ServicioCalificaciones calificaciones, ContextoActual contexto) {
        this.calificaciones = calificaciones;
        this.contexto = contexto;
    }

    @GetMapping("/reservas/{id}/calificacion")
    @Operation(summary = "¿Puedo calificar esta reserva? y mi calificación actual")
    public EstadoCalificacion estado(@PathVariable long id) {
        return calificaciones.estadoDe(contexto.actor().orElseThrow().id(), id);
    }

    @PutMapping("/reservas/{id}/calificacion")
    @Operation(summary = "Calificar (o corregir la calificación de) una reserva completada")
    public Calificacion calificar(@PathVariable long id, @Valid @RequestBody SolicitudCalificacion s) {
        return calificaciones.calificar(contexto.actor().orElseThrow().id(), id, s.estrellas(), s.comentario());
    }

    @GetMapping("/publico/canchas/{id}/calificaciones")
    @Operation(summary = "Promedio, distribución y últimas opiniones de una cancha")
    public ResumenCalificaciones deCancha(@PathVariable long id) {
        return calificaciones.deCancha(id);
    }
}
