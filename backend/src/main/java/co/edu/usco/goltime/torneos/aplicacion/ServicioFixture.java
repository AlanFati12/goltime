package co.edu.usco.goltime.torneos.aplicacion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.usco.goltime.auditoria.aplicacion.RegistroAuditoria;
import co.edu.usco.goltime.compartido.dominio.Actor;
import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.reservas.aplicacion.ServicioReservasTorneo;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.EstadoInscripcion;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.Fase;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.Grupo;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.InscripcionVista;
import co.edu.usco.goltime.torneos.dominio.ConfiguracionFixture;
import co.edu.usco.goltime.torneos.dominio.EstadoTorneo;
import co.edu.usco.goltime.torneos.dominio.FasePlan;
import co.edu.usco.goltime.torneos.dominio.GeneradorFixture;
import co.edu.usco.goltime.torneos.dominio.ModoEmparejamiento;
import co.edu.usco.goltime.torneos.dominio.OrdenadorParticipantes;
import co.edu.usco.goltime.torneos.dominio.Partido;
import co.edu.usco.goltime.torneos.dominio.PartidoPlan;
import co.edu.usco.goltime.torneos.dominio.PlanFixture;
import co.edu.usco.goltime.torneos.dominio.Torneo;

/**
 * Genera (y regenera) los partidos de un torneo según su formato. Mientras no se haya jugado ningún partido el fixture
 * se puede volver a generar; al hacerlo se liberan las canchas que tenían reservadas los partidos descartados.
 */
@Service
public class ServicioFixture {

    private static final String ENTIDAD = "torneos";

    private final RepositorioTorneos torneos;
    private final RepositorioPartidos partidos;
    private final AvanceDeLlaves avance;
    private final ServicioReservasTorneo reservasTorneo;
    private final RegistroAuditoria auditoria;

    public ServicioFixture(RepositorioTorneos torneos, RepositorioPartidos partidos, AvanceDeLlaves avance, ServicioReservasTorneo reservasTorneo,
            RegistroAuditoria auditoria) {
        this.torneos = torneos;
        this.partidos = partidos;
        this.avance = avance;
        this.reservasTorneo = reservasTorneo;
        this.auditoria = auditoria;
    }

    public record ResumenFixture(int fases, int grupos, int partidos, int descansos) {
    }

