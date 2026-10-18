package co.edu.usco.goltime.premios.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.compartido.dominio.TipoDescuento;

class CalculadoraPremiosTest {

    @Test
    void losPremiosGanadosSalenDeLasReservasCompletadasEntreLasRequeridas() {
        assertThat(CalculadoraPremios.ganados(0, 5)).isZero();
        assertThat(CalculadoraPremios.ganados(4, 5)).isZero();
        assertThat(CalculadoraPremios.ganados(5, 5)).isEqualTo(1);
        assertThat(CalculadoraPremios.ganados(14, 5)).isEqualTo(2);
        assertThat(CalculadoraPremios.ganados(-3, 5)).isZero();
    }

    @Test
    void laDiferenciaIndicaCuantosOtorgarOCuantosRevocar() {
        assertThat(CalculadoraPremios.diferencia(10, 5, 1)).isEqualTo(1); // faltó otorgar uno
        assertThat(CalculadoraPremios.diferencia(10, 5, 2)).isZero();
        assertThat(CalculadoraPremios.diferencia(9, 5, 2)).isEqualTo(-1); // una completada se corrigió
    }

    @Test
    void elProgresoDelCicloActual() {
        assertThat(CalculadoraPremios.enCiclo(7, 5)).isEqualTo(2);
        assertThat(CalculadoraPremios.faltan(7, 5)).isEqualTo(3);
        assertThat(CalculadoraPremios.enCiclo(5, 5)).isZero();
        assertThat(CalculadoraPremios.faltan(5, 5)).isEqualTo(5);
    }

    @Test
    void laReglaValidaSusDatos() {
        assertThat(new ReglaPremio(null, " Hora gratis ", null, 5, TipoDescuento.PORCENTAJE, 100, 30, true).nombre()).isEqualTo("Hora gratis");
        assertThatThrownBy(() -> new ReglaPremio(null, "x", null, 0, TipoDescuento.PORCENTAJE, 10, 30, true)).isInstanceOf(ExcepcionNegocio.class);
        assertThatThrownBy(() -> new ReglaPremio(null, "x", null, 5, TipoDescuento.PORCENTAJE, 150, 30, true)).isInstanceOf(ExcepcionNegocio.class);
        assertThatThrownBy(() -> new ReglaPremio(null, "x", null, 5, TipoDescuento.MONTO_FIJO, 10, 0, true)).isInstanceOf(ExcepcionNegocio.class);
        assertThatThrownBy(() -> new ReglaPremio(null, " ", null, 5, TipoDescuento.MONTO_FIJO, 10, 30, true)).isInstanceOf(ExcepcionNegocio.class);
    }
}
