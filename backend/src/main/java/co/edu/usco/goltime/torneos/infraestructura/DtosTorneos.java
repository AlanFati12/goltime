package co.edu.usco.goltime.torneos.infraestructura;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import co.edu.usco.goltime.torneos.dominio.CriterioDesempate;
import co.edu.usco.goltime.torneos.dominio.EstadoPartido;
import co.edu.usco.goltime.torneos.dominio.EstadoTorneo;
import co.edu.usco.goltime.torneos.dominio.FormatoTorneo;
import co.edu.usco.goltime.torneos.dominio.Lado;
import co.edu.usco.goltime.torneos.dominio.ModoEmparejamiento;
import co.edu.usco.goltime.torneos.dominio.TipoEvento;
import co.edu.usco.goltime.torneos.dominio.TipoFase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Contratos JSON de equipos, torneos y partidos. Los instantes viajan en ISO-8601 UTC. */
public final class DtosTorneos {

    private DtosTorneos() {
    }

    // ===== Solicitudes de administración =====

    public record SolicitudEquipo(@NotBlank @Size(max = 100) String nombre, @Size(max = 20) String color, @Size(max = 150) String delegadoNombre,
            @Size(max = 30) String delegadoTelefono, Long usuarioResponsableId) {
    }

    public record SolicitudActivo(@NotNull Boolean activo) {
    }

    /** O bien {@code jugadorId} (jugador existente), o bien los datos de uno nuevo. */
    public record SolicitudJugadorEnEquipo(Long jugadorId, @Size(max = 100) String nombres, @Size(max = 100) String apellidos,
            @Size(max = 30) String documento, @Size(max = 30) String posicion, @Min(0) @Max(99) Integer dorsal) {
    }

    public record SolicitudJugador(@NotBlank @Size(max = 100) String nombres, @NotBlank @Size(max = 100) String apellidos,
            @Size(max = 30) String documento, @Size(max = 30) String posicion) {
    }

    public record SolicitudDorsal(@Min(0) @Max(99) Integer dorsal) {
    }

    public record SolicitudTorneo(
            @NotBlank @Size(max = 150) String nombre,
            String descripcion,
            @NotNull Long sedeId,
            @NotNull @Min(2) @Max(11) Integer jugadoresPorEquipo,
            @NotNull FormatoTorneo formato,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            @Min(2) Integer cupoEquipos,
            @Min(0) Integer valorInscripcion,
            String premiosTexto,
            String reglamento,
            ModoEmparejamiento modoEmparejamiento,
            Boolean idaYVuelta,
            Boolean tercerPuesto,
            @Min(2) Integer numGrupos,
            @Min(1) Integer clasificadosPorGrupo,
            Integer puntosVictoria,
            Integer puntosEmpate,
            Integer puntosDerrota,
            List<CriterioDesempate> criteriosDesempate,
            @Min(1) Integer marcadorWoGoles,
            Set<Long> canchaIds) {
    }

    public record SolicitudEstadoTorneo(@NotNull EstadoTorneo estado, @Size(max = 500) String motivo) {
    }

    public record SolicitudInscripcion(@NotNull Long equipoId, @Min(1) Integer semilla) {
    }

    public record SolicitudSemilla(@Min(1) Integer semilla) {
    }

    public record SolicitudFixture(List<Long> ordenManual) {
    }

    public record SolicitudProgramacion(@NotNull Long canchaId, @NotNull Instant inicio, @NotNull @Min(20) @Max(240) Integer duracionMin,
            @Size(max = 100) String arbitro, String huellaConfirmada) {
    }

    public record SolicitudResultado(@NotNull @Min(0) Integer golesLocal, @NotNull @Min(0) Integer golesVisitante, @Min(0) Integer penalesLocal,
            @Min(0) Integer penalesVisitante, Long figuraJugadorId) {
    }

    public record SolicitudWO(@NotNull Lado ganador, @Size(max = 500) String motivo) {
    }

    public record SolicitudEvento(@NotNull Long inscripcionId, @NotNull Long jugadorId, @NotNull TipoEvento tipo, @NotNull @Min(0) @Max(130) Integer minuto,
            Long eventoRelacionadoId, @Size(max = 200) String observacion) {
    }

    // ===== Respuestas de administración =====

    public record RespuestaEquipo(Long id, String nombre, String color, String delegadoNombre, String delegadoTelefono, Long usuarioResponsableId,
            boolean activo) {
    }

    public record RespuestaResumenEquipo(RespuestaEquipo equipo, int jugadores, int torneos) {
    }

