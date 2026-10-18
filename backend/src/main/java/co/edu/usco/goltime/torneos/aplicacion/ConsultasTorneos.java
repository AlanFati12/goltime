package co.edu.usco.goltime.torneos.aplicacion;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import co.edu.usco.goltime.torneos.aplicacion.Modelos.EquipoPartido;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.EstadisticasTorneo;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.PerfilEquipo;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.PerfilJugador;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.VistaEvento;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.VistaPartido;
import co.edu.usco.goltime.torneos.dominio.EstadoPartido;

/** Consultas de lectura con nombres ya resueltos para las pantallas públicas y de administración. */
public interface ConsultasTorneos {

    /** Filtros de partidos; los nulos no filtran. */
    record FiltroPartidos(Long faseId, Long inscripcionId, EstadoPartido estado) {
    }

    List<VistaPartido> partidos(long torneoId, FiltroPartidos filtro);

    Optional<VistaPartido> partido(long partidoId);

    List<VistaEvento> eventos(long partidoId);

    /** Equipo que corresponde a cada inscripción del torneo. */
    Map<Long, EquipoPartido> equiposInscritos(long torneoId);

    EstadisticasTorneo estadisticas(long torneoId, int limite);

    /** Solo cuenta torneos públicos (no borradores). */
    Optional<PerfilEquipo> perfilEquipo(long equipoId);

    Optional<PerfilJugador> perfilJugador(long jugadorId);

    /** Próximos partidos programados de torneos públicos, para la portada. */
    List<VistaPartido> proximosPartidos(int limite);
}
