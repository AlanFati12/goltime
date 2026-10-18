package co.edu.usco.goltime.promociones.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;

import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.compartido.dominio.TipoDescuento;
import co.edu.usco.goltime.promociones.dominio.Promocion.ContextoReserva;

class PromocionTest {

    private static final LocalDate MARTES = LocalDate.of(2026, 11, 3);

    private static Promocion promo(TipoDescuento tipo, int valor, Set<Integer> dias, Integer desde, Integer hasta, Long sede, Long cancha, boolean primera,
            boolean activa) {
        return new Promocion(1L, "Promo", null, null, tipo, valor, MARTES.minusDays(5), MARTES.plusDays(5), dias, desde, hasta, sede, cancha, null, null, primera, true,
                activa);
    }

    private static ContextoReserva ctx(LocalDate fecha, int inicio, int fin) {
        return new ContextoReserva(fecha, fecha.getDayOfWeek().getValue(), inicio, fin, 10L, 20L, false);
    }

    @Test
    void sinCondicionesAplicaATodaReservaVigente() {
        Promocion p = promo(TipoDescuento.PORCENTAJE, 20, Set.of(), null, null, null, null, false, true);
        assertThat(p.motivoNoAplica(ctx(MARTES, 18 * 60, 19 * 60))).isEmpty();
        assertThat(p.esAutomatica()).isTrue();
    }

    @Test
    void respetaVigenciaDiaFranjaSedeYCancha() {
        Promocion p = promo(TipoDescuento.MONTO_FIJO, 5_000, Set.of(2, 3), 14 * 60, 18 * 60, 10L, 20L, false, true);

        assertThat(p.motivoNoAplica(ctx(MARTES, 15 * 60, 16 * 60))).isEmpty();
        assertThat(p.motivoNoAplica(ctx(MARTES.plusDays(10), 15 * 60, 16 * 60))).hasValueSatisfying(m -> assertThat(m).contains("vigente"));
        assertThat(p.motivoNoAplica(ctx(MARTES.plusDays(2), 15 * 60, 16 * 60))).hasValueSatisfying(m -> assertThat(m).contains("día"));
        assertThat(p.motivoNoAplica(ctx(MARTES, 13 * 60, 14 * 60))).hasValueSatisfying(m -> assertThat(m).contains("franja"));
        assertThat(p.motivoNoAplica(ctx(MARTES, 17 * 60, 19 * 60))).as("debe terminar dentro de la franja").isPresent();
        assertThat(p.motivoNoAplica(new ContextoReserva(MARTES, 2, 15 * 60, 16 * 60, 99L, 20L, false))).hasValueSatisfying(m -> assertThat(m).contains("sede"));
        assertThat(p.motivoNoAplica(new ContextoReserva(MARTES, 2, 15 * 60, 16 * 60, 10L, 99L, false))).hasValueSatisfying(m -> assertThat(m).contains("cancha"));
    }

    @Test
    void laPromocionInactivaOSoloParaLaPrimeraReservaNoAplica() {
        assertThat(promo(TipoDescuento.PORCENTAJE, 10, Set.of(), null, null, null, null, false, false).motivoNoAplica(ctx(MARTES, 600, 660))).isPresent();
        Promocion primera = promo(TipoDescuento.PORCENTAJE, 10, Set.of(), null, null, null, null, true, true);
        assertThat(primera.motivoNoAplica(ctx(MARTES, 600, 660))).hasValueSatisfying(m -> assertThat(m).contains("primera"));
        assertThat(primera.motivoNoAplica(new ContextoReserva(MARTES, 2, 600, 660, 10L, 20L, true))).isEmpty();
    }

    @Test
    void elDescuentoNuncaSuperaElPrecioNiEsNegativo() {
        assertThat(TipoDescuento.PORCENTAJE.calcular(20, 80_000)).isEqualTo(16_000);
        assertThat(TipoDescuento.PORCENTAJE.calcular(100, 80_000)).isEqualTo(80_000);
        assertThat(TipoDescuento.PORCENTAJE.calcular(33, 10_000)).isEqualTo(3_300);
        assertThat(TipoDescuento.MONTO_FIJO.calcular(5_000, 80_000)).isEqualTo(5_000);
        assertThat(TipoDescuento.MONTO_FIJO.calcular(100_000, 80_000)).isEqualTo(80_000);
        assertThat(TipoDescuento.MONTO_FIJO.calcular(5_000, 0)).isZero();
        assertThat(TipoDescuento.PORCENTAJE.texto(20)).isEqualTo("20%");
    }

    @Test
    void validaLosDatosAlConstruirla() {
        assertThatThrownBy(() -> promo(TipoDescuento.PORCENTAJE, 101, Set.of(), null, null, null, null, false, true)).isInstanceOf(ExcepcionNegocio.class);
        assertThatThrownBy(() -> promo(TipoDescuento.MONTO_FIJO, 0, Set.of(), null, null, null, null, false, true)).isInstanceOf(ExcepcionNegocio.class);
        assertThatThrownBy(() -> promo(TipoDescuento.PORCENTAJE, 10, Set.of(8), null, null, null, null, false, true)).isInstanceOf(ExcepcionNegocio.class);
        assertThatThrownBy(() -> promo(TipoDescuento.PORCENTAJE, 10, Set.of(), 600, null, null, null, false, true)).isInstanceOf(ExcepcionNegocio.class);
        assertThatThrownBy(() -> promo(TipoDescuento.PORCENTAJE, 10, Set.of(), 700, 600, null, null, false, true)).isInstanceOf(ExcepcionNegocio.class);
        assertThatThrownBy(() -> new Promocion(null, "x", null, "mal codigo!", TipoDescuento.PORCENTAJE, 10, MARTES, MARTES, Set.of(), null, null, null, null, null, null,
                false, true, true)).isInstanceOf(ExcepcionNegocio.class);
        assertThatThrownBy(() -> new Promocion(null, "x", null, null, TipoDescuento.PORCENTAJE, 10, MARTES, MARTES.minusDays(1), Set.of(), null, null, null, null, null,
                null, false, true, true)).isInstanceOf(ExcepcionNegocio.class);
    }

    @Test
    void elCodigoSeGuardaEnMayusculas() {
        Promocion p = new Promocion(null, "Verano", null, " verano-20 ", TipoDescuento.PORCENTAJE, 20, MARTES, MARTES, Set.of(), null, null, null, null, null, null, false,
                true, true);
        assertThat(p.codigo()).isEqualTo("VERANO-20");
        assertThat(p.esAutomatica()).isFalse();
    }
}