    public record RespuestaJugador(Long id, String nombres, String apellidos, String nombreCompleto, String documento, String posicion, Integer dorsal) {
    }

    public record RespuestaDetalleEquipo(RespuestaEquipo equipo, List<RespuestaJugador> plantilla) {
    }

    public record RespuestaTorneo(Long id, String nombre, String descripcion, Long sedeId, String sedeNombre, int jugadoresPorEquipo, FormatoTorneo formato,
            EstadoTorneo estado, LocalDate fechaInicio, LocalDate fechaFin, Integer cupoEquipos, int valorInscripcion, String premiosTexto, String reglamento,
            ModoEmparejamiento modoEmparejamiento, boolean idaYVuelta, boolean tercerPuesto, Integer numGrupos, Integer clasificadosPorGrupo,
            int puntosVictoria, int puntosEmpate, int puntosDerrota, List<CriterioDesempate> criteriosDesempate, int marcadorWoGoles, Long campeonId,
            Long subcampeonId, Long terceroId, String campeon, String subcampeon, String tercero, int equiposInscritos) {
    }

    public record RespuestaInscripcion(Long id, Long equipoId, String equipo, String color, Integer semilla, int jugadores) {
    }

    public record RespuestaFase(Long id, TipoFase tipo, int orden, String nombre) {
    }

    public record RespuestaDetalleTorneo(RespuestaTorneo torneo, List<Long> canchaIds, List<RespuestaInscripcion> inscripciones, List<RespuestaFase> fases,
            int partidosTotales, int partidosJugados) {
    }

    public record RespuestaResumenFixture(int fases, int grupos, int partidos, int descansos) {
    }

    public record RespuestaConflictoHorario(Long reservaId, String codigo, String cliente, String cancha, Instant inicio, Instant fin, int precioFinal,
            boolean conPago, String estadoPago, String estado) {
    }

    public record RespuestaPrevisualizacionPartido(List<RespuestaConflictoHorario> conflictos, String huella, boolean requiereConfirmacion) {
    }

    public record RespuestaEvento(Long id, Long partidoId, Long inscripcionId, String equipo, Long jugadorId, String jugador, Integer dorsal, TipoEvento tipo,
            int minuto, Long eventoRelacionadoId, String observacion) {
    }

    // ===== Respuestas públicas y de lectura =====

    public record RespuestaFilaPosicion(Long inscripcionId, Long equipoId, String equipo, String color, int posicion, int jugados, int ganados, int empatados,
            int perdidos, int golesFavor, int golesContra, int diferencia, int puntos, int puntosDisciplina, List<String> forma) {
    }

    public record RespuestaTabla(Long faseId, String fase, Long grupoId, String grupo, List<RespuestaFilaPosicion> filas) {
    }

    public record RespuestaEquipoPublico(Long id, String nombre, String color) {
    }

    public record RespuestaPartido(Long id, Long torneoId, String torneo, Long faseId, TipoFase tipoFase, String grupo, int ronda, int numeroLlave,
            String nombreRonda, RespuestaLado local, RespuestaLado visitante, Long canchaId, String cancha, Instant inicio, Integer duracionMin,
            String arbitro, EstadoPartido estado, Integer golesLocal, Integer golesVisitante, Integer penalesLocal, Integer penalesVisitante,
            Long ganadorInscripcionId, boolean descanso, boolean porWO, Long figuraJugadorId, String figura, Long siguientePartidoId, Lado ladoSiguiente) {
    }

    public record RespuestaLado(Long inscripcionId, Long equipoId, String nombre, String color, String etiqueta) {
    }

    public record RespuestaDetallePartido(RespuestaPartido partido, List<RespuestaEvento> eventos) {
    }

    public record RespuestaPerfilEquipo(RespuestaEquipoPublico equipo, List<RespuestaJugadorPublico> plantilla, List<RespuestaTorneoDeEquipo> torneos,
            List<RespuestaPartido> partidos) {
    }

    public record RespuestaJugadorPublico(Long id, String nombre, String posicion, Integer dorsal) {
    }

    public record RespuestaTorneoDeEquipo(Long torneoId, String nombre, EstadoTorneo estado) {
    }

    public record RespuestaPerfilJugador(Long id, String nombre, String posicion, List<RespuestaEstadisticaJugador> torneos) {
    }

    public record RespuestaEstadisticaJugador(Long torneoId, String torneo, String equipo, int goles, int asistencias, int amarillas, int rojas) {
    }
}
