package co.edu.usco.goltime.sedes.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.Test;

import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.sedes.dominio.Cancha.DatosCancha;

class CanchaTest {

    private static DatosCancha datos(int bloque, int min, int max) {
        return new DatosCancha("Cancha 1", "F5", 5, 14, null, null, null, false, true, 80_000, bloque, min, max, null, 0);
    }

    @Test
    void creaUnaCanchaActivaConValoresPorDefecto() {
        Cancha c = Cancha.nueva(1L, datos(60, 60, 120), Set.of());
        assertThat(c.estado()).isEqualTo(EstadoCancha.ACTIVA);
        assertThat(c.superficie()).isEqualTo("Sintética");
        assertThat(c.esReservable()).isTrue();
        assertThat(c.esCompuesta()).isFalse();
    }

    @Test
    void lasDuracionesDebenSerMultiplosDelBloque() {
        assertThat(Cancha.nueva(1L, datos(30, 60, 90), Set.of()).duracionMaxMin()).isEqualTo(90);
        assertThatThrownBy(() -> Cancha.nueva(1L, datos(60, 90, 120), Set.of())).isInstanceOf(ExcepcionNegocio.class);
        assertThatThrownBy(() -> Cancha.nueva(1L, datos(60, 120, 60), Set.of())).isInstanceOf(ExcepcionNegocio.class);
        assertThatThrownBy(() -> Cancha.nueva(1L, datos(10, 60, 60), Set.of())).isInstanceOf(ExcepcionNegocio.class);
    }

    @Test
    void validaJugadoresCapacidadYPrecio() {
        assertThatThrownBy(() -> Cancha.nueva(1L, new DatosCancha("C", "F5", 5, 8, null, null, null, false, true, 1, 60, 60, 60, null, 0), Set.of()))
                .as("capacidad menor a dos equipos").isInstanceOf(ExcepcionNegocio.class);
        assertThatThrownBy(() -> Cancha.nueva(1L, new DatosCancha("C", "F5", 5, 14, null, null, null, false, true, -5, 60, 60, 60, null, 0), Set.of()))
                .isInstanceOf(ExcepcionNegocio.class);
        assertThatThrownBy(() -> Cancha.nueva(1L, new DatosCancha(" ", "F5", 5, 14, null, null, null, false, true, 1, 60, 60, 60, null, 0), Set.of()))
                .isInstanceOf(ExcepcionNegocio.class);
    }

    @Test
    void unaCanchaNoPuedeDependerDeSiMisma() {
        Cancha c = Cancha.reconstruir(7L, 1L, datos(60, 60, 120), EstadoCancha.ACTIVA, null, Set.of());
        assertThatThrownBy(() -> c.definirComponentes(Set.of(7L))).isInstanceOf(ExcepcionNegocio.class);
        c.definirComponentes(Set.of(8L, 9L));
        assertThat(c.esCompuesta()).isTrue();
    }

    @Test
    void soloLasCanchasActivasSonReservables() {
        Cancha c = Cancha.nueva(1L, datos(60, 60, 120), Set.of());
        c.cambiarEstado(EstadoCancha.MANTENIMIENTO);
        assertThat(c.esReservable()).isFalse();
        c.cambiarEstado(EstadoCancha.INACTIVA);
        assertThat(c.esReservable()).isFalse();
    }
}
