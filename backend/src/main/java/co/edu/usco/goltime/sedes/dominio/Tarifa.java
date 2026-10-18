package co.edu.usco.goltime.sedes.dominio;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;

/**
 * Franja de tarifa de una cancha: precio por hora para ciertos días y un rango horario.
 * Una franja con {@code aplicaFestivo} rige solo en festivos (ignora los días de la semana);
 * una franja normal rige en los días indicados, también en festivos si no hay una franja de festivo.
 */
public record Tarifa(Long id, long canchaId, Set<DayOfWeek> diasSemana, int minutoInicio, int minutoFin,
        boolean aplicaFestivo, int precioHora, LocalDate vigenteDesde, LocalDate vigenteHasta) {

    public Tarifa {
        diasSemana = diasSemana == null ? Set.of() : Set.copyOf(diasSemana);
        if (minutoInicio < 0 || minutoFin > MinutoDelDia.FIN_DEL_DIA || minutoFin <= minutoInicio) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "La hora final de la franja debe ser posterior a la inicial.");
        }
        if (precioHora < 0) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "El precio por hora no puede ser negativo.");
        }
        if (!aplicaFestivo && diasSemana.isEmpty()) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Indica los días de la semana de la franja o márcala como de festivo.");
        }
        if (vigenteDesde != null && vigenteHasta != null && vigenteHasta.isBefore(vigenteDesde)) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "La vigencia final no puede ser anterior a la inicial.");
        }
    }

    public boolean vigenteEn(LocalDate fecha) {
        return (vigenteDesde == null || !fecha.isBefore(vigenteDesde)) && (vigenteHasta == null || !fecha.isAfter(vigenteHasta));
    }

    /** ¿Rige esta franja en esa fecha y a ese minuto del día? {@code festivo} indica si la fecha es festivo. */
    public boolean rigeEn(LocalDate fecha, boolean festivo, int minuto) {
        if (!vigenteEn(fecha) || minuto < minutoInicio || minuto >= minutoFin) {
            return false;
        }
        return aplicaFestivo ? festivo : diasSemana.contains(fecha.getDayOfWeek());
    }

    /** Dos franjas se solapan si comparten algún día (o ambas son de festivo), vigencia y rango horario. */
    public boolean seSolapaCon(Tarifa otra) {
        if (this.aplicaFestivo != otra.aplicaFestivo) {
            return false;
        }
        boolean comparteDia = aplicaFestivo || diasSemana.stream().anyMatch(otra.diasSemana::contains);
        boolean comparteHora = minutoInicio < otra.minutoFin && otra.minutoInicio < minutoFin;
        boolean comparteVigencia = (vigenteHasta == null || otra.vigenteDesde == null || !vigenteHasta.isBefore(otra.vigenteDesde))
                && (otra.vigenteHasta == null || vigenteDesde == null || !otra.vigenteHasta.isBefore(vigenteDesde));
        return comparteDia && comparteHora && comparteVigencia;
    }
}
