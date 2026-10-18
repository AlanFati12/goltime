package co.edu.usco.goltime.torneos.aplicacion;

import java.util.List;
import java.util.Optional;

import co.edu.usco.goltime.torneos.aplicacion.Modelos.Equipo;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.Jugador;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.JugadorConDorsal;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.ResumenEquipo;

/** Equipos, jugadores y la plantilla habitual de cada equipo (se reutilizan entre torneos). */
public interface RepositorioEquipos {

    Equipo guardar(Equipo equipo);

    Optional<Equipo> buscar(long id);

    boolean existeNombre(String nombre, Long ignorarId);

    List<ResumenEquipo> listar(String busqueda, boolean incluirInactivos);

    Jugador guardarJugador(Jugador jugador);

    Optional<Jugador> buscarJugador(long id);

    List<Jugador> buscarJugadores(String texto, int limite);

    /** Jugadores activos de la plantilla habitual del equipo. */
    List<JugadorConDorsal> plantilla(long equipoId);

    boolean enPlantilla(long equipoId, long jugadorId);

    void agregarAPlantilla(long equipoId, long jugadorId, Integer dorsal);

    void actualizarDorsal(long equipoId, long jugadorId, Integer dorsal);

    void quitarDePlantilla(long equipoId, long jugadorId);

    boolean dorsalEnUso(long equipoId, int dorsal, Long ignorarJugadorId);
}
