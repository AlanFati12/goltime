package co.edu.usco.goltime.compartido.dominio;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Regla 11: las fechas se guardan en UTC y se muestran/interpretan en America/Bogota.
 * Colombia no tiene horario de verano, pero se usa la zona completa por claridad.
 */
public final class ZonaHoraria {

    public static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    private ZonaHoraria() {
    }

    public static Instant aInstante(LocalDate fecha, LocalTime hora) {
        return ZonedDateTime.of(fecha, hora, BOGOTA).toInstant();
    }

    public static LocalDateTime aHoraLocal(Instant instante) {
        return LocalDateTime.ofInstant(instante, BOGOTA);
    }

    public static LocalDate fechaLocal(Instant instante) {
        return aHoraLocal(instante).toLocalDate();
    }

    public static Instant inicioDelDia(LocalDate fecha) {
        return fecha.atStartOfDay(BOGOTA).toInstant();
    }
}
