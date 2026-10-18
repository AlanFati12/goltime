package co.edu.usco.goltime.compartido.dominio;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formatos de texto para mensajes al usuario: fecha en hora de Bogotá y moneda COP sin decimales. */
public final class FormatoTexto {

    private static final Locale COLOMBIA = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter FECHA_HORA =
            DateTimeFormatter.ofPattern("EEE dd/MM/yyyy HH:mm", COLOMBIA).withZone(ZonaHoraria.BOGOTA);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm", COLOMBIA).withZone(ZonaHoraria.BOGOTA);

    private FormatoTexto() {
    }

    /** "jue 13/11/2026 19:00" */
    public static String fechaHora(Instant instante) {
        return FECHA_HORA.format(instante);
    }

    public static String hora(Instant instante) {
        return HORA.format(instante);
    }

    /** 80000 -> "$ 80.000" */
    public static String cop(long valor) {
        NumberFormat formato = NumberFormat.getIntegerInstance(COLOMBIA);
        formato.setGroupingUsed(true);
        return "$ " + formato.format(valor);
    }
}
