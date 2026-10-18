package co.edu.usco.goltime.promociones.dominio;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.compartido.dominio.TipoDescuento;

/**
 * Promoción de descuento. Con {@code codigo} la usa quien lo escribe; sin código se aplica sola a las reservas que cumplan sus condiciones
 * (fechas, días, franja horaria, sede, cancha o primera reserva). Los cupos de uso se verifican aparte, contra los usos registrados.
 */
public record Promocion(Long id, String nombre, String descripcion, String codigo, TipoDescuento tipo, int valor, LocalDate fechaDesde, LocalDate fechaHasta,
        Set<Integer> diasSemana, Integer minutoInicio, Integer minutoFin, Long sedeId, Long canchaId, Integer usosMaximos, Integer usosPorCliente,
        boolean soloPrimeraReserva, boolean publica, boolean activa) {

    /** Datos de la reserva que se compara con la promoción. Los minutos son del día en hora de Bogotá (el fin puede pasar de 1440). */
    public record ContextoReserva(LocalDate fecha, int diaSemana, int minutoInicio, int minutoFin, long sedeId, long canchaId, boolean primeraReserva) {
    }

    public Promocion {
        nombre = limpiar(nombre);
        if (nombre == null || nombre.length() > 100) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "El nombre de la promoción es obligatorio (máximo 100 caracteres).");
        }
        descripcion = limpiar(descripcion);
        codigo = codigo == null || codigo.isBlank() ? null : codigo.trim().toUpperCase();
        if (codigo != null && !codigo.matches("[A-Z0-9_-]{3,40}")) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "El código debe tener de 3 a 40 letras, números, guiones o guion bajo.");
        }
        if (tipo == null) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Indica si el descuento es un porcentaje o un monto fijo.");
        }
        tipo.validarValor(valor);
        if (fechaDesde == null || fechaHasta == null || fechaHasta.isBefore(fechaDesde)) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Indica las fechas de vigencia; la final no puede ser anterior a la inicial.");
        }
        diasSemana = diasSemana == null ? Set.of() : Set.copyOf(diasSemana);
        if (diasSemana.stream().anyMatch(d -> d < 1 || d > 7)) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Los días de la semana van de 1 (lunes) a 7 (domingo).");
        }
        if ((minutoInicio == null) != (minutoFin == null) || (minutoInicio != null && (minutoInicio < 0 || minutoFin > 1440 || minutoFin <= minutoInicio))) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "La franja horaria necesita una hora de inicio y una de fin posterior.");
        }
        if (usosMaximos != null && usosMaximos < 1 || usosPorCliente != null && usosPorCliente < 1) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Los cupos de uso deben ser de al menos 1.");
        }
    }

    public boolean esAutomatica() {
        return codigo == null;
    }

    /** Por qué la promoción no aplica a esta reserva, o vacío si cumple todas las condiciones. */
    public Optional<String> motivoNoAplica(ContextoReserva r) {
        if (!activa) {
            return Optional.of("La promoción no está activa.");
        }
        if (r.fecha().isBefore(fechaDesde) || r.fecha().isAfter(fechaHasta)) {
            return Optional.of("La promoción no está vigente para la fecha de la reserva.");
        }
        if (!diasSemana.isEmpty() && !diasSemana.contains(r.diaSemana())) {
            return Optional.of("La promoción no aplica a ese día de la semana.");
        }
        if (minutoInicio != null && (r.minutoInicio() < minutoInicio || r.minutoFin() > minutoFin)) {
            return Optional.of("La promoción solo aplica en su franja horaria.");
        }
        if (sedeId != null && sedeId != r.sedeId()) {
            return Optional.of("La promoción no aplica a esta sede.");
        }
        if (canchaId != null && canchaId != r.canchaId()) {
            return Optional.of("La promoción no aplica a esta cancha.");
        }
        if (soloPrimeraReserva && !r.primeraReserva()) {
            return Optional.of("La promoción es solo para tu primera reserva.");
        }
        return Optional.empty();
    }

    public int descuentoPara(int precioBase) {
        return tipo.calcular(valor, precioBase);
    }

    public Promocion conActiva(boolean nueva) {
        return new Promocion(id, nombre, descripcion, codigo, tipo, valor, fechaDesde, fechaHasta, diasSemana, minutoInicio, minutoFin, sedeId, canchaId,
                usosMaximos, usosPorCliente, soloPrimeraReserva, publica, nueva);
    }

    private static String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
