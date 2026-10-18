package co.edu.usco.goltime.estadisticas.dominio;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.compartido.dominio.ZonaHoraria;

/** Rango de fechas (inclusive, hora de Bogotá) sobre el que se calculan las estadísticas. */
public record PeriodoEstadistico(LocalDate desde, LocalDate hasta) {

    public static final int DIAS_MAXIMOS = 366;

    public PeriodoEstadistico {
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Indica un periodo válido: la fecha final no puede ser anterior a la inicial.");
        }
        if (ChronoUnit.DAYS.between(desde, hasta) + 1 > DIAS_MAXIMOS) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "El periodo no puede superar " + DIAS_MAXIMOS + " días.");
        }
    }

    public int dias() {
        return (int) ChronoUnit.DAYS.between(desde, hasta) + 1;
    }

    /** Primer instante del periodo (medianoche de Bogotá del primer día). */
    public Instant inicio() {
        return ZonaHoraria.inicioDelDia(desde);
    }

    /** Primer instante posterior al periodo (límite superior excluyente). */
    public Instant fin() {
        return ZonaHoraria.inicioDelDia(hasta.plusDays(1));
    }

    /** El periodo inmediatamente anterior, de la misma duración, para comparar. */
    public PeriodoEstadistico anterior() {
        return new PeriodoEstadistico(desde.minusDays(dias()), desde.minusDays(1));
    }

    /** Variación porcentual de {@code actual} respecto de {@code previo}; {@code null} si no hay base para comparar. */
    public static Double variacion(double actual, double previo) {
        if (previo == 0) {
            return null;
        }
        return Math.round((actual - previo) / previo * 1000.0) / 10.0;
    }
}
