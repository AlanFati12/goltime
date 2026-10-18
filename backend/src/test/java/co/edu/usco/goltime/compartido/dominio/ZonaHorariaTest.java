package co.edu.usco.goltime.compartido.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

class ZonaHorariaTest {

    @Test
    void bogotaEstaCincoHorasDetrasDeUtc() {
        Instant instante = ZonaHoraria.aInstante(LocalDate.of(2026, 11, 19), LocalTime.of(19, 0));
        assertThat(instante).isEqualTo(Instant.parse("2026-11-20T00:00:00Z"));
    }

    @Test
    void laFechaLocalSeCalculaEnBogotaYNoEnUtc() {
        // 03:00 UTC del 20/nov sigue siendo 19/nov a las 22:00 en Bogotá.
        Instant instante = Instant.parse("2026-11-20T03:00:00Z");
        assertThat(ZonaHoraria.fechaLocal(instante)).isEqualTo(LocalDate.of(2026, 11, 19));
    }
}
