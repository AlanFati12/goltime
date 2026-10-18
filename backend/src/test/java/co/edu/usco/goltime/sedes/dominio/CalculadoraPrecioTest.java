package co.edu.usco.goltime.sedes.dominio;

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
import co.edu.usco.goltime.sedes.dominio.DesglosePrecio.Origen;

class CalculadoraPrecioTest {

    // 2026-11-17 es martes; 2026-11-21 es sábado; 2026-11-16 es lunes festivo (Independencia de Cartagena)
    private static final LocalDate MARTES = LocalDate.of(2026, 11, 17);
    private static final LocalDate SABADO = LocalDate.of(2026, 11, 21);
    private static final LocalDate FESTIVO = LocalDate.of(2026, 11, 16);
    private static final Set<LocalDate> FESTIVOS = Set.of(FESTIVO);
    private static final Set<DayOfWeek> ENTRE_SEMANA = Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);

    private static Instant en(LocalDate fecha, String hora) {
        return ZonaHoraria.aInstante(fecha, LocalTime.parse(hora));
    }

    private static Tarifa franja(Set<DayOfWeek> dias, String desde, String hasta, int precio, boolean festivo) {
        return new Tarifa(null, 1L, dias, MinutoDelDia.analizar(desde), MinutoDelDia.analizar(hasta), festivo, precio, null, null);
    }

    private final List<Tarifa> tarifas = List.of(
            franja(ENTRE_SEMANA, "06:00", "17:00", 70_000, false),
            franja(ENTRE_SEMANA, "17:00", "23:00", 100_000, false),
            franja(Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), "06:00", "23:00", 110_000, false),
            franja(Set.of(), "06:00", "23:00", 130_000, true));

    @Test
    void sinTarifasUsaElPrecioBaseProporcionalAlTiempo() {
        DesglosePrecio una = CalculadoraPrecio.calcular(80_000, List.of(), FESTIVOS, en(MARTES, "19:00"), en(MARTES, "20:00"));
        DesglosePrecio hora_y_media = CalculadoraPrecio.calcular(80_000, List.of(), FESTIVOS, en(MARTES, "19:00"), en(MARTES, "20:30"));
        assertThat(una.total()).isEqualTo(80_000);
        assertThat(hora_y_media.total()).isEqualTo(120_000);
        assertThat(una.tramos()).singleElement().satisfies(t -> assertThat(t.origen()).isEqualTo(Origen.BASE));
    }

    @Test
    void aplicaLaFranjaDelDiaYLaHora() {
        assertThat(CalculadoraPrecio.calcular(80_000, tarifas, FESTIVOS, en(MARTES, "10:00"), en(MARTES, "11:00")).total()).isEqualTo(70_000);
        assertThat(CalculadoraPrecio.calcular(80_000, tarifas, FESTIVOS, en(MARTES, "19:00"), en(MARTES, "20:00")).total()).isEqualTo(100_000);
        assertThat(CalculadoraPrecio.calcular(80_000, tarifas, FESTIVOS, en(SABADO, "19:00"), en(SABADO, "20:00")).total()).isEqualTo(110_000);
    }

    @Test
    void siCruzaDosFranjasSeCobraProporcionalmenteCadaTramo() {
        // 16:30-17:30 = 30 min a 70.000 + 30 min a 100.000 = 35.000 + 50.000
        DesglosePrecio d = CalculadoraPrecio.calcular(80_000, tarifas, FESTIVOS, en(MARTES, "16:30"), en(MARTES, "17:30"));
        assertThat(d.total()).isEqualTo(85_000);
        assertThat(d.tramos()).hasSize(2);
        assertThat(d.tramos().get(0).minutos()).isEqualTo(30);
        assertThat(d.tramos().get(0).precioHora()).isEqualTo(70_000);
        assertThat(d.tramos().get(1).precioHora()).isEqualTo(100_000);
    }

    @Test
    void unTramoSinFranjaUsaElPrecioBase() {
        // 05:30-06:30: 30 min sin franja (base 80.000) + 30 min a 70.000
        DesglosePrecio d = CalculadoraPrecio.calcular(80_000, tarifas, FESTIVOS, en(MARTES, "05:30"), en(MARTES, "06:30"));
        assertThat(d.total()).isEqualTo(75_000);
        assertThat(d.tramos().get(0).origen()).isEqualTo(Origen.BASE);
        assertThat(d.tramos().get(1).origen()).isEqualTo(Origen.TARIFA);
    }

    @Test
    void enFestivoRigeLaFranjaDeFestivoPorEncimaDeLaDelDia() {
        // El 16/nov es lunes: sin festivo costaría 100.000 a las 19:00
        DesglosePrecio d = CalculadoraPrecio.calcular(80_000, tarifas, FESTIVOS, en(FESTIVO, "19:00"), en(FESTIVO, "20:00"));
        assertThat(d.total()).isEqualTo(130_000);
        assertThat(d.tramos().getFirst().origen()).isEqualTo(Origen.FESTIVO);
    }

    @Test
    void enFestivoSinFranjaDeFestivoCaeALaFranjaNormalDelDia() {
        List<Tarifa> sinFestivo = tarifas.subList(0, 3);
        assertThat(CalculadoraPrecio.calcular(80_000, sinFestivo, FESTIVOS, en(FESTIVO, "19:00"), en(FESTIVO, "20:00")).total()).isEqualTo(100_000);
    }

    @Test
    void siCruzaLaMedianocheCambiaDeDia() {
        // martes 23:00 a miércoles 00:30: 60 min a precio base (la franja termina a las 23:00) + 30 min base
        DesglosePrecio d = CalculadoraPrecio.calcular(80_000, tarifas, FESTIVOS, en(MARTES, "23:00"), en(MARTES.plusDays(1), "00:30"));
        assertThat(d.total()).isEqualTo(120_000);
        assertThat(d.tramos()).as("los tramos contiguos con igual precio se fusionan").hasSize(1);
        assertThat(d.tramos().getFirst().minutos()).isEqualTo(90);
    }

    @Test
    void respetaLaVigenciaDeLaFranja() {
        Tarifa temporal = new Tarifa(null, 1L, ENTRE_SEMANA, 0, 1440, false, 50_000, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 31));
        assertThat(CalculadoraPrecio.calcular(80_000, List.of(temporal), FESTIVOS, en(MARTES, "10:00"), en(MARTES, "11:00")).total()).isEqualTo(80_000);
        LocalDate dic = LocalDate.of(2026, 12, 15); // martes
        assertThat(CalculadoraPrecio.calcular(80_000, List.of(temporal), FESTIVOS, en(dic, "10:00"), en(dic, "11:00")).total()).isEqualTo(50_000);
    }

    @Test
    void redondeaAlPesoMasCercano() {
        // 20 minutos a 100.000/h = 33.333,33 -> 33.333
        assertThat(CalculadoraPrecio.calcular(100_000, List.of(), FESTIVOS, en(MARTES, "10:00"), en(MARTES, "10:20")).total()).isEqualTo(33_333);
    }

    @Test
    void rechazaIntervalosInvalidos() {
        assertThatThrownBy(() -> CalculadoraPrecio.calcular(80_000, tarifas, FESTIVOS, en(MARTES, "10:00"), en(MARTES, "10:00")))
                .isInstanceOf(ExcepcionNegocio.class);
        assertThatThrownBy(() -> CalculadoraPrecio.calcular(80_000, tarifas, FESTIVOS, en(MARTES, "11:00"), en(MARTES, "10:00")))
                .isInstanceOf(ExcepcionNegocio.class);
    }
}
