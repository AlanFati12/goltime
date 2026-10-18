package co.edu.usco.goltime.torneos.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;

class FixtureTest {

    private static List<Long> equipos(int n) {
        return IntStream.rangeClosed(1, n).mapToObj(i -> (long) i).toList();
    }

    private static ConfiguracionFixture config(int n, boolean idaYVuelta, boolean tercero) {
        return new ConfiguracionFixture(equipos(n), idaYVuelta, tercero, 0, 0);
    }

    // ----- Liga -----

    @Test
    void ligaConEquiposParesJuegaTodosContraTodosUnaVez() {
        List<PartidoPlan> partidos = new FixtureLiga().generar(config(6, false, false)).todosLosPartidos();

        assertThat(partidos).hasSize(15);
        assertThat(partidos.stream().mapToInt(PartidoPlan::ronda).max().getAsInt()).isEqualTo(5);
        assertThat(parejasSinOrden(partidos)).hasSize(15);
        assertCadaEquipoJuegaUnaVezPorJornada(partidos, 6);
    }

    @Test
    void ligaConEquiposImparesHaceDescansarUnoPorJornada() {
        List<PartidoPlan> partidos = new FixtureLiga().generar(config(5, false, false)).todosLosPartidos();

        assertThat(partidos).hasSize(10);
        assertThat(partidos.stream().mapToInt(PartidoPlan::ronda).max().getAsInt()).isEqualTo(5);
        assertThat(parejasSinOrden(partidos)).hasSize(10);
        Map<Integer, Long> porJornada = partidos.stream().collect(Collectors.groupingBy(PartidoPlan::ronda, Collectors.counting()));
        assertThat(porJornada.values()).containsOnly(2L);
        assertCadaEquipoJuegaUnaVezPorJornada(partidos, 5);
        // todos descansan exactamente una vez
        for (long e : equipos(5)) {
            assertThat(partidos.stream().filter(p -> p.local().inscripcionId() == e || p.visitante().inscripcionId() == e)).hasSize(4);
        }
    }

    @Test
    void ligaIdaYVueltaDuplicaLosPartidosInvirtiendoLaLocalia() {
        List<PartidoPlan> partidos = new FixtureLiga().generar(config(4, true, false)).todosLosPartidos();

        assertThat(partidos).hasSize(12);
        assertThat(partidos.stream().mapToInt(PartidoPlan::ronda).max().getAsInt()).isEqualTo(6);
        Set<String> dirigidos = partidos.stream().map(p -> p.local().inscripcionId() + ">" + p.visitante().inscripcionId()).collect(Collectors.toSet());
        assertThat(dirigidos).hasSize(12); // cada enfrentamiento ordenado aparece una sola vez: 4*3
    }

    @Test
    void ligaAlternaLaLocaliaDelEquipoFijo() {
        List<PartidoPlan> partidos = new FixtureLiga().generar(config(6, false, false)).todosLosPartidos();
        long comoLocal = partidos.stream().filter(p -> p.local().inscripcionId() == 1L).count();
        assertThat(comoLocal).isBetween(2L, 3L);
    }

    @Test
    void ligaNecesitaAlMenosDosEquipos() {
        assertThatThrownBy(() -> new FixtureLiga().generar(config(1, false, false))).isInstanceOf(ExcepcionNegocio.class);
    }

    // ----- Eliminación directa -----

    @Test
    void eliminacionDeOchoEquiposTieneSieteJuegosYLasSemillasSeSeparan() {
        List<PartidoPlan> p = new FixtureEliminacionDirecta().generar(config(8, false, false)).todosLosPartidos();

        assertThat(p).hasSize(7);
        assertThat(p.stream().filter(x -> x.ronda() == 1)).hasSize(4);
        PartidoPlan primero = p.get(0);
        assertThat(primero.local().inscripcionId()).isEqualTo(1L);
        assertThat(primero.visitante().inscripcionId()).isEqualTo(8L);
        assertThat(p.get(1).local().inscripcionId()).isEqualTo(4L);
        assertThat(p.get(1).visitante().inscripcionId()).isEqualTo(5L);
        assertThat(p.stream().filter(x -> x.ronda() == 3).findFirst().orElseThrow().nombreRonda()).isEqualTo("Final");
        assertThat(p.stream().filter(x -> x.ronda() == 2).map(PartidoPlan::nombreRonda).distinct()).containsExactly("Semifinal");
        assertThat(p.stream().filter(x -> x.ronda() == 1).map(PartidoPlan::nombreRonda).distinct()).containsExactly("Cuartos de final");
    }

