package co.edu.usco.goltime.reservas.dominio;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Evento de calendario (iCalendar, RFC 5545) de una reserva, para agregarla al calendario del celular o de Google/Outlook.
 * Las horas viajan en UTC; el calendario del cliente las muestra en su zona. Incluye un aviso 1 hora antes.
 */
public final class GeneradorIcs {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

    private GeneradorIcs() {
    }

    public static String evento(String codigo, String resumen, String ubicacion, String descripcion, Instant inicio, Instant fin, Instant ahora) {
        StringBuilder ics = new StringBuilder();
        linea(ics, "BEGIN:VCALENDAR");
        linea(ics, "VERSION:2.0");
        linea(ics, "PRODID:-//Goltime//Reservas//ES");
        linea(ics, "CALSCALE:GREGORIAN");
        linea(ics, "METHOD:PUBLISH");
        linea(ics, "BEGIN:VEVENT");
        linea(ics, "UID:" + codigo + "@goltime");
        linea(ics, "DTSTAMP:" + FECHA.format(ahora));
        linea(ics, "DTSTART:" + FECHA.format(inicio));
        linea(ics, "DTEND:" + FECHA.format(fin));
        linea(ics, "SUMMARY:" + escapar(resumen));
        if (ubicacion != null && !ubicacion.isBlank()) {
            linea(ics, "LOCATION:" + escapar(ubicacion));
        }
        if (descripcion != null && !descripcion.isBlank()) {
            linea(ics, "DESCRIPTION:" + escapar(descripcion));
        }
        linea(ics, "STATUS:CONFIRMED");
        linea(ics, "BEGIN:VALARM");
        linea(ics, "ACTION:DISPLAY");
        linea(ics, "DESCRIPTION:Tu reserva en Goltime empieza en 1 hora");
        linea(ics, "TRIGGER:-PT1H");
        linea(ics, "END:VALARM");
        linea(ics, "END:VEVENT");
        linea(ics, "END:VCALENDAR");
        return ics.toString();
    }

    /** Escapa barra, punto y coma, coma y saltos de línea, como exige el formato. */
    static String escapar(String texto) {
        return texto.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\r\n", "\\n").replace("\n", "\\n");
    }

    /** Líneas de máximo 75 octetos: las más largas continúan en la siguiente con un espacio al inicio. */
    private static void linea(StringBuilder salida, String contenido) {
        StringBuilder actual = new StringBuilder();
        int octetos = 0;
        for (int i = 0; i < contenido.length(); ) {
            int punto = contenido.codePointAt(i);
            String caracter = new String(Character.toChars(punto));
            int largo = caracter.getBytes(StandardCharsets.UTF_8).length;
            if (octetos + largo > 75) {
                salida.append(actual).append("\r\n");
                actual = new StringBuilder(" ");
                octetos = 1;
            }
            actual.append(caracter);
            octetos += largo;
            i += Character.charCount(punto);
        }
        salida.append(actual).append("\r\n");
    }
}
