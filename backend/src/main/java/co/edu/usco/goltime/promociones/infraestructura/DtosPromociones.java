package co.edu.usco.goltime.promociones.infraestructura;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import co.edu.usco.goltime.compartido.dominio.TipoDescuento;
import co.edu.usco.goltime.reservas.aplicacion.PoliticaDescuentos.Origen;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class DtosPromociones {

    private DtosPromociones() {
    }

    public record SolicitudPromocion(@NotBlank @Size(max = 100) String nombre, String descripcion, @Size(max = 40) String codigo, @NotNull TipoDescuento tipo,
            @NotNull @Min(1) Integer valor, @NotNull LocalDate fechaDesde, @NotNull LocalDate fechaHasta, Set<Integer> diasSemana, @Min(0) @Max(1439) Integer minutoInicio,
            @Min(1) @Max(1440) Integer minutoFin, Long sedeId, Long canchaId, @Min(1) Integer usosMaximos, @Min(1) Integer usosPorCliente, Boolean soloPrimeraReserva,
            Boolean publica) {
    }

    public record SolicitudActiva(@NotNull Boolean activa) {
    }

    public record RespuestaPromocion(Long id, String nombre, String descripcion, String codigo, TipoDescuento tipo, int valor, String descuentoTexto, LocalDate fechaDesde,
            LocalDate fechaHasta, List<Integer> diasSemana, Integer minutoInicio, Integer minutoFin, Long sedeId, Long canchaId, Integer usosMaximos, Integer usosPorCliente,
            boolean soloPrimeraReserva, boolean publica, boolean activa, boolean automatica) {
    }

    public record RespuestaUso(Long reservaId, Long clienteId, int monto, String estado, Instant creadoEn) {
    }

    public record RespuestaDetallePromocion(RespuestaPromocion promocion, int usosActivos, long montoDescontado, List<RespuestaUso> ultimosUsos) {
    }

    public record RespuestaPrevisualizacion(int precioBase, int descuento, int precioFinal, String descripcion, Origen origen) {
    }
}
