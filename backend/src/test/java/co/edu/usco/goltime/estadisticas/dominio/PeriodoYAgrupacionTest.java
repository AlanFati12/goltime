package co.edu.usco.goltime.estadisticas.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;

class PeriodoYAgrupacionTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 11, 2);

    @Test
    void elPeriodoAnteriorTieneLaMismaDuracionYTerminaElDiaPrevio() {
        PeriodoEstadistico p = new PeriodoEstadistico(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 16));

        assertThat(p.dias()).isEqualTo(7);
        PeriodoEstadistico anterior = p.anterior();
        assertThat(anterior.desde()).isEqualTo(LocalDate.of(2026, 11, 3));
        assertThat(anterior.hasta()).isEqualTo(LocalDate.of(2026, 11, 9));
        assertThat(anterior.dias()).isEqualTo(7);
    }

    @Test
    void elPeriodoVaDeMedianocheABogotaHastaLaMedianocheDelDiaSiguiente() {
        PeriodoEstadistico p = new PeriodoEstadistico(LUNES, LUNES);

        // 00:00 en Bogotá (UTC-5) = 05:00 UTC
        assertThat(p.inicio().atOffset(ZoneOffset.UTC).getHour()).isEqualTo(5);
        assertThat(p.fin()).isEqualTo(p.inicio().plusSeconds(24 * 3600));
    }

    @Test
    void rechazaPeriodosInvalidos() {
        assertThatThrownBy(() -> new PeriodoEstadistico(LUNES, LUNES.minusDays(1))).isInstanceOf(ExcepcionNegocio.class);
        assertThatThrownBy(() -> new PeriodoEstadistico(null, LUNES)).isInstanceOf(ExcepcionNegocio.class);
        assertThatThrownBy(() -> new PeriodoEstadistico(LUNES, LUNES.plusDays(366))).isInstanceOf(ExcepcionNegocio.class);
        assertThat(new PeriodoEstadistico(LUNES, LUNES.plusDays(365)).dias()).isEqualTo(366);
    }

    @Test
    void laVariacionNoSeCalculaSinBaseDeComparacion() {
        assertThat(PeriodoEstadistico.variacion(150, 100)).isEqualTo(50.0);
        assertThat(PeriodoEstadistico.variacion(50, 100)).isEqualTo(-50.0);
        assertThat(PeriodoEstadistico.variacion(10, 0)).isNull();
        assertThat(PeriodoEstadistico.variacion(1, 3)).isEqualTo(-66.7);
    }

    @Test
    void lasCubetasCubrenElPeriodoCompletoIncluidosLosExtremos() {
        PeriodoEstadistico p = new PeriodoEstadistico(LocalDate.of(2026, 11, 4), LocalDate.of(2026, 11, 18)); // miércoles a miércoles

        assertThat(Agrupacion.DIA.cubetas(p)).hasSize(15);
        // semanas que empiezan lunes: 2, 9 y 16 de noviembre
        assertThat(Agrupacion.SEMANA.cubetas(p)).containsExactly(LUNES, LUNES.plusDays(7), LUNES.plusDays(14));
        assertThat(Agrupacion.MES.cubetas(p)).containsExactly(LocalDate.of(2026, 11, 1));
        assertThat(Agrupacion.MES.cubetas(new PeriodoEstadistico(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 12, 2))))
                .containsExactly(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 1));
    }

    @Test
    void elInicioDeLaCubetaSigueLaConvencionDePostgreSql() {
        assertThat(Agrupacion.SEMANA.inicioDeCubeta(LocalDate.of(2026, 11, 8))).isEqualTo(LUNES); // domingo
        assertThat(Agrupacion.SEMANA.inicioDeCubeta(LUNES)).isEqualTo(LUNES);
        assertThat(Agrupacion.MES.inicioDeCubeta(LocalDate.of(2026, 11, 30))).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(Agrupacion.DIA.truncado()).isEqualTo("day");
        assertThat(Agrupacion.SEMANA.truncado()).isEqualTo("week");
    }
}
