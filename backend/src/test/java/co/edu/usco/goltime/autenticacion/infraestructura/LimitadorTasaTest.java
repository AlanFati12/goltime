package co.edu.usco.goltime.autenticacion.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class LimitadorTasaTest {

    @Test
    void permiteHastaLaCapacidadYLuegoRechaza() {
        LimitadorTasa limitador = new LimitadorTasa(3, Duration.ofMinutes(1));
        assertThat(limitador.permitir("ip1")).isTrue();
        assertThat(limitador.permitir("ip1")).isTrue();
        assertThat(limitador.permitir("ip1")).isTrue();
        assertThat(limitador.permitir("ip1")).isFalse();
    }

    @Test
    void cadaClaveTieneSuPropioCubo() {
        LimitadorTasa limitador = new LimitadorTasa(1, Duration.ofMinutes(1));
        assertThat(limitador.permitir("ip1")).isTrue();
        assertThat(limitador.permitir("ip1")).isFalse();
        assertThat(limitador.permitir("ip2")).isTrue();
    }
}
