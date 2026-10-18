package co.edu.usco.goltime.reservas.infraestructura;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import co.edu.usco.goltime.compartido.dominio.Rol;
import co.edu.usco.goltime.reservas.dominio.EstadoPago;
import co.edu.usco.goltime.reservas.dominio.EstadoReserva;
import co.edu.usco.goltime.reservas.dominio.OrigenReserva;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Contratos JSON de reservas y disponibilidad. Los instantes viajan en ISO-8601 UTC. */
public final class DtosReservas {

    private DtosReservas() {
    }

    // ----- Solicitudes -----

    public record SolicitudReserva(
            @NotNull Long canchaId,
            @NotNull Instant inicio,
            @NotNull Instant fin,
            @Size(max = 100) String nombreEquipo,
            @Size(max = 500) String notas) {
    }

    public record SolicitudMotivo(@Size(max = 500) String motivo) {
    }

    public record SolicitudMotivoObligatorio(@NotBlank @Size(max = 500) String motivo) {
    }

    /** {@code canchaId} nulo = la misma cancha. */
    public record SolicitudReprogramacion(Long canchaId, @NotNull Instant inicio, @NotNull Instant fin) {
    }

    // ----- Respuestas -----

    public record RespuestaCanchaReserva(Long id, String nombre, String modalidad) {
    }

    public record RespuestaSedeReserva(Long id, String nombre) {
    }

    public record RespuestaClienteReserva(Long id, String nombre, String correo, String telefono) {
    }

    public record RespuestaHistorial(Long id, String accion, Long actorId, String actorNombre, Rol actorRol, String motivo,
            Map<String, Object> datosAnteriores, Map<String, Object> datosNuevos, Instant fecha) {
    }

    public record RespuestaReserva(
            Long id, String codigo, EstadoReserva estado, OrigenReserva origen,
            RespuestaCanchaReserva cancha, RespuestaSedeReserva sede, RespuestaClienteReserva cliente,
            Instant inicio, Instant fin, int duracionMin,
            int precioBase, int descuento, int precioFinal, EstadoPago estadoPago, int montoPagado,
            String nombreEquipo, String notas, Instant expiraEn, int vecesReprogramada,
            boolean cancelacionTardia, Rol canceladaPorRol, String motivoCancelacion, Instant fechaCancelacion, String motivoRechazo,
            boolean puedeCancelar, boolean puedeReprogramar, boolean cancelacionSeriaTardia,
            Instant creadoEn, List<RespuestaHistorial> historial) {
    }

    public record RespuestaPrevisualizacion(boolean disponible, int precioActual, int precioNuevo, int diferencia) {
    }

    public record RespuestaSlot(Instant inicio, Instant fin, String estado, long precio) {
    }

    public record RespuestaDisponibilidadCancha(Long canchaId, String nombre, String modalidad, Long sedeId, String sedeNombre,
            int duracionBloqueMin, int duracionMinMin, int duracionMaxMin, boolean cerrado, List<RespuestaSlot> slots) {
    }

    public record RespuestaDisponibilidad(LocalDate fecha, List<RespuestaDisponibilidadCancha> canchas) {
    }

    public record RespuestaResumenDia(LocalDate fecha, int horariosLibres) {
    }
}
