package co.edu.usco.goltime.sedes.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;

import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;

class TarifaTest {

    private static Tarifa t(Set<DayOfWeek> dias, int ini, int fin, boolean festivo) {
        return new Tarifa(null, 1L, dias, ini, fin, festivo, 70_000, null, null);
    }

    @Test
    void validaRangoPrecioYDias() {
        assertThatThrownBy(() -> t(Set.of(DayOfWeek.MONDAY), 600, 600, false)).isInstanceOf(ExcepcionNegocio.class);
        assertThatThrownBy(() -> t(Set.of(DayOfWeek.MONDAY), 600, 1500, false)).isInstanceOf(ExcepcionNegocio.class);
        assertThatThrownBy(() -> t(Set.of(), 600, 700, false)).isInstanceOf(ExcepcionNegocio.class);
        assertThatThrownBy(() -> new Tarifa(null, 1L, Set.of(DayOfWeek.MONDAY), 0, 60, false, -1, null, null)).isInstanceOf(ExcepcionNegocio.class);
        assertThatThrownBy(() -> new Tarifa(null, 1L, Set.of(DayOfWeek.MONDAY), 0, 60, false, 1, LocalDate.of(2026, 5, 2), LocalDate.of(2026, 5, 1)))
                .isInstanceOf(ExcepcionNegocio.class);
        assertThat(t(Set.of(), 600, 700, true).aplicaFestivo()).isTrue();
    }

    @Test
    void detectaSolapamientos() {
        Tarifa a = t(Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY), 360, 1020, false);
        assertThat(a.seSolapaCon(t(Set.of(DayOfWeek.TUESDAY), 600, 720, false))).as("mismo día y horas cruzadas").isTrue();
        assertThat(a.seSolapaCon(t(Set.of(DayOfWeek.TUESDAY), 1020, 1380, false))).as("contiguas no se solapan").isFalse();
        assertThat(a.seSolapaCon(t(Set.of(DayOfWeek.WEDNESDAY), 600, 720, false))).as("otro día").isFalse();
        assertThat(a.seSolapaCon(t(Set.of(), 600, 720, true))).as("festivo y normal no chocan").isFalse();
        assertThat(t(Set.of(), 600, 720, true).seSolapaCon(t(Set.of(), 700, 800, true))).isTrue();
    }

    @Test
    void laVigenciaEvitaElChoque() {
        Tarifa enero = new Tarifa(null, 1L, Set.of(DayOfWeek.MONDAY), 0, 60, false, 1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
        Tarifa febrero = new Tarifa(null, 1L, Set.of(DayOfWeek.MONDAY), 0, 60, false, 1, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
        assertThat(enero.seSolapaCon(febrero)).isFalse();
    }
}
