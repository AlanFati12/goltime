package co.edu.usco.goltime.reservas.aplicacion;

import java.time.Instant;
import java.util.List;

public interface RepositorioAgenda {

    /** Ocupaciones (reservas y bloqueos) de las canchas de la sede que tocan el rango [desde, hasta). */
    List<ElementoAgenda> elementos(long sedeId, Instant desde, Instant hasta);

    /**
     * Un cuadro de la agenda en la columna de una cancha. Una reserva de una cancha compuesta aparece también
     * en sus componentes, con {@code canchaReservaNombre} indicando en cuál se reservó.
     */
    record ElementoAgenda(String tipo, long canchaId, Instant inicio, Instant fin, Long reservaId, String codigo, String estado,
            String cliente, String equipo, Integer precioFinal, String estadoPago, String origen, String canchaReservaNombre,
            Long bloqueoId, String motivoBloqueo) {
    }
}
