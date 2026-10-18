package co.edu.usco.goltime.reservas.infraestructura;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import co.edu.usco.goltime.compartido.aplicacion.ContextoActual;
import co.edu.usco.goltime.reservas.aplicacion.RepositorioListaEspera.Espera;
import co.edu.usco.goltime.reservas.aplicacion.RepositorioListaEspera.Estado;
import co.edu.usco.goltime.reservas.aplicacion.ServicioListaEspera;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/v1/me/lista-espera")
@Tag(name = "Cliente: lista de espera", description = "Avisa cuando un horario ocupado se libera")
public class ControladorListaEspera {

    public record SolicitudEspera(@NotNull Long canchaId, @NotNull Instant inicio, @NotNull Instant fin) {
    }

    public record RespuestaEspera(long id, long canchaId, String cancha, String sede, Instant inicio, Instant fin, Estado estado, Instant creadoEn) {
    }

    private final ServicioListaEspera espera;
    private final ContextoActual contexto;

    public ControladorListaEspera(ServicioListaEspera espera, ContextoActual contexto) {
        this.espera = espera;
        this.contexto = contexto;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Esperar un horario ocupado", description = "Solo si el horario está ocupado. Máximo 5 esperas a la vez. Te avisamos (en la app y por correo) si se libera.")
    public RespuestaEspera unirse(@Valid @RequestBody SolicitudEspera s) {
        return aRespuesta(espera.unirse(contexto.actor().orElseThrow().id(), s.canchaId(), s.inicio(), s.fin()));
    }

    @GetMapping
    public List<RespuestaEspera> mias() {
        return espera.deCliente(contexto.actor().orElseThrow().id()).stream().map(ControladorListaEspera::aRespuesta).toList();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void salir(@PathVariable long id) {
        espera.salir(contexto.actor().orElseThrow().id(), id);
    }

    private static RespuestaEspera aRespuesta(Espera e) {
        return new RespuestaEspera(e.id(), e.canchaId(), e.canchaNombre(), e.sedeNombre(), e.inicio(), e.fin(), e.estado(), e.creadoEn());
    }
}
