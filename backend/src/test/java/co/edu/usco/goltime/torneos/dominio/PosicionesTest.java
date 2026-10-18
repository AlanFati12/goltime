package co.edu.usco.goltime.torneos.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.torneos.dominio.CalculadoraPosiciones.FilaPosicion;
import co.edu.usco.goltime.torneos.dominio.CalculadoraPosiciones.PartidoJugado;

class PosicionesTest {

    private static final ReglasPuntuacion REGLAS = ReglasPuntuacion.porDefecto();

    private static PartidoJugado p(long l, long v, int gl, int gv) {
        return new PartidoJugado(l, v, gl, gv);
    }

    private static List<Long> orden(List<FilaPosicion> filas) {
        return filas.stream().map(FilaPosicion::inscripcionId).toList();
    }

    @Test
    void sumaPuntosGolesYPartidosConLaPuntuacionPorDefecto() {
        List<FilaPosicion> t = CalculadoraPosiciones.calcular(List.of(1L, 2L, 3L), List.of(p(1, 2, 3, 1), p(2, 3, 0, 0), p(3, 1, 0, 2)), Map.of(), REGLAS);

        FilaPosicion uno = t.get(0);
        assertThat(uno.inscripcionId()).isEqualTo(1L);
        assertThat(uno.puntos()).isEqualTo(6);
        assertThat(uno.jugados()).isEqualTo(2);
        assertThat(uno.ganados()).isEqualTo(2);
        assertThat(uno.golesFavor()).isEqualTo(5);
        assertThat(uno.golesContra()).isEqualTo(1);
        assertThat(uno.diferencia()).isEqualTo(4);
        assertThat(orden(t)).containsExactly(1L, 2L, 3L); // 2 y 3 igualan en puntos y diferencia: decide el gol a favor
        assertThat(t.get(1).puntos()).isEqualTo(1);
        assertThat(t.get(1).empatados()).isEqualTo(1);
        assertThat(t.get(2).puntos()).isEqualTo(1);
        assertThat(t.get(2).perdidos()).isEqualTo(1);
        assertThat(t).extracting(FilaPosicion::posicion).containsExactly(1, 2, 3);
    }

    @Test
    void usaLosPuntosConfigurados() {
        ReglasPuntuacion reglas = new ReglasPuntuacion(2, 1, 0, null, 3);
        List<FilaPosicion> t = CalculadoraPosiciones.calcular(List.of(1L, 2L), List.of(p(1, 2, 1, 0)), Map.of(), reglas);
        assertThat(t.get(0).puntos()).isEqualTo(2);
    }

    @Test
    void incluyeALosEquiposQueNoHanJugado() {
        List<FilaPosicion> t = CalculadoraPosiciones.calcular(List.of(1L, 2L, 3L), List.of(p(1, 2, 1, 0)), Map.of(), REGLAS);
        assertThat(t).hasSize(3);
        assertThat(t.stream().filter(f -> f.inscripcionId() == 3L).findFirst().orElseThrow().jugados()).isZero();
    }

    @Test
    void empatadosEnPuntosDecideLaDiferenciaDeGol() {
        // 1 y 2 ganan un partido cada uno; 1 con mejor diferencia
        List<FilaPosicion> t = CalculadoraPosiciones.calcular(List.of(1L, 2L, 3L, 4L), List.of(p(1, 3, 4, 0), p(2, 4, 1, 0)), Map.of(), REGLAS);
        assertThat(orden(t).subList(0, 2)).containsExactly(1L, 2L);
    }

    @Test
    void siSigueIgualDecideGolesAFavor() {
        // 1: +1 con 3 goles a favor; 2: +1 con 1 gol a favor
        List<FilaPosicion> t = CalculadoraPosiciones.calcular(List.of(1L, 2L, 3L, 4L), List.of(p(1, 3, 3, 2), p(2, 4, 1, 0)), Map.of(), REGLAS);
        assertThat(orden(t).subList(0, 2)).containsExactly(1L, 2L);
    }

    @Test
    void elEnfrentamientoDirectoPasaAntesQueLaDiferenciaSoloSiAsiSeConfigura() {
        // 1 y 2 terminan con 6 puntos; 1 le ganó a 2, pero 2 tiene mucha mejor diferencia de gol
        List<PartidoJugado> partidos = List.of(p(1, 2, 1, 0), p(1, 3, 0, 1), p(1, 4, 1, 0), p(2, 3, 3, 0), p(2, 4, 3, 0), p(3, 4, 0, 0));

        List<FilaPosicion> porDefecto = CalculadoraPosiciones.calcular(List.of(1L, 2L, 3L, 4L), partidos, Map.of(), REGLAS);
        assertThat(porDefecto.get(0).inscripcionId()).isEqualTo(2L); // diferencia antes que el directo
        assertThat(porDefecto.get(0).puntos()).isEqualTo(6);
        assertThat(porDefecto.get(1).puntos()).isEqualTo(6);

        ReglasPuntuacion directoPrimero = new ReglasPuntuacion(3, 1, 0, List.of(CriterioDesempate.PUNTOS, CriterioDesempate.ENFRENTAMIENTO_DIRECTO), 3);
        List<FilaPosicion> conDirecto = CalculadoraPosiciones.calcular(List.of(1L, 2L, 3L, 4L), partidos, Map.of(), directoPrimero);
        assertThat(orden(conDirecto).subList(0, 2)).containsExactly(1L, 2L);
        assertThat(orden(conDirecto).subList(2, 4)).containsExactly(3L, 4L);
    }

