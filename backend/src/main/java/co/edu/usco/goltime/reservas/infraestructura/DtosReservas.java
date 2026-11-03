package co.edu.usco.goltime.reservas.infraestructura;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import co.edu.usco.goltime.compartido.dominio.Rol;
import co.edu.usco.goltime.reservas.dominio.MetodoPago;
import co.edu.usco.goltime.reservas.dominio.MotivoBloqueo;
import co.edu.usco.goltime.reservas.dominio.Bloqueo;
import co.edu.usco.goltime.reservas.dominio.TipoPago;
import java.util.Set;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
            @Size(max = 500) String notas,
            @Size(max = 40) String codigoPromocion,
            Long premioId) {
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

    // ----- Administración: solicitudes -----

    public record SolicitudMostrador(@NotBlank @Size(max = 150) String nombre, @NotBlank @Size(max = 30) String telefono) {
    }

    /** Un cliente registrado ({@code clienteId}) o uno de mostrador ({@code mostrador}), nunca ambos. */
    public record SolicitudReservaAdmin(
            @NotNull Long canchaId,
            @NotNull Instant inicio,
            @NotNull Instant fin,
            Long clienteId,
            @Valid SolicitudMostrador mostrador,
            @Size(max = 100) String nombreEquipo,
            @Size(max = 500) String notas) {
    }

    public record SolicitudReprogramacionAdmin(Long canchaId, @NotNull Instant inicio, @NotNull Instant fin,
            @NotBlank @Size(max = 500) String motivo) {
    }

    public record SolicitudEstadoReserva(@NotNull EstadoReserva estado, @NotBlank @Size(max = 500) String motivo) {
    }

    public record RespuestaPrevisualizacionEstado(EstadoReserva actual, EstadoReserva nuevo, boolean ocuparaHorario, boolean horarioLibre,
            boolean liberaraHorario, boolean cuentaParaPremios, boolean dejaDeContarParaPremios, boolean quedaReembolsoPendiente,
            boolean seCompletaraSola, boolean expiraraSola, boolean avisaAlCliente) {
    }

    public record SolicitudPago(@Min(1) @Max(100_000_000) int monto, @NotNull MetodoPago metodo, @Size(max = 100) String referencia) {
    }

    public record SolicitudReembolso(@Min(1) @Max(100_000_000) int monto, @NotNull MetodoPago metodo, @Size(max = 100) String referencia,
            boolean cerrar) {
    }

    // ----- Administración: respuestas -----

    public record RespuestaPago(Long id, int monto, MetodoPago metodo, String referencia, TipoPago tipo, String registradoPor, Instant fecha) {
    }

    public record RespuestaMostrador(Long id, String nombre, String telefono) {
    }

    public record RespuestaColumnaAgenda(Long id, String nombre, String modalidad, String estado, boolean esCompuesta) {
    }

    public record RespuestaElementoAgenda(String tipo, Long canchaId, Instant inicio, Instant fin, Long reservaId, String codigo, String estado,
            String cliente, String equipo, Integer precioFinal, String estadoPago, String origen, String canchaReservaNombre,
            Long bloqueoId, String motivoBloqueo) {
    }

    public record RespuestaAgenda(List<RespuestaColumnaAgenda> canchas, List<RespuestaElementoAgenda> elementos) {
    }

    // ----- Bloqueos -----

    public record SolicitudBloqueo(
            @NotNull Long sedeId,
            Set<Long> canchaIds,
            @NotNull Bloqueo.Tipo tipo,
            Instant inicio,
            Instant fin,
            boolean todoElDia,
            Set<@Min(1) @Max(7) Integer> diasSemana,
            String horaInicio,
            String horaFin,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            @NotNull MotivoBloqueo motivo,
            @Size(max = 500) String detalle,
            @Size(max = 64) String huellaConfirmada) {
    }

    public record RespuestaBloqueo(Long id, Long sedeId, List<Long> canchaIds, Bloqueo.Tipo tipo, Instant inicio, Instant fin, boolean todoElDia,
            List<Integer> diasSemana, String horaInicio, String horaFin, LocalDate fechaDesde, LocalDate fechaHasta, MotivoBloqueo motivo,
            String detalle, String motivoTexto, boolean activo) {
    }

    public record RespuestaConflicto(Long reservaId, String codigo, String cliente, String cancha, Instant inicio, Instant fin, int precioFinal,
            boolean conPago, String estadoPago, String estado) {
    }

    public record RespuestaPrevisualizacionBloqueo(List<RespuestaConflicto> conflictos, int intervalos, int canchasAfectadas, String huella,
            boolean requiereConfirmacion) {
    }
}
