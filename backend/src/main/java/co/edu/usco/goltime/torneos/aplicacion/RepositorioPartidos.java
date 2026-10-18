package co.edu.usco.goltime.torneos.aplicacion;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import co.edu.usco.goltime.torneos.aplicacion.Modelos.EventoPartido;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.Fase;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.Grupo;
import co.edu.usco.goltime.torneos.dominio.Partido;
import co.edu.usco.goltime.torneos.dominio.TipoFase;

/** Fases, grupos, partidos y eventos de partido de un torneo. */
public interface RepositorioPartidos {

    /** Borra fases, grupos, partidos y eventos del torneo (regenerar el fixture). */
    void eliminarFixture(long torneoId);

    Fase insertarFase(long torneoId, TipoFase tipo, int orden, String nombre);

    Grupo insertarGrupo(long faseId, String nombre, List<Long> inscripcionIds);

    Partido insertar(Partido partido);

    void guardar(Partido partido);

    Optional<Partido> buscar(long id);

    /** Igual que {@link #buscar} pero bloquea la fila (resultados simultáneos sobre el mismo cuadro). */
    Optional<Partido> buscarBloqueando(long id);

    List<Partido> deTorneo(long torneoId);

    List<Fase> fases(long torneoId);

    List<Grupo> grupos(long torneoId);

    boolean hayPartidosJugados(long torneoId);

    /** Partidos de este torneo (distintos de {@code ignorarPartidoId}) que usan a alguna de las inscripciones y se solapan con el rango. */
    boolean equipoOcupado(long torneoId, Collection<Long> inscripcionIds, Instant inicio, Instant fin, Long ignorarPartidoId);

    // ----- Eventos -----

    EventoPartido insertarEvento(EventoPartido evento);

    Optional<EventoPartido> buscarEvento(long id);

    void eliminarEvento(long id);

    List<EventoPartido> eventosDe(long partidoId);

    /** Puntos disciplinarios por inscripción (amarilla = 1, roja = 3) en el torneo. */
    Map<Long, Integer> puntosDisciplina(long torneoId);
}
