package co.edu.usco.goltime.torneos.aplicacion;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import co.edu.usco.goltime.torneos.aplicacion.Modelos.EstadoInscripcion;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.Inscripcion;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.InscripcionVista;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.JugadorConDorsal;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.ResumenTorneo;
import co.edu.usco.goltime.torneos.dominio.EstadoTorneo;
import co.edu.usco.goltime.torneos.dominio.Torneo;

/** Torneos, sus canchas, inscripciones de equipos y plantillas por torneo. */
public interface RepositorioTorneos {

    Torneo guardar(Torneo torneo);

    Optional<Torneo> buscar(long id);

    /** Bloquea la fila del torneo hasta terminar la transacción (serializa inscripciones y cambios de estado). */
    Optional<Torneo> buscarBloqueando(long id);

    List<ResumenTorneo> listar(EstadoTorneo estado, boolean soloPublicos);

    Set<Long> canchas(long torneoId);

    void reemplazarCanchas(long torneoId, Set<Long> canchaIds);

    // ----- Inscripciones -----

    Inscripcion insertarInscripcion(long torneoId, long equipoId, EstadoInscripcion estado, Integer semilla);

    Optional<Inscripcion> buscarInscripcion(long id);

    Optional<Inscripcion> buscarInscripcion(long torneoId, long equipoId);

    List<InscripcionVista> inscripciones(long torneoId);

    void actualizarInscripcion(long id, EstadoInscripcion estado, Integer semilla);

    void eliminarInscripcion(long id);

    int contarAprobadas(long torneoId);

    // ----- Plantillas por torneo -----

    /** Copia la plantilla habitual del equipo a la del torneo. Lanza 409 si algún jugador ya juega en otro equipo del torneo. */
    void copiarPlantilla(long inscripcionId, long torneoId, long equipoId);

    List<JugadorConDorsal> plantillaDe(long inscripcionId);

    int contarJugadores(long inscripcionId);

    boolean jugadorEnInscripcion(long inscripcionId, long jugadorId);

    Optional<Long> inscripcionDeJugador(long torneoId, long jugadorId);

    void agregarJugador(long inscripcionId, long torneoId, long jugadorId, Integer dorsal);

    void quitarJugador(long inscripcionId, long jugadorId);

    boolean jugadorTieneEventos(long torneoId, long jugadorId);

    boolean dorsalEnUso(long inscripcionId, int dorsal, Long ignorarJugadorId);

    void actualizarDorsal(long inscripcionId, long jugadorId, Integer dorsal);

    /** Solicitud o inscripción de un equipo cuyo responsable es el usuario (para que el cliente siga sus solicitudes). */
    record SolicitudDeCliente(long inscripcionId, long torneoId, String torneoNombre, String equipoNombre, EstadoInscripcion estado, java.time.Instant creadaEn) {
    }

    List<SolicitudDeCliente> solicitudesDe(long usuarioId);

    /** Borra la plantilla del torneo de esa inscripción (al rechazarla o retirarla, para liberar a los jugadores). */
    void vaciarPlantilla(long inscripcionId);

    /** Inscripciones solicitadas del torneo, con su equipo. */
    List<InscripcionVista> solicitadas(long torneoId);

    /** Ids de usuarios responsables de los equipos inscritos (para avisarles). */
    List<Long> responsablesDeInscripciones(java.util.Collection<Long> inscripcionIds);
}
