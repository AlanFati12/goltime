package co.edu.usco.goltime.estadisticas.dominio;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/** Cómo se agrupa una serie en el tiempo. Las cubetas se identifican por su primer día (semana desde el lunes). */
public enum Agrupacion {
    DIA("day"),
    SEMANA("week"),
    MES("month");

    private final String truncado;

    Agrupacion(String truncado) {
        this.truncado = truncado;
    }

    /** Argumento de {@code date_trunc} de PostgreSQL. */
    public String truncado() {
        return truncado;
    }

    /** Primer día de la cubeta que contiene a {@code fecha}. */
    public LocalDate inicioDeCubeta(LocalDate fecha) {
        return switch (this) {
            case DIA -> fecha;
            case SEMANA -> fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MES -> fecha.withDayOfMonth(1);
        };
    }

    /** Todas las cubetas que tocan el periodo, en orden, para mostrar también los días sin movimiento. */
    public List<LocalDate> cubetas(PeriodoEstadistico periodo) {
        List<LocalDate> cubetas = new ArrayList<>();
        LocalDate actual = inicioDeCubeta(periodo.desde());
        while (!actual.isAfter(periodo.hasta())) {
            cubetas.add(actual);
            actual = switch (this) {
                case DIA -> actual.plusDays(1);
                case SEMANA -> actual.plusWeeks(1);
                case MES -> actual.plusMonths(1);
            };
        }
        return cubetas;
    }
}
