package co.edu.usco.goltime.reservas.infraestructura;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import co.edu.usco.goltime.compartido.aplicacion.ContextoActual;
import co.edu.usco.goltime.compartido.dominio.Actor;
import co.edu.usco.goltime.reservas.aplicacion.RepositorioSeries.ResumenSerie;
import co.edu.usco.goltime.reservas.aplicacion.ServicioSeries;
import co.edu.usco.goltime.reservas.aplicacion.ServicioSeries.DatosSerie;
import co.edu.usco.goltime.reservas.aplicacion.ServicioSeries.EstadoOcurrencia;
import co.edu.usco.goltime.reservas.aplicacion.ServicioSeries.Ocurrencia;
import co.edu.usco.goltime.reservas.infraestructura.DtosReservas.RespuestaReserva;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/admin/series")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: reservas recurrentes", description = "La misma reserva todas las semanas, con revisión previa de cada fecha")
public class ControladorSeries {

    public record SolicitudSerie(@NotNull Long canchaId, @NotNull Instant inicio, @NotNull Instant fin, @NotNull @Min(ServicioSeries.SEMANAS_MIN) @Max(ServicioSeries.SEMANAS_MAX) Integer semanas,
            Long clienteId, @Size(max = 150) String mostradorNombre, @Size(max = 30) String mostradorTelefono, @Size(max = 100) String nombreEquipo,
            @Size(max = 500) String notas, Boolean omitirOcupadas) {
    }

    public record SolicitudCancelarSerie(@NotBlank @Size(max = 500) String motivo) {
    }

    public record RespuestaOcurrencia(int numero, Instant inicio, Instant fin, EstadoOcurrencia estado, String motivo, int precio) {
    }

    public record RespuestaSerie(long id, long canchaId, String cancha, String cliente, int diaSemana, int minutoInicio, int duracionMin, int semanasPedidas,
            int reservasActivas, int reservasFuturas, Instant creadaEn) {
    }

    public record RespuestaCreacion(long serieId, List<RespuestaReserva> reservas, List<RespuestaOcurrencia> omitidas) {
    }

    public record RespuestaCancelacion(int canceladas) {
    }

    private final ServicioSeries series;
    private final ContextoActual contexto;

    public ControladorSeries(ServicioSeries series, ContextoActual contexto) {
        this.series = series;
        this.contexto = contexto;
    }

    @PostMapping("/previsualizar")
    @Operation(summary = "Qué fechas de la serie se pueden reservar (horario, ocupación y precio de cada una)")
    public List<RespuestaOcurrencia> previsualizar(@Valid @RequestBody SolicitudSerie s) {
        return series.previsualizar(datos(s)).stream().map(ControladorSeries::aRespuesta).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear la serie de reservas confirmadas",
            description = "Si alguna fecha no está disponible responde 409 CONFLICTOS_SIN_CONFIRMAR; con `omitirOcupadas` crea solo las disponibles.")
    public RespuestaCreacion crear(@Valid @RequestBody SolicitudSerie s) {
        var r = series.crear(actor(), datos(s), Boolean.TRUE.equals(s.omitirOcupadas()));
        return new RespuestaCreacion(r.serieId(), r.reservas().stream().map(MapeadorReservas::aRespuesta).toList(), r.omitidas().stream().map(ControladorSeries::aRespuesta).toList());
    }

    @GetMapping
    public List<RespuestaSerie> listar() {
        return series.listar().stream().map(ControladorSeries::aRespuesta).toList();
    }

    @PostMapping("/{id}/cancelar")
    @Operation(summary = "Cancela las reservas futuras activas de la serie, con motivo")
    public RespuestaCancelacion cancelar(@PathVariable long id, @Valid @RequestBody SolicitudCancelarSerie s) {
        return new RespuestaCancelacion(series.cancelar(actor(), id, s.motivo()));
    }

    private Actor actor() {
        return contexto.actor().orElseThrow();
    }

    private static DatosSerie datos(SolicitudSerie s) {
        return new DatosSerie(s.canchaId(), s.inicio(), s.fin(), s.semanas(), s.clienteId(), s.mostradorNombre(), s.mostradorTelefono(), s.nombreEquipo(), s.notas());
    }

    private static RespuestaOcurrencia aRespuesta(Ocurrencia o) {
        return new RespuestaOcurrencia(o.numero(), o.inicio(), o.fin(), o.estado(), o.motivo(), o.precio());
    }

    private static RespuestaSerie aRespuesta(ResumenSerie r) {
        return new RespuestaSerie(r.id(), r.canchaId(), r.canchaNombre(), r.cliente(), r.diaSemana(), r.minutoInicio(), r.duracionMin(), r.semanasPedidas(), r.reservasActivas(),
                r.reservasFuturas(), r.creadaEn());
    }
}
