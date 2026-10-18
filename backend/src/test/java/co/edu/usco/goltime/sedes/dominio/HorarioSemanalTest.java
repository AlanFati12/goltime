package co.edu.usco.goltime.sedes.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.util.List;

import org.junit.jupiter.api.Test;

import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;

class HorarioSemanalTest {

    @Test
    void elHorarioPorDefectoAbreDeLunesADomingo() {
        HorarioSemanal h = HorarioSemanal.porDefecto();
        assertThat(h.dias()).hasSize(7);
        assertThat(h.deDia(DayOfWeek.SATURDAY).orElseThrow().minutoCierre()).isEqualTo(23 * 60);
        assertThat(h.deDia(DayOfWeek.SUNDAY).orElseThrow().minutoCierre()).isEqualTo(20 * 60);
    }

    @Test
    void rechazaDiasRepetidosYRangosInvalidos() {
        assertThatThrownBy(() -> new HorarioSemanal(List.of(HorarioDia.abierto(1, "08:00", "20:00"), HorarioDia.cerradoEl(1))))
                .isInstanceOf(ExcepcionNegocio.class);
        assertThatThrownBy(() -> HorarioDia.abierto(1, "20:00", "08:00")).isInstanceOf(ExcepcionNegocio.class);
        assertThatThrownBy(() -> HorarioDia.abierto(8, "08:00", "20:00")).isInstanceOf(ExcepcionNegocio.class);
    }

    @Test
    void permiteCerrarALaMedianoche() {
        HorarioDia dia = HorarioDia.abierto(5, "10:00", "24:00");
        assertThat(dia.minutoCierre()).isEqualTo(1440);
        assertThat(dia.contiene(23 * 60, 24 * 60)).isTrue();
    }

    @Test
    void contieneSoloIntervalosDentroDelHorario() {
        HorarioDia dia = HorarioDia.abierto(2, "08:00", "23:00");
        assertThat(dia.contiene(8 * 60, 9 * 60)).isTrue();
        assertThat(dia.contiene(7 * 60, 9 * 60)).isFalse();
        assertThat(dia.contiene(22 * 60, 24 * 60)).isFalse();
        assertThat(HorarioDia.cerradoEl(2).contiene(10 * 60, 11 * 60)).isFalse();
    }

    @Test
    void elHorarioPropioDeLaCanchaReemplazaAlDeLaSedeSoloEnLosDiasDefinidos() {
        HorarioSemanal sede = HorarioSemanal.porDefecto();
        HorarioSemanal propio = new HorarioSemanal(List.of(HorarioDia.cerradoEl(1), HorarioDia.abierto(2, "10:00", "14:00")));

        assertThat(HorarioSemanal.efectivo(DayOfWeek.MONDAY, sede, propio).cerrado()).isTrue();
        assertThat(HorarioSemanal.efectivo(DayOfWeek.TUESDAY, sede, propio).minutoCierre()).isEqualTo(14 * 60);
        assertThat(HorarioSemanal.efectivo(DayOfWeek.WEDNESDAY, sede, propio).minutoCierre()).isEqualTo(23 * 60);
        assertThat(HorarioSemanal.efectivo(DayOfWeek.WEDNESDAY, sede, null).minutoCierre()).isEqualTo(23 * 60);
    }

    @Test
    void unDiaSinHorarioSeConsideraCerrado() {
        assertThat(HorarioSemanal.efectivo(DayOfWeek.FRIDAY, new HorarioSemanal(List.of()), null).cerrado()).isTrue();
    }

    @Test
    void convierteTextoAMinutos() {
        assertThat(MinutoDelDia.analizar("08:30")).isEqualTo(510);
        assertThat(MinutoDelDia.analizar("24:00")).isEqualTo(1440);
        assertThat(MinutoDelDia.formatear(1440)).isEqualTo("24:00");
        assertThatThrownBy(() -> MinutoDelDia.analizar("25:00")).isInstanceOf(ExcepcionNegocio.class);
        assertThatThrownBy(() -> MinutoDelDia.analizar("8h")).isInstanceOf(ExcepcionNegocio.class);
        assertThatThrownBy(() -> MinutoDelDia.analizar("12:75")).isInstanceOf(ExcepcionNegocio.class);
    }
}
