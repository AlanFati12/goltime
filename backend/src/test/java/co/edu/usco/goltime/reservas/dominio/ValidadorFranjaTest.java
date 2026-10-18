package co.edu.usco.goltime.reservas.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.compartido.dominio.ZonaHoraria;
import co.edu.usco.goltime.reservas.dominio.ValidadorFranja.ReglasCancha;
import co.edu.usco.goltime.sedes.dominio.HorarioDia;

class ValidadorFranjaTest {

    private static final Instant AHORA = ZonaHoraria.aInstante(LocalDate.of(2026, 11, 10), LocalTime.of(9, 0));
    private static final LocalDate DIA = LocalDate.of(2026, 11, 14);
    private static final HorarioDia HORARIO = HorarioDia.abierto(6, "08:00", "23:00");
    private static final ReglasCancha CANCHA = new ReglasCancha(60, 60, 120);
    private static final PoliticaReservas POLITICA = PoliticaReservas.porDefecto();

    private static Instant en(LocalDate fecha, String hora) {
        return ZonaHoraria.aInstante(fecha, LocalTime.parse(hora));
    }

    private static void validar(Instant inicio, Instant fin) {
        ValidadorFranja.validar(HORARIO, CANCHA, inicio, fin, AHORA, POLITICA);
    }

    private static CodigoError codigo(Runnable accion) {
        try {
            accion.run();
        } catch (ExcepcionNegocio e) {
            return e.codigo();
        }
        return null;
    }

    @Test
    void aceptaUnaFranjaValida() {
        assertThatCode(() -> validar(en(DIA, "19:00"), en(DIA, "20:00"))).doesNotThrowAnyException();
        assertThatCode(() -> validar(en(DIA, "19:00"), en(DIA, "21:00"))).doesNotThrowAnyException();
    }

    @Test
    void rechazaFueraDelHorarioDeOperacion() {
        assertThat(codigo(() -> validar(en(DIA, "07:00"), en(DIA, "08:00")))).isEqualTo(CodigoError.FUERA_DE_HORARIO);
        assertThat(codigo(() -> validar(en(DIA, "22:00"), en(DIA, "24:00".replace("24:00", "23:59"))))).isNotNull();
        assertThat(codigo(() -> ValidadorFranja.validar(HorarioDia.cerradoEl(6), CANCHA, en(DIA, "19:00"), en(DIA, "20:00"), AHORA, POLITICA)))
                .isEqualTo(CodigoError.FUERA_DE_HORARIO);
    }

    @Test
    void permiteTerminarJustoAlCierreYNoPasarse() {
        assertThatCode(() -> validar(en(DIA, "22:00"), en(DIA, "23:00"))).doesNotThrowAnyException();
        assertThat(codigo(() -> validar(en(DIA, "22:30"), en(DIA, "23:30")))).isEqualTo(CodigoError.FUERA_DE_HORARIO);
    }

    @Test
    void cierreAMedianocheSePuedeReservar() {
        HorarioDia hastaMedianoche = HorarioDia.abierto(6, "08:00", "24:00");
        assertThatCode(() -> ValidadorFranja.validar(hastaMedianoche, CANCHA, en(DIA, "23:00"), en(DIA.plusDays(1), "00:00"), AHORA, POLITICA))
                .doesNotThrowAnyException();
        assertThat(codigo(() -> ValidadorFranja.validar(hastaMedianoche, CANCHA, en(DIA, "23:00"), en(DIA.plusDays(1), "01:00"), AHORA, POLITICA)))
                .isEqualTo(CodigoError.FUERA_DE_HORARIO);
    }

    @Test
    void exigeAlinearseAlBloqueDesdeLaApertura() {
        assertThat(codigo(() -> validar(en(DIA, "19:30"), en(DIA, "20:30")))).isEqualTo(CodigoError.DATOS_INVALIDOS);
        ReglasCancha mediaHora = new ReglasCancha(30, 60, 120);
        assertThatCode(() -> ValidadorFranja.validar(HORARIO, mediaHora, en(DIA, "19:30"), en(DIA, "20:30"), AHORA, POLITICA)).doesNotThrowAnyException();
    }

    @Test
    void validaLaDuracion() {
        assertThat(codigo(() -> validar(en(DIA, "19:00"), en(DIA, "19:30")))).isEqualTo(CodigoError.DATOS_INVALIDOS);
        assertThat(codigo(() -> validar(en(DIA, "19:00"), en(DIA, "22:00")))).isEqualTo(CodigoError.DATOS_INVALIDOS);
        assertThat(codigo(() -> validar(en(DIA, "19:00"), en(DIA, "19:00")))).isEqualTo(CodigoError.DATOS_INVALIDOS);
    }

    @Test
    void respetaLaVentanaDeAntelacion() {
        // AHORA es martes 10/nov 09:00; la antelación mínima es de 3 h
        LocalDate hoy = LocalDate.of(2026, 11, 10);
        assertThat(codigo(() -> validar(en(hoy, "10:00"), en(hoy, "11:00")))).isEqualTo(CodigoError.FUERA_DE_VENTANA);
        assertThatCode(() -> validar(en(hoy, "12:00"), en(hoy, "13:00"))).doesNotThrowAnyException();
        // máximo 30 días
        LocalDate lejano = hoy.plusDays(31);
        HorarioDia abierto = HorarioDia.abierto(lejano.getDayOfWeek().getValue(), "08:00", "23:00");
        assertThat(codigo(() -> ValidadorFranja.validar(abierto, CANCHA, en(lejano, "19:00"), en(lejano, "20:00"), AHORA, POLITICA)))
                .isEqualTo(CodigoError.FUERA_DE_VENTANA);
    }
}