    @Test
    void cadaPartidoApuntaAlSiguienteYElFinalNoTieneSiguiente() {
        List<PartidoPlan> p = new FixtureEliminacionDirecta().generar(config(8, false, false)).todosLosPartidos();
        Map<Integer, PartidoPlan> porClave = p.stream().collect(Collectors.toMap(PartidoPlan::clave, x -> x));

        for (PartidoPlan x : p) {
            if (x.ronda() == 3) {
                assertThat(x.siguienteClave()).isNull();
            } else {
                PartidoPlan sig = porClave.get(x.siguienteClave());
                assertThat(sig.ronda()).isEqualTo(x.ronda() + 1);
                assertThat(sig.numeroLlave()).isEqualTo((x.numeroLlave() + 1) / 2);
                assertThat(x.ladoEnSiguiente()).isEqualTo(x.numeroLlave() % 2 == 1 ? Lado.LOCAL : Lado.VISITANTE);
            }
        }
    }

    @Test
    void conCincoEquiposLasTresMejoresSemillasDescansan() {
        List<PartidoPlan> p = new FixtureEliminacionDirecta().generar(config(5, false, false)).todosLosPartidos();

        assertThat(p).hasSize(7);
        List<PartidoPlan> byes = p.stream().filter(PartidoPlan::bye).toList();
        assertThat(byes).hasSize(3);
        assertThat(byes.stream().map(x -> x.local().inscripcionId())).containsExactlyInAnyOrder(1L, 2L, 3L);
        assertThat(byes).allMatch(x -> x.visitante() == null);
        // el único partido de primera ronda real es 4 contra 5
        PartidoPlan real = p.stream().filter(x -> x.ronda() == 1 && !x.bye()).findFirst().orElseThrow();
        assertThat(Set.of(real.local().inscripcionId(), real.visitante().inscripcionId())).containsExactlyInAnyOrder(4L, 5L);
    }

    @Test
    void conTresEquiposHayUnByeYNoHayTercerPuestoAunqueSePida() {
        List<PartidoPlan> p = new FixtureEliminacionDirecta().generar(config(3, false, true)).todosLosPartidos();

        assertThat(p).hasSize(3);
        assertThat(p.stream().filter(PartidoPlan::bye)).hasSize(1);
        assertThat(p).noneMatch(x -> "Tercer puesto".equals(x.nombreRonda()));
    }

    @Test
    void tercerPuestoRecibeALosPerdedoresDeLasSemifinales() {
        List<PartidoPlan> p = new FixtureEliminacionDirecta().generar(config(4, false, true)).todosLosPartidos();

        assertThat(p).hasSize(4);
        PartidoPlan tercero = p.stream().filter(x -> "Tercer puesto".equals(x.nombreRonda())).findFirst().orElseThrow();
        List<PartidoPlan> semis = p.stream().filter(x -> "Semifinal".equals(x.nombreRonda())).toList();
        assertThat(semis).hasSize(2);
        assertThat(semis).allMatch(s -> s.siguientePerdedorClave() == tercero.clave());
        assertThat(semis.stream().map(PartidoPlan::ladoPerdedor)).containsExactly(Lado.LOCAL, Lado.VISITANTE);
        assertThat(tercero.siguienteClave()).isNull();
    }

    @Test
    void conDosEquiposSoloHayUnaFinal() {
        List<PartidoPlan> p = new FixtureEliminacionDirecta().generar(config(2, false, true)).todosLosPartidos();
        assertThat(p).hasSize(1);
        assertThat(p.get(0).nombreRonda()).isEqualTo("Final");
    }

    @Test
    void conDieciseisEquiposLasRondasTienenSuNombre() {
        List<PartidoPlan> p = new FixtureEliminacionDirecta().generar(config(16, false, false)).todosLosPartidos();
        assertThat(p).hasSize(15);
        assertThat(p.stream().filter(x -> x.ronda() == 1).findFirst().orElseThrow().nombreRonda()).isEqualTo("Octavos de final");
        assertThat(p.stream().filter(x -> x.ronda() == 4).findFirst().orElseThrow().nombreRonda()).isEqualTo("Final");
    }

    // ----- Grupos y eliminación -----

    @Test
    void gruposYEliminacionRepartenEnSerpentinaYCruzanLosClasificados() {
        ConfiguracionFixture c = new ConfiguracionFixture(equipos(8), false, false, 2, 2);
        PlanFixture plan = new FixtureGruposYEliminacion().generar(c);

        assertThat(plan.fases()).hasSize(2);
        FasePlan grupos = plan.fases().get(0);
        assertThat(grupos.tipo()).isEqualTo(TipoFase.GRUPOS);
        assertThat(grupos.grupos().get("A")).containsExactly(1L, 4L, 5L, 8L);
        assertThat(grupos.grupos().get("B")).containsExactly(2L, 3L, 6L, 7L);
        assertThat(grupos.partidos()).hasSize(12);
        assertThat(grupos.partidos().stream().map(PartidoPlan::grupo).distinct()).containsExactly("A", "B");

        FasePlan llaves = plan.fases().get(1);
        assertThat(llaves.partidos()).hasSize(3);
        PartidoPlan s1 = llaves.partidos().get(0);
        PartidoPlan s2 = llaves.partidos().get(1);
        assertThat(s1.local().referencia()).isEqualTo("G:A:1");
        assertThat(s1.visitante().referencia()).isEqualTo("G:B:2");
        assertThat(s2.local().referencia()).isEqualTo("G:B:1");
        assertThat(s2.visitante().referencia()).isEqualTo("G:A:2");
        assertThat(s1.local().etiqueta()).isEqualTo("1° Grupo A");
        assertThat(s1.local().conocido()).isFalse();
    }