    @Test
    void enUnTripleEmpateCircularElDirectoSeCalculaSoloEntreLosIgualados() {
        // 1 gana a 2, 2 gana a 3, 3 gana a 1: en la mini-tabla entre ellos decide la diferencia de gol (2: +2, 1: 0, 3: -2)
        ReglasPuntuacion r = new ReglasPuntuacion(3, 1, 0,
                List.of(CriterioDesempate.PUNTOS, CriterioDesempate.ENFRENTAMIENTO_DIRECTO, CriterioDesempate.DIFERENCIA_GOL), 3);
        List<FilaPosicion> t = CalculadoraPosiciones.calcular(List.of(1L, 2L, 3L), List.of(p(1, 2, 1, 0), p(2, 3, 3, 0), p(3, 1, 1, 0)), Map.of(), r);
        assertThat(t).extracting(FilaPosicion::puntos).containsOnly(3);
        assertThat(orden(t)).containsExactly(2L, 1L, 3L);
    }

    @Test
    void juegoLimpioDesempataConMenosTarjetas() {
        ReglasPuntuacion r = new ReglasPuntuacion(3, 1, 0, List.of(CriterioDesempate.PUNTOS, CriterioDesempate.JUEGO_LIMPIO), 3);
        List<FilaPosicion> t = CalculadoraPosiciones.calcular(List.of(1L, 2L), List.of(p(1, 2, 1, 1)), Map.of(1L, 4, 2L, 1), r);
        assertThat(orden(t)).containsExactly(2L, 1L);
        assertThat(t.get(1).puntosDisciplina()).isEqualTo(4);
    }

    @Test
    void elSorteoFinalEsDeterminista() {
        List<Long> equipos = List.of(1L, 2L, 3L, 4L);
        List<FilaPosicion> a = CalculadoraPosiciones.calcular(equipos, List.of(), Map.of(), REGLAS);
        List<FilaPosicion> b = CalculadoraPosiciones.calcular(equipos, List.of(), Map.of(), REGLAS);
        assertThat(orden(a)).isEqualTo(orden(b));
        assertThat(orden(a)).containsExactlyInAnyOrderElementsOf(equipos);
    }

    @Test
    void laFormaMuestraLosUltimosCincoResultados() {
        List<PartidoJugado> partidos = List.of(p(1, 2, 1, 0), p(1, 2, 0, 0), p(1, 2, 0, 1), p(1, 2, 2, 0), p(1, 2, 1, 0), p(1, 2, 3, 0), p(1, 2, 0, 2));
        FilaPosicion uno = CalculadoraPosiciones.calcular(List.of(1L, 2L), partidos, Map.of(), REGLAS).stream()
                .filter(f -> f.inscripcionId() == 1L).findFirst().orElseThrow();
        assertThat(uno.forma()).containsExactly("P", "G", "G", "G", "P");
    }

    @Test
    void lasReglasExigenPuntosEnLosCriteriosYPuntuacionCoherente() {
        assertThatThrownBy(() -> new ReglasPuntuacion(3, 1, 0, List.of(CriterioDesempate.DIFERENCIA_GOL), 3)).isInstanceOf(ExcepcionNegocio.class);
        assertThatThrownBy(() -> new ReglasPuntuacion(1, 3, 0, null, 3)).isInstanceOf(ExcepcionNegocio.class);
        assertThatThrownBy(() -> new ReglasPuntuacion(3, 1, 0, null, 0)).isInstanceOf(ExcepcionNegocio.class);
    }

    // ----- Resolución de partidos -----

    @Test
    void elGanadorSaleDelMarcadorOPenales() {
        assertThat(ResolucionPartido.ganador(2, 1, null, null, true)).isEqualTo(Lado.LOCAL);
        assertThat(ResolucionPartido.ganador(0, 3, null, null, false)).isEqualTo(Lado.VISITANTE);
        assertThat(ResolucionPartido.ganador(1, 1, null, null, false)).isNull();
        assertThat(ResolucionPartido.ganador(1, 1, 3, 4, true)).isEqualTo(Lado.VISITANTE);
    }

    @Test
    void enEliminatoriaElEmpateExigePenalesSinEmpate() {
        assertThatThrownBy(() -> ResolucionPartido.ganador(1, 1, null, null, true)).isInstanceOf(ExcepcionNegocio.class);
        assertThatThrownBy(() -> ResolucionPartido.ganador(1, 1, 3, 3, true)).isInstanceOf(ExcepcionNegocio.class);
        assertThatThrownBy(() -> ResolucionPartido.ganador(2, 1, 3, 1, true)).isInstanceOf(ExcepcionNegocio.class);
        assertThatThrownBy(() -> ResolucionPartido.ganador(1, 1, 3, 1, false)).isInstanceOf(ExcepcionNegocio.class);
    }
}
