package co.edu.usco.goltime.sedes.infraestructura;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import co.edu.usco.goltime.sedes.dominio.EstadoCancha;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Contratos JSON de sedes, canchas, tarifas, horarios y festivos. Las horas viajan como "HH:mm" (hasta "24:00"). */
public final class DtosSedes {

    private DtosSedes() {
    }

    // ----- Solicitudes -----

    public record SolicitudSede(
            @NotBlank @Size(max = 120) String nombre,
            @NotBlank @Size(max = 200) String direccion,
            @NotBlank @Size(max = 80) String ciudad,
            @Size(max = 30) String telefono,
            @Size(max = 30) String whatsapp,
            @Size(max = 2000) String descripcion,
            @Size(max = 20) List<@Size(max = 60) String> servicios,
            @DecimalMin("-90") @DecimalMax("90") BigDecimal latitud,
            @DecimalMin("-180") @DecimalMax("180") BigDecimal longitud,
            @Valid List<SolicitudHorarioDia> horario) {
    }

    public record SolicitudHorarioDia(@Min(1) @Max(7) int diaSemana, boolean cerrado, String apertura, String cierre) {
    }

    public record SolicitudHorario(@NotNull @Valid List<SolicitudHorarioDia> dias) {
    }

    public record SolicitudCancha(
            @NotBlank @Size(max = 80) String nombre,
            @Min(2) @Max(11) int jugadoresPorEquipo,
            @Min(4) @Max(40) Integer capacidadTotal,
            @Size(max = 60) String superficie,
            @DecimalMin("1") @DecimalMax("200") BigDecimal largoM,
            @DecimalMin("1") @DecimalMax("200") BigDecimal anchoM,
            Boolean techada,
            Boolean iluminacion,
            @NotNull @Min(0) @Max(100_000_000) Integer precioBaseHora,
            @Min(15) @Max(240) Integer duracionBloqueMin,
            @Min(15) @Max(1440) Integer duracionMinMin,
            @Min(15) @Max(1440) Integer duracionMaxMin,
            @Size(max = 2000) String descripcion,
            Integer orden,
            Set<Long> componentes) {
    }

    public record SolicitudTarifa(
            Set<@Min(1) @Max(7) Integer> diasSemana,
            @NotBlank String horaInicio,
            @NotBlank String horaFin,
            boolean aplicaFestivo,
            @NotNull @Min(0) @Max(100_000_000) Integer precioHora,
            LocalDate vigenteDesde,
            LocalDate vigenteHasta) {
    }

    public record SolicitudEstadoSede(boolean activa, @Size(max = 500) String motivo, boolean confirmarConflictos) {
    }

    public record SolicitudEstadoCancha(@NotNull EstadoCancha estado, @Size(max = 500) String motivo, boolean confirmarConflictos) {
    }

    public record SolicitudPrevisualizarSede(boolean activa) {
    }

    public record SolicitudPrevisualizarCancha(@NotNull EstadoCancha estado) {
    }

    public record SolicitudFestivo(@NotNull LocalDate fecha, @NotBlank @Size(max = 100) String nombre) {
    }

    // ----- Respuestas -----

    public record RespuestaFoto(Long id, String url, int orden) {
    }

    public record RespuestaHorarioDia(int diaSemana, boolean cerrado, String apertura, String cierre) {
    }

    public record RespuestaComponente(Long id, String nombre, String modalidad) {
    }

    public record RespuestaTarifa(Long id, Long canchaId, List<Integer> diasSemana, String horaInicio, String horaFin,
            boolean aplicaFestivo, int precioHora, LocalDate vigenteDesde, LocalDate vigenteHasta) {
    }

    public record RespuestaCancha(
            Long id, Long sedeId, String sedeNombre, String nombre, String modalidad, int jugadoresPorEquipo,
            int capacidadTotal, String superficie, BigDecimal largoM, BigDecimal anchoM, boolean techada,
            boolean iluminacion, int precioBaseHora, int duracionBloqueMin, int duracionMinMin, int duracionMaxMin,
            EstadoCancha estado, String descripcion, int orden, boolean esCompuesta,
            List<RespuestaComponente> componentes, List<RespuestaFoto> fotos, List<RespuestaTarifa> tarifas,
            List<RespuestaHorarioDia> horarioEfectivo, List<RespuestaHorarioDia> horarioPropio) {
    }

    public record RespuestaSede(
            Long id, String nombre, String direccion, String ciudad, String telefono, String whatsapp,
            String descripcion, List<String> servicios, BigDecimal latitud, BigDecimal longitud, boolean activa,
            List<RespuestaHorarioDia> horario, List<RespuestaFoto> fotos, List<RespuestaCancha> canchas) {
    }

    public record RespuestaTramo(Instant inicio, Instant fin, int minutos, int precioHora, String origen) {
    }

    public record RespuestaCotizacion(long total, List<RespuestaTramo> tramos) {
    }

    public record RespuestaFestivo(Long id, LocalDate fecha, String nombre) {
    }

    public record PrevisualizacionEstado(int reservasFuturasActivas, boolean requiereConfirmacion) {
    }
}
