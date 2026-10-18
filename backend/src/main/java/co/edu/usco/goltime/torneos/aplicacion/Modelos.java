package co.edu.usco.goltime.torneos.aplicacion;

import java.time.Instant;
import java.util.List;

import co.edu.usco.goltime.torneos.dominio.EstadoPartido;
import co.edu.usco.goltime.torneos.dominio.EstadoTorneo;
import co.edu.usco.goltime.torneos.dominio.Lado;
import co.edu.usco.goltime.torneos.dominio.TipoEvento;
import co.edu.usco.goltime.torneos.dominio.TipoFase;
import co.edu.usco.goltime.torneos.dominio.Torneo;

/** Datos simples que circulan entre repositorios y servicios de torneos. */
public final class Modelos {

    private Modelos() {
    }

    public record Equipo(Long id, String nombre, String color, String delegadoNombre, String delegadoTelefono, Long usuarioResponsableId,
            boolean activo) {
    }

    public record Jugador(Long id, String nombres, String apellidos, String documento, String posicion, Long usuarioId, boolean activo) {
        public String nombreCompleto() {
            return nombres + " " + apellidos;
        }
    }

    /** Jugador en la plantilla habitual de un equipo o en la de un torneo. */
    public record JugadorConDorsal(Jugador jugador, Integer dorsal) {
    }

    public record ResumenEquipo(Equipo equipo, int jugadores, int torneos) {
    }

    public enum EstadoInscripcion {
        SOLICITADA, APROBADA, RECHAZADA, RETIRADA
    }

    public record Inscripcion(long id, long torneoId, long equipoId, EstadoInscripcion estado, Integer semilla) {
    }

    public record InscripcionVista(Inscripcion inscripcion, String equipoNombre, String equipoColor, int jugadores) {
    }

    public record ResumenTorneo(Torneo torneo, String sedeNombre, int equiposInscritos, String campeonNombre) {
    }

    public record Fase(long id, long torneoId, TipoFase tipo, int orden, String nombre) {
    }

    public record Grupo(long id, long faseId, String nombre, List<Long> inscripcionIds) {
    }

    public record EventoPartido(Long id, long partidoId, long inscripcionId, long jugadorId, TipoEvento tipo, Integer minuto, Long eventoRelacionadoId,
            String observacion) {
    }

    // ----- Vistas de lectura (ya con nombres) -----

    public record EquipoPartido(Long inscripcionId, Long equipoId, String nombre, String color, String etiqueta) {
    }

    public record VistaPartido(long id, long torneoId, String torneoNombre, long faseId, TipoFase tipoFase, String grupo, int ronda, int numeroLlave,
            String nombreRonda, EquipoPartido local, EquipoPartido visitante, Long canchaId, String canchaNombre, Long reservaId, Instant inicio,
            Integer duracionMin, String arbitro, EstadoPartido estado, Integer golesLocal, Integer golesVisitante, Integer penalesLocal,
            Integer penalesVisitante, Long ganadorInscripcionId, boolean bye, boolean wo, Long figuraJugadorId, String figuraNombre,
            Long siguientePartidoId, Lado ladoSiguiente) {
    }

    public record VistaEvento(long id, long partidoId, long inscripcionId, String equipoNombre, long jugadorId, String jugadorNombre, Integer dorsal,
            TipoEvento tipo, Integer minuto, Long eventoRelacionadoId, String observacion) {
    }

    public record FilaGoleador(long jugadorId, String jugador, long equipoId, String equipo, int goles, int penales, int partidos) {
    }

    public record FilaAsistidor(long jugadorId, String jugador, long equipoId, String equipo, int asistencias) {
    }

    public record FilaTarjetas(long jugadorId, String jugador, long equipoId, String equipo, int amarillas, int rojas) {
    }

    public record EstadisticasTorneo(List<FilaGoleador> goleadores, List<FilaAsistidor> asistidores, List<FilaTarjetas> tarjetas) {
    }

    public record TorneoDeEquipo(long torneoId, String torneoNombre, EstadoTorneo estado, long inscripcionId) {
    }

    public record PerfilEquipo(Equipo equipo, List<JugadorConDorsal> plantilla, List<TorneoDeEquipo> torneos, List<VistaPartido> partidos) {
    }

    public record EstadisticaJugadorTorneo(long torneoId, String torneoNombre, String equipoNombre, int goles, int asistencias, int amarillas,
            int rojas) {
    }

    /** Perfil público de un jugador: sin documento ni datos de contacto. */
    public record PerfilJugador(long id, String nombreCompleto, String posicion, List<EstadisticaJugadorTorneo> torneos) {
    }
}