    /**
     * @param ordenManual para el modo MANUAL, ids de inscripción en el orden deseado (de la mejor a la peor semilla, o
     *                    grupo A primero con serpentina); si falta se usa el orden de inscripción
     */
    @Transactional
    public ResumenFixture generar(Actor admin, long torneoId, List<Long> ordenManual) {
        Torneo torneo = torneos.buscarBloqueando(torneoId)
                .orElseThrow(() -> new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "El torneo no existe."));
        if (torneo.estado() == EstadoTorneo.FINALIZADO || torneo.estado() == EstadoTorneo.CANCELADO) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "El torneo ya terminó: no se puede generar el fixture.");
        }
        if (partidos.hayPartidosJugados(torneoId)) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "Ya se jugaron partidos: el fixture ya no se puede regenerar.");
        }
        List<InscripcionVista> aprobadas = torneos.inscripciones(torneoId).stream()
                .filter(i -> i.inscripcion().estado() == EstadoInscripcion.APROBADA).toList();
        GeneradorFixture.exigirEquipos(aprobadas.size(), 2);
        exigirPlantillasCompletas(torneo, aprobadas);

        List<Long> orden = ordenar(torneo, aprobadas, ordenManual);
        PlanFixture plan = GeneradorFixture.para(torneo.formato())
                .generar(new ConfiguracionFixture(orden, torneo.idaYVuelta(), torneo.tercerPuesto(), valor(torneo.numGrupos()), valor(torneo.clasificadosPorGrupo())));

        descartarFixtureActual(admin, torneoId);
        ResumenFixture resumen = guardar(torneo, plan);
        auditoria.registrar("GENERAR_FIXTURE", ENTIDAD, torneoId, Map.of("formato", torneo.formato().name(), "equipos", aprobadas.size(),
                "partidos", resumen.partidos(), "modo", torneo.modoEmparejamiento().name()), null);
        return resumen;
    }

    /** Descarta el fixture sin generar otro. Solo si todavía no se jugó ningún partido. */
    @Transactional
    public void descartar(Actor admin, long torneoId) {
        Torneo torneo = torneos.buscarBloqueando(torneoId)
                .orElseThrow(() -> new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "El torneo no existe."));
        if (torneo.estado() == EstadoTorneo.FINALIZADO || torneo.estado() == EstadoTorneo.CANCELADO || partidos.hayPartidosJugados(torneoId)) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "Ya se jugaron partidos o el torneo terminó: el fixture no se puede descartar.");
        }
        descartarFixtureActual(admin, torneoId);
        auditoria.registrar("DESCARTAR_FIXTURE", ENTIDAD, torneoId, Map.of(), null);
    }

    /** Libera las reservas de los partidos que seguían programados y borra el fixture. */
    @Transactional
    public void descartarFixtureActual(Actor admin, long torneoId) {
        liberarProgramaciones(admin, torneoId, "Fixture del torneo regenerado");
        partidos.eliminarFixture(torneoId);
    }

    /** Cancela la reserva de cada partido pendiente de jugar (el partido queda sin fecha ni cancha). */
    @Transactional
    public void liberarProgramaciones(Actor admin, long torneoId, String motivo) {
        for (Partido p : partidos.deTorneo(torneoId)) {
            if (p.reservaId() != null && !p.jugado()) {
                reservasTorneo.liberar(admin, p.reservaId(), motivo);
                p.desprogramar(co.edu.usco.goltime.torneos.dominio.EstadoPartido.PENDIENTE);
                partidos.guardar(p);
            }
        }
    }

    // ----- Interno -----

    private ResumenFixture guardar(Torneo torneo, PlanFixture plan) {
        Map<Integer, Partido> porClave = new LinkedHashMap<>();
        Map<Integer, PartidoPlan> planes = new HashMap<>();
        int grupos = 0;
        for (FasePlan fp : plan.fases()) {
            Fase fase = partidos.insertarFase(torneo.id(), fp.tipo(), fp.orden(), fp.nombre());
            Map<String, Grupo> gruposGuardados = new HashMap<>();
            for (Map.Entry<String, List<Long>> g : fp.grupos().entrySet()) {
                gruposGuardados.put(g.getKey(), partidos.insertarGrupo(fase.id(), g.getKey(), g.getValue()));
                grupos++;
            }
            for (PartidoPlan pp : fp.partidos()) {
                Grupo g = pp.grupo() == null ? null : gruposGuardados.get(pp.grupo());
                Partido nuevo = Partido.desdePlan(torneo.id(), fase.id(), fp.tipo(), g == null ? null : g.id(), g == null ? null : g.nombre(), pp);
                porClave.put(pp.clave(), partidos.insertar(nuevo));
                planes.put(pp.clave(), pp);
            }
        }
        // enlaces del cuadro: ya con ids reales
        for (Map.Entry<Integer, Partido> e : porClave.entrySet()) {
            PartidoPlan pp = planes.get(e.getKey());
            if (pp.siguienteClave() != null || pp.siguientePerdedorClave() != null) {
                e.getValue().enlazar(pp.siguienteClave() == null ? null : porClave.get(pp.siguienteClave()).id(),
                        pp.siguientePerdedorClave() == null ? null : porClave.get(pp.siguientePerdedorClave()).id());
                partidos.guardar(e.getValue());
            }
        }
        avance.avanzarDescansos(torneo.id());
        int descansos = (int) porClave.values().stream().filter(Partido::bye).count();
        return new ResumenFixture(plan.fases().size(), grupos, porClave.size(), descansos);
    }

    private List<Long> ordenar(Torneo torneo, List<InscripcionVista> aprobadas, List<Long> ordenManual) {
        List<OrdenadorParticipantes.Entrada> entradas = new ArrayList<>();
        Set<Long> ids = new HashSet<>();
        for (InscripcionVista i : aprobadas) {
            entradas.add(new OrdenadorParticipantes.Entrada(i.inscripcion().id(), i.inscripcion().semilla()));
            ids.add(i.inscripcion().id());
        }
        if (torneo.modoEmparejamiento() == ModoEmparejamiento.MANUAL && ordenManual != null && !ordenManual.isEmpty()) {
            if (ordenManual.size() != ids.size() || !ids.equals(new HashSet<>(ordenManual))) {
                throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "El orden manual debe incluir exactamente los equipos inscritos, una vez cada uno.");
            }
            return List.copyOf(ordenManual);
        }
        return OrdenadorParticipantes.ordenar(torneo.modoEmparejamiento(), entradas, new Random());
    }

    private void exigirPlantillasCompletas(Torneo torneo, List<InscripcionVista> aprobadas) {
        List<String> incompletos = aprobadas.stream().filter(i -> i.jugadores() < torneo.jugadoresPorEquipo())
                .map(i -> i.equipoNombre() + " (" + i.jugadores() + "/" + torneo.jugadoresPorEquipo() + ")").toList();
        if (!incompletos.isEmpty()) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS,
                    "Estos equipos no tienen los jugadores mínimos para el torneo: " + String.join(", ", incompletos) + ".");
        }
    }

    private static int valor(Integer n) {
        return n == null ? 0 : n;
    }
}
