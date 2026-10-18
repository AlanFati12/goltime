package co.edu.usco.goltime.autenticacion.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import co.edu.usco.goltime.autenticacion.aplicacion.PropiedadesAutenticacion;

class ControlIntentosMemoriaTest {

    private static final PropiedadesAutenticacion PROPIEDADES =
            new PropiedadesAutenticacion(15, 7, 12, 3, 15, false, "http://localhost");

    /** Reloj que se puede adelantar manualmente. */
    private static final class RelojMutable extends Clock {
        Instant ahora = Instant.parse("2026-10-18T10:00:00Z");

        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return ahora; }
    }

    @Test
    void bloqueaTrasLosIntentosMaximosYSeLevantaConElTiempo() {
        RelojMutable reloj = new RelojMutable();
        ControlIntentosMemoria control = new ControlIntentosMemoria(PROPIEDADES, reloj);

        control.registrarFallo("a|1.1.1.1");
        control.registrarFallo("a|1.1.1.1");
        assertThat(control.estaBloqueado("a|1.1.1.1")).isFalse();

        control.registrarFallo("a|1.1.1.1");
        assertThat(control.estaBloqueado("a|1.1.1.1")).isTrue();
        assertThat(control.estaBloqueado("a|2.2.2.2")).as("otra IP no queda bloqueada").isFalse();

        reloj.ahora = reloj.ahora.plusSeconds(15 * 60);
        assertThat(control.estaBloqueado("a|1.1.1.1")).isFalse();
    }

    @Test
    void ingresarBienReiniciaElContador() {
        ControlIntentosMemoria control = new ControlIntentosMemoria(PROPIEDADES, new RelojMutable());
        control.registrarFallo("b|x");
        control.registrarFallo("b|x");
        control.limpiar("b|x");
        control.registrarFallo("b|x");
        assertThat(control.estaBloqueado("b|x")).isFalse();
    }
}