    @Test
    void lasClavesDeUnPlanNoSeRepitenEntreFases() {
        PlanFixture plan = new FixtureGruposYEliminacion().generar(new ConfiguracionFixture(equipos(12), false, true, 4, 2));
        List<Integer> claves = plan.todosLosPartidos().stream().map(PartidoPlan::clave).toList();
        assertThat(new HashSet<>(claves)).hasSameSizeAs(claves);
        // 4 grupos de 3 -> 12 partidos de grupo; 8 clasificados -> 7 llaves + tercer puesto
        assertThat(plan.todosLosPartidos()).hasSize(12 + 8);
    }

    @Test
    void gruposValidaLaConfiguracion() {
        assertThatThrownBy(() -> new FixtureGruposYEliminacion().generar(new ConfiguracionFixture(equipos(8), false, false, 1, 2)))
                .isInstanceOf(ExcepcionNegocio.class);
        assertThatThrownBy(() -> new FixtureGruposYEliminacion().generar(new ConfiguracionFixture(equipos(7), false, false, 4, 2)))
                .isInstanceOf(ExcepcionNegocio.class); // 4 grupos necesitan al menos 8 equipos
    }

    @Test
    void laEstrategiaSeElijePorFormato() {
        assertThat(GeneradorFixture.para(FormatoTorneo.LIGA)).isInstanceOf(FixtureLiga.class);
        assertThat(GeneradorFixture.para(FormatoTorneo.ELIMINACION_DIRECTA)).isInstanceOf(FixtureEliminacionDirecta.class);
        assertThat(GeneradorFixture.para(FormatoTorneo.GRUPOS_Y_ELIMINACION)).isInstanceOf(FixtureGruposYEliminacion.class);
    }

    @Test
    void elOrdenadorRespetaElModo() {
        var entradas = List.of(new OrdenadorParticipantes.Entrada(1, null), new OrdenadorParticipantes.Entrada(2, 2),
                new OrdenadorParticipantes.Entrada(3, 1), new OrdenadorParticipantes.Entrada(4, null));
        assertThat(OrdenadorParticipantes.ordenar(ModoEmparejamiento.MANUAL, entradas, new java.util.Random(1))).containsExactly(1L, 2L, 3L, 4L);
        assertThat(OrdenadorParticipantes.ordenar(ModoEmparejamiento.SEMBRADO, entradas, new java.util.Random(1))).containsExactly(3L, 2L, 1L, 4L);
        assertThat(OrdenadorParticipantes.ordenar(ModoEmparejamiento.ALEATORIO, entradas, new java.util.Random(1))).containsExactlyInAnyOrder(1L, 2L, 3L, 4L);
        assertThat(OrdenadorParticipantes.ordenar(ModoEmparejamiento.ALEATORIO, entradas, new java.util.Random(7)))
                .isEqualTo(OrdenadorParticipantes.ordenar(ModoEmparejamiento.ALEATORIO, entradas, new java.util.Random(7)));
    }

    // ----- utilidades -----

    private static Set<String> parejasSinOrden(List<PartidoPlan> partidos) {
        return partidos.stream().map(p -> {
            long a = p.local().inscripcionId();
            long b = p.visitante().inscripcionId();
            return Math.min(a, b) + "-" + Math.max(a, b);
        }).collect(Collectors.toSet());
    }

    private static void assertCadaEquipoJuegaUnaVezPorJornada(List<PartidoPlan> partidos, int n) {
        Map<Integer, List<PartidoPlan>> porJornada = partidos.stream().collect(Collectors.groupingBy(PartidoPlan::ronda));
        porJornada.forEach((ronda, lista) -> {
            Set<Long> vistos = new HashSet<>();
            for (PartidoPlan p : lista) {
                assertThat(vistos.add(p.local().inscripcionId())).as("jornada %d repite local", ronda).isTrue();
                assertThat(vistos.add(p.visitante().inscripcionId())).as("jornada %d repite visitante", ronda).isTrue();
            }
        });
    }
}
