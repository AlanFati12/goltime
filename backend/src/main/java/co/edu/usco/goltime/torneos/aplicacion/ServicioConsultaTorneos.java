package co.edu.usco.goltime.torneos.aplicacion;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.sedes.aplicacion.ServicioSedes;
import co.edu.usco.goltime.torneos.aplicacion.CalculoTablas.TablaPosiciones;
import co.edu.usco.goltime.torneos.aplicacion.ConsultasTorneos.FiltroPartidos;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.EquipoPartido;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.EstadisticasTorneo;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.EstadoInscripcion;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.Fase;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.InscripcionVista;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.PerfilEquipo;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.PerfilJugador;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.ResumenTorneo;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.VistaEvento;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.VistaPartido;
import co.edu.usco.goltime.torneos.dominio.EstadoTorneo;
import co.edu.usco.goltime.torneos.dominio.Partido;
import co.edu.usco.goltime.torneos.dominio.Torneo;

/**
 * Lectura de torneos. Con {@code publico = true} (visitantes y clientes) los borradores no existen; el administrador
 * los ve todos. Las tablas, goleadores y demás estadísticas se calculan siempre desde los partidos y eventos.
 */
@Service
public class ServicioConsultaTorneos {

    private final RepositorioTorneos torneos;
    private final RepositorioPartidos partidos;
    private final ConsultasTorneos consultas;
    private final CalculoTablas tablas;
    private final ServicioSedes sedes;

    public ServicioConsultaTorneos(RepositorioTorneos torneos, RepositorioPartidos partidos, ConsultasTorneos consultas, CalculoTablas tablas,
            ServicioSedes sedes) {
        this.torneos = torneos;
        this.partidos = partidos;
        this.consultas = consultas;
        this.tablas = tablas;
        this.sedes = sedes;
    }

    public record DetalleTorneo(Torneo torneo, String sedeNombre, List<Long> canchaIds, List<InscripcionVista> inscripciones, List<Fase> fases,
            int partidosTotales, int partidosJugados, String campeon, String subcampeon, String tercero) {
    }

    @Transactional(readOnly = true)
    public List<ResumenTorneo> listar(EstadoTorneo estado, boolean publico) {
        return torneos.listar(estado, publico);
    }

    @Transactional(readOnly = true)
    public DetalleTorneo detalle(long id, boolean publico) {
        Torneo t = cargar(id, publico);
        List<InscripcionVista> inscripciones = torneos.inscripciones(id).stream()
                .filter(i -> i.inscripcion().estado() == EstadoInscripcion.APROBADA).toList();
        List<Partido> todos = partidos.deTorneo(id);
        int jugables = (int) todos.stream().filter(p -> !p.bye()).count();
        int jugados = (int) todos.stream().filter(Partido::jugado).count();
        Map<Long, String> nombres = inscripciones.stream().collect(Collectors.toMap(i -> i.inscripcion().equipoId(), InscripcionVista::equipoNombre, (a, b) -> a));
        return new DetalleTorneo(t, sedes.detalleAdmin(t.sedeId()).sede().nombre(), torneos.canchas(id).stream().sorted().toList(), inscripciones,
                partidos.fases(id), jugables, jugados, nombres.get(t.campeonId()), nombres.get(t.subcampeonId()), nombres.get(t.terceroId()));
    }

    @Transactional(readOnly = true)
    public List<TablaPosiciones> posiciones(long id, boolean publico) {
        Torneo t = cargar(id, publico);
        List<Long> inscritas = torneos.inscripciones(id).stream().filter(i -> i.inscripcion().estado() == EstadoInscripcion.APROBADA)
                .map(i -> i.inscripcion().id()).toList();
        return tablas.calcular(t, partidos.fases(id), partidos.grupos(id), partidos.deTorneo(id), partidos.puntosDisciplina(id), inscritas);
    }

    @Transactional(readOnly = true)
    public List<VistaPartido> partidos(long id, FiltroPartidos filtro, boolean publico) {
        cargar(id, publico);
        return consultas.partidos(id, filtro);
    }

    @Transactional(readOnly = true)
    public Map<Long, EquipoPartido> equipos(long id, boolean publico) {
        cargar(id, publico);
        return consultas.equiposInscritos(id);
    }

    @Transactional(readOnly = true)
    public EstadisticasTorneo estadisticas(long id, boolean publico) {
        cargar(id, publico);
        return consultas.estadisticas(id, 20);
    }

    public record DetallePartido(VistaPartido partido, List<VistaEvento> eventos) {
    }

    @Transactional(readOnly = true)
    public DetallePartido partido(long partidoId, boolean publico) {
        VistaPartido v = consultas.partido(partidoId).orElseThrow(() -> new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "El partido no existe."));
        cargar(v.torneoId(), publico);
        return new DetallePartido(v, consultas.eventos(partidoId));
    }

    @Transactional(readOnly = true)
    public PerfilEquipo equipo(long equipoId) {
        return consultas.perfilEquipo(equipoId).orElseThrow(() -> new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "El equipo no existe."));
    }

    @Transactional(readOnly = true)
    public PerfilJugador jugador(long jugadorId) {
        return consultas.perfilJugador(jugadorId).orElseThrow(() -> new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "El jugador no existe."));
    }

    @Transactional(readOnly = true)
    public List<VistaPartido> proximosPartidos(int limite) {
        return consultas.proximosPartidos(Math.min(Math.max(limite, 1), 20));
    }

    private Torneo cargar(long id, boolean publico) {
        Optional<Torneo> t = torneos.buscar(id);
        return t.filter(x -> !publico || x.estado().esPublico()).orElseThrow(() -> new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "El torneo no existe."));
    }
}
