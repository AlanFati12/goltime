package co.edu.usco.goltime.reservas.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class GeneradorIcsTest {

    private static final Instant INICIO = Instant.parse("2026-11-10T00:00:00Z"); // 19:00 en Bogotá
    private static final Instant FIN = Instant.parse("2026-11-10T01:00:00Z");

    @Test
    void generaUnEventoConHorasEnUtcYAvisoPrevio() {
        String ics = GeneradorIcs.evento("GT-2026-000123", "Cancha 1 · Goltime", "Goltime Centro", "Reserva GT-2026-000123", INICIO, FIN,
                Instant.parse("2026-11-01T12:00:00Z"));

        assertThat(ics).startsWith("BEGIN:VCALENDAR\r\n").endsWith("END:VCALENDAR\r\n");
        assertThat(ics).contains("UID:GT-2026-000123@goltime\r\n", "DTSTART:20261110T000000Z\r\n", "DTEND:20261110T010000Z\r\n", "DTSTAMP:20261101T120000Z\r\n");
        assertThat(ics).contains("SUMMARY:Cancha 1 · Goltime\r\n", "LOCATION:Goltime Centro\r\n", "TRIGGER:-PT1H\r\n");
        assertThat(ics.lines().filter(l -> l.endsWith("\r")).count()).isZero(); // lines() ya separa CRLF
        assertThat(ics.split("\r\n")).allSatisfy(l -> assertThat(l.getBytes(java.nio.charset.StandardCharsets.UTF_8).length).isLessThanOrEqualTo(75));
    }

    @Test
    void escapaLosCaracteresEspecialesYOmiteLosCamposVacios() {
        String ics = GeneradorIcs.evento("X", "Partido; semifinal, ida\\vuelta\nsegundo tiempo", null, " ", INICIO, FIN, INICIO);

        assertThat(ics).contains("SUMMARY:Partido\\; semifinal\\, ida\\\\vuelta\\nsegundo tiempo\r\n");
        assertThat(ics).doesNotContain("LOCATION").doesNotContain("DESCRIPTION:  ");
    }

    @Test
    void plieguaLasLineasLargas() {
        String largo = "a".repeat(200);

        String ics = GeneradorIcs.evento("X", largo, null, null, INICIO, FIN, INICIO);

        String[] lineas = ics.split("\r\n");
        assertThat(lineas).allSatisfy(l -> assertThat(l.length()).isLessThanOrEqualTo(75));
        // desplegadas (quitando el espacio inicial de las continuaciones) reconstruyen el texto original
        assertThat(ics.replace("\r\n ", "")).contains("SUMMARY:" + largo);
    }
}
