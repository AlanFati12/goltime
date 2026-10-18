package co.edu.usco.goltime.reservas.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.compartido.dominio.ZonaHoraria;
import co.edu.usco.goltime.reservas.dominio.Bloqueo.Intervalo;

class BloqueoTest {

    private static final Instant AHORA = ZonaHoraria.aInstante(LocalDate.of(2026, 11, 2), LocalTime.of(9, 0)); // lunes

    private static Instant en(LocalDate fecha, String hora) {
        return ZonaHoraria.aInstante(fecha, LocalTime.parse(hora));
    }

    @Test
    void unBloqueoPuntualOcupaElIntervaloIndicado() {
        LocalDate dia = LocalDate.of(2026, 11, 10);
        Bloqueo b = Bloqueo.puntual(null, 1L, Set.of(), en(dia, "14:00"), en(dia, "18:00"), false, MotivoBloqueo.EVENTO_PRIVADO, "Cumpleaños", true);
        assertThat(b.expandir(AHORA)).containsExactly(new Intervalo(en(dia, "14:00"), en(dia, "18:00")));
        assertThat(b.motivoTexto()).isEqualTo("Evento privado: Cumpleaños");
    }

    @Test
    void todoElDiaSeExtiendeDeMedianocheAMedianocheEnBogota() {
        LocalDate d1 = LocalDate.of(2026, 11, 10);
        LocalDate d2 = LocalDate.of(2026, 11, 11);
        Bloqueo b = Bloqueo.puntual(null, 1L, Set.of(), en(d1, "15:00"), en(d2, "09:00"), true, MotivoBloqueo.CLIMA, null, true);
        assertThat(b.expandir(AHORA)).containsExactly(new Intervalo(ZonaHoraria.inicioDelDia(d1), ZonaHoraria.inicioDelDia(d2.plusDays(1))));
        assertThat(b.motivoTexto()).isEqualTo("Clima");
    }

    @Test
    void unBloqueoRecurrenteGeneraUnIntervaloPorCadaDiaQueCoincide() {
        // martes de noviembre de 2026: 3, 10, 17, 24
        Bloqueo b = Bloqueo.recurrente(null, 1L, Set.of(), Set.of(DayOfWeek.TUESDAY), 18 * 60, 20 * 60,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), MotivoBloqueo.MANTENIMIENTO, null, true);
        List<Intervalo> intervalos = b.expandir(AHORA);
        assertThat(intervalos).hasSize(4);
        assertThat(intervalos.getFirst()).isEqualTo(new Intervalo(en(LocalDate.of(2026, 11, 3), "18:00"), en(LocalDate.of(2026, 11, 3), "20:00")));
        assertThat(intervalos.getLast().inicio()).isEqualTo(en(LocalDate.of(2026, 11, 24), "18:00"));
    }

    @Test
    void sePuedenElegirVariosDiasYCerrarAMedianoche() {
        Bloqueo b = Bloqueo.recurrente(null, 1L, Set.of(), Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), 22 * 60, 24 * 60,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 15), MotivoBloqueo.OTRO, null, true);
        List<Intervalo> intervalos = b.expandir(AHORA);
        assertThat(intervalos).hasSize(4); // 7, 8, 14, 15 de noviembre
        assertThat(intervalos.getFirst().fin()).isEqualTo(ZonaHoraria.inicioDelDia(LocalDate.of(2026, 11, 8)));
    }

    @Test
    void losIntervalosQueYaPasaronSeDescartan() {
        // el lunes 2/nov a las 09:00 es "ahora": el bloqueo de las 06:00-08:00 de hoy ya pasó
        Bloqueo b = Bloqueo.recurrente(null, 1L, Set.of(), Set.of(DayOfWeek.MONDAY), 6 * 60, 8 * 60,
                LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 16), MotivoBloqueo.OTRO, null, true);
        assertThat(b.expandir(AHORA)).extracting(Intervalo::inicio)
                .containsExactly(en(LocalDate.of(2026, 11, 9), "06:00"), en(LocalDate.of(2026, 11, 16), "06:00"));
    }

    @Test
    void unBloqueoTotalmentePasadoSeRechaza() {
        Bloqueo pasado = Bloqueo.puntual(null, 1L, Set.of(), en(LocalDate.of(2026, 10, 1), "10:00"), en(LocalDate.of(2026, 10, 1), "12:00"), false,
                MotivoBloqueo.OTRO, null, true);
        assertThatThrownBy(() -> pasado.expandir(AHORA)).isInstanceOf(ExcepcionNegocio.class).hasMessageContaining("ya pasó");
    }

    @Test
    void validaLosDatosDeEntrada() {
        LocalDate dia = LocalDate.of(2026, 11, 10);
        assertThatThrownBy(() -> Bloqueo.puntual(null, 1L, Set.of(), en(dia, "18:00"), en(dia, "14:00"), false, MotivoBloqueo.OTRO, null, true))
                .isInstanceOf(ExcepcionNegocio.class);
        assertThatThrownBy(() -> Bloqueo.puntual(null, 1L, Set.of(), en(dia, "14:00"), en(dia, "18:00"), false, null, null, true))
                .isInstanceOf(ExcepcionNegocio.class);
        assertThatThrownBy(() -> Bloqueo.recurrente(null, 1L, Set.of(), Set.of(), 600, 700, dia, dia.plusDays(7), MotivoBloqueo.OTRO, null, true))
                .isInstanceOf(ExcepcionNegocio.class);
        assertThatThrownBy(() -> Bloqueo.recurrente(null, 1L, Set.of(), Set.of(DayOfWeek.MONDAY), 700, 600, dia, dia.plusDays(7), MotivoBloqueo.OTRO, null, true))
                .isInstanceOf(ExcepcionNegocio.class);
        assertThatThrownBy(() -> Bloqueo.recurrente(null, 1L, Set.of(), Set.of(DayOfWeek.MONDAY), 600, 700, dia, dia.minusDays(1), MotivoBloqueo.OTRO, null, true))
                .isInstanceOf(ExcepcionNegocio.class);
        assertThatThrownBy(() -> Bloqueo.recurrente(null, 1L, Set.of(), Set.of(DayOfWeek.MONDAY), 600, 700, dia, dia.plusDays(400), MotivoBloqueo.OTRO, null, true))
                .hasMessageContaining("un año");
    }

    @Test
    void unBloqueoDiarioDeUnAnioGeneraUnIntervaloPorDiaSinPasarseDelLimite() {
        Bloqueo b = Bloqueo.recurrente(null, 1L, Set.of(), Set.of(DayOfWeek.values()), 600, 700, LocalDate.of(2026, 11, 3),
                LocalDate.of(2027, 11, 3), MotivoBloqueo.OTRO, null, true);
        assertThat(b.expandir(AHORA)).hasSize(366).hasSizeLessThanOrEqualTo(Bloqueo.MAXIMO_INTERVALOS);
    }
}
