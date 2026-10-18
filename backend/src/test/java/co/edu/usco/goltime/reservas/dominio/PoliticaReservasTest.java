package co.edu.usco.goltime.reservas.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class PoliticaReservasTest {

    private final PoliticaReservas politica = PoliticaReservas.porDefecto();
    private final Instant ahora = Instant.parse("2026-11-10T15:00:00Z");

    @Test
    void laSolicitudExpiraA_las12HorasSiElInicioEstaLejos() {
        Instant inicio = ahora.plusSeconds(3 * 24 * 3600);
        assertThat(politica.calcularExpiracion(ahora, inicio)).isEqualTo(ahora.plusSeconds(12 * 3600));
    }

    @Test
    void laSolicitudExpiraDosHorasAntesDelInicioSiEsoOcurrePrimero() {
        Instant inicio = ahora.plusSeconds(6 * 3600);
        assertThat(politica.calcularExpiracion(ahora, inicio)).isEqualTo(inicio.minusSeconds(2 * 3600));
    }

    @Test
    void conUnInicioJustoALas14HorasGanaElLimiteDeLas12Horas() {
        Instant inicio = ahora.plusSeconds(14 * 3600);
        assertThat(politica.calcularExpiracion(ahora, inicio)).isEqualTo(ahora.plusSeconds(12 * 3600));
    }

    @Test
    void laAntelacionSuficienteSeMideContraLasHorasMinimas() {
        assertThat(politica.conAntelacionSuficiente(ahora, ahora.plusSeconds(4 * 3600))).isTrue();
        assertThat(politica.conAntelacionSuficiente(ahora, ahora.plusSeconds(4 * 3600 - 1))).isFalse();
        assertThat(politica.conAntelacionSuficiente(ahora, ahora.plusSeconds(24 * 3600))).isTrue();
    }
}
