package co.edu.usco.goltime.reservas.aplicacion;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import co.edu.usco.goltime.reservas.dominio.Bloqueo;

/**
 * Ocupación de canchas. La base de datos garantiza con una restricción de exclusión que dos ocupaciones
 * no se solapen en la misma cancha, incluso con peticiones concurrentes.
 */
public interface RepositorioOcupaciones {

    /**
     * Ocupa las canchas indicadas durante [inicio, fin). Lanza {@code ExcepcionNegocio(RESERVA_SOLAPADA)}
     * si alguna ya está ocupada.
     */
    void ocupar(long reservaId, Collection<Long> canchaIds, Instant inicio, Instant fin);

    /**
     * Ocupa las canchas durante cada intervalo del bloqueo. Lanza {@code ExcepcionNegocio(BLOQUEO_SOLAPADO)} si
     * alguna ya está ocupada por otro bloqueo (las reservas en conflicto se cancelan antes).
     */
    void ocuparBloqueo(long bloqueoId, Collection<Long> canchaIds, List<Bloqueo.Intervalo> intervalos);

    /** Libera todas las ocupaciones del bloqueo. */
    void liberarBloqueo(long bloqueoId);

    /** Ids de reservas activas (pendientes o confirmadas) que ocupan alguna de las canchas durante algún intervalo. */
    List<Long> reservasActivasEnIntervalos(Collection<Long> canchaIds, List<Bloqueo.Intervalo> intervalos);

    /** Libera todas las ocupaciones de la reserva. */
    void liberar(long reservaId);

    /** Ocupaciones que tocan el intervalo, para pintar la disponibilidad. */
    List<Ocupacion> entre(Collection<Long> canchaIds, Instant desde, Instant hasta);

    /** ¿Alguna de las canchas está ocupada en el intervalo? {@code ignorarReservaId} excluye a una reserva (al reprogramar). */
    boolean hayOcupacion(Collection<Long> canchaIds, Instant inicio, Instant fin, Long ignorarReservaId);

    /** Ids de reservas activas y futuras que ocupan alguna de las canchas indicadas. */
    List<Long> reservasFuturasActivasEnCanchas(Collection<Long> canchaIds, Instant ahora);

    /** Ids de reservas activas y futuras que ocupan alguna cancha de la sede. */
    List<Long> reservasFuturasActivasEnSede(long sedeId, Instant ahora);
}
