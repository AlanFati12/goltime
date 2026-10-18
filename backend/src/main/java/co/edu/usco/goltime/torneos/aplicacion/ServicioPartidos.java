package co.edu.usco.goltime.torneos.aplicacion;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.usco.goltime.auditoria.aplicacion.RegistroAuditoria;
import co.edu.usco.goltime.compartido.dominio.Actor;
import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.notificaciones.aplicacion.Notificador;
import co.edu.usco.goltime.reservas.aplicacion.ServicioReservasTorneo;
import co.edu.usco.goltime.reservas.aplicacion.ServicioReservasTorneo.ConflictosHorario;
import co.edu.usco.goltime.sedes.aplicacion.ServicioCanchas;
import co.edu.usco.goltime.sedes.aplicacion.Vistas.VistaCancha;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.EquipoPartido;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.EventoPartido;
import co.edu.usco.goltime.torneos.dominio.EstadoPartido;
import co.edu.usco.goltime.torneos.dominio.EstadoTorneo;
import co.edu.usco.goltime.torneos.dominio.Lado;
import co.edu.usco.goltime.torneos.dominio.Partido;
import co.edu.usco.goltime.torneos.dominio.TipoEvento;
import co.edu.usco.goltime.torneos.dominio.TipoFase;
import co.edu.usco.goltime.torneos.dominio.Torneo;

/**
 * Programación de los partidos (con reserva de cancha), resultados, W.O. y eventos. Cada resultado de eliminatoria
 * mueve al ganador por el cuadro; al terminar la fase de grupos los clasificados ocupan las llaves.
 */
@Service
public class ServicioPartidos {

    private static final String ENTIDAD = "partidos";

    private final RepositorioPartidos partidos;
    private final RepositorioTorneos torneos;
    private final ConsultasTorneos consultas;
    private final AvanceDeLlaves avance;
    private final ServicioReservasTorneo reservasTorneo;
    private final ServicioCanchas canchas;
    private final Notificador notificador;
    private final RegistroAuditoria auditoria;
    private final Clock reloj;

    public ServicioPartidos(RepositorioPartidos partidos, RepositorioTorneos torneos, ConsultasTorneos consultas, AvanceDeLlaves avance,
            ServicioReservasTorneo reservasTorneo, ServicioCanchas canchas, Notificador notificador, RegistroAuditoria auditoria, Clock reloj) {
        this.partidos = partidos;
        this.torneos = torneos;
        this.consultas = consultas;
        this.avance = avance;
        this.reservasTorneo = reservasTorneo;
        this.canchas = canchas;
        this.notificador = notificador;
        this.auditoria = auditoria;
        this.reloj = reloj;
    }

    public record DatosProgramacion(long canchaId, Instant inicio, int duracionMin, String arbitro) {
    }

    public record DatosResultado(int golesLocal, int golesVisitante, Integer penalesLocal, Integer penalesVisitante, Long figuraJugadorId) {
    }

    public record DatosEvento(long inscripcionId, long jugadorId, TipoEvento tipo, int minuto, Long eventoRelacionadoId, String observacion) {
    }

    // ===================================================================================
    // Programación
    // ===================================================================================

    /** Muestra qué reservas de clientes se cancelarían al programar el partido así, sin cambiar nada. */
    @Transactional(readOnly = true)
    public ConflictosHorario previsualizarProgramacion(long partidoId, DatosProgramacion d) {
        Partido p = cargar(partidoId);
        Torneo torneo = cargarTorneo(p.torneoId());
        validarProgramacion(torneo, p, d);
        return reservasTorneo.previsualizar(d.canchaId(), d.inicio(), d.inicio().plusSeconds(d.duracionMin() * 60L), p.reservaId());
    }

    @Transactional
    public Partido programar(Actor admin, long partidoId, DatosProgramacion d, String huellaConfirmada) {
        Partido p = partidos.buscarBloqueando(partidoId).orElseThrow(this::noExiste);
        Torneo torneo = cargarTorneo(p.torneoId());
        validarProgramacion(torneo, p, d);
        Instant fin = d.inicio().plusSeconds(d.duracionMin() * 60L);
        if (p.listoParaJugar() && partidos.equipoOcupado(torneo.id(), List.of(p.localId(), p.visitanteId()), d.inicio(), fin, partidoId)) {
            throw new ExcepcionNegocio(CodigoError.EQUIPO_OCUPADO, "Alguno de los equipos ya tiene otro partido en ese horario.");
        }
        Long anterior = p.reservaId();
        if (anterior != null) {
            reservasTorneo.liberar(admin, anterior, "Partido reprogramado");
        }
        long reservaId = reservasTorneo.reservar(admin, d.canchaId(), d.inicio(), fin, descripcion(torneo, p), huellaConfirmada, anterior);
        p.programar(d.canchaId(), reservaId, d.inicio(), d.duracionMin(), d.arbitro());
        partidos.guardar(p);
        auditoria.registrar("PROGRAMAR_PARTIDO", ENTIDAD, partidoId,
                Map.of("torneoId", torneo.id(), "canchaId", d.canchaId(), "inicio", d.inicio().toString(), "duracionMin", d.duracionMin()), null);
        avisarEquipos(p, torneo, "PARTIDO_PROGRAMADO", "Partido programado",
                "%s se juega el %s.".formatted(descripcion(torneo, p), co.edu.usco.goltime.compartido.dominio.FormatoTexto.fechaHora(d.inicio())));
        return p;
    }

    /** Quita la fecha y cancha del partido y libera la cancha. {@code aplazar} lo deja como APLAZADO. */
    @Transactional
    public Partido desprogramar(Actor admin, long partidoId, boolean aplazar) {
        Partido p = partidos.buscarBloqueando(partidoId).orElseThrow(this::noExiste);
        Torneo torneo = cargarTorneo(p.torneoId());
        exigirTorneoActivo(torneo);
        if (p.reservaId() != null) {
            reservasTorneo.liberar(admin, p.reservaId(), aplazar ? "Partido aplazado" : "Partido sin programación");
        }
        p.desprogramar(aplazar ? EstadoPartido.APLAZADO : EstadoPartido.PENDIENTE);
        partidos.guardar(p);
        auditoria.registrar(aplazar ? "APLAZAR_PARTIDO" : "DESPROGRAMAR_PARTIDO", ENTIDAD, partidoId, Map.of("torneoId", torneo.id()), null);
        if (aplazar) {
            avisarEquipos(p, torneo, "PARTIDO_APLAZADO", "Partido aplazado", descripcion(torneo, p) + " fue aplazado. Te avisaremos la nueva fecha.");
        }
        return p;
    }

    @Transactional
    public Partido iniciar(long partidoId) {
        Partido p = partidos.buscarBloqueando(partidoId).orElseThrow(this::noExiste);
        exigirEnCurso(cargarTorneo(p.torneoId()));
        p.iniciar();
        partidos.guardar(p);
        return p;
    }

    // ===================================================================================
    // Resultados
    // ===================================================================================

    @Transactional
    public Partido registrarResultado(Actor admin, long partidoId, DatosResultado d) {
        Partido p = partidos.buscarBloqueando(partidoId).orElseThrow(this::noExiste);
        Torneo torneo = cargarTorneo(p.torneoId());
        exigirEnCurso(torneo);
        validarFigura(p, d.figuraJugadorId());
        boolean estabaJugado = p.jugado();
        Long ganadorAntes = p.ganadorId();
        antesDeCambiarResultado(p, estabaJugado);

        p.registrarResultado(d.golesLocal(), d.golesVisitante(), d.penalesLocal(), d.penalesVisitante(), d.figuraJugadorId());
        exigirEventosCoherentes(p);
        if (estabaJugado && p.esEliminatoria() && !Objects.equals(ganadorAntes, p.ganadorId())) {
            avance.exigirSiguientesSinJugar(p);
            avance.retirar(p);
        }
        despuesDeResultado(admin, torneo, p, "REGISTRAR_RESULTADO", estabaJugado);
        return p;
    }

    /** Gana {@code ganador} por no presentación del rival, con el marcador de W.O. del torneo. */
    @Transactional
    public Partido registrarWO(Actor admin, long partidoId, Lado ganador, String motivo) {
        Partido p = partidos.buscarBloqueando(partidoId).orElseThrow(this::noExiste);
        Torneo torneo = cargarTorneo(p.torneoId());
        exigirEnCurso(torneo);
        if (!partidos.eventosDe(partidoId).isEmpty()) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "El partido tiene eventos registrados: elimínalos antes de darlo por W.O.");
        }
        boolean estabaJugado = p.jugado();
        Long ganadorAntes = p.ganadorId();
        antesDeCambiarResultado(p, estabaJugado);
        p.registrarWO(ganador, torneo.reglas().marcadorWoGoles());
        if (estabaJugado && p.esEliminatoria() && !Objects.equals(ganadorAntes, p.ganadorId())) {
            avance.exigirSiguientesSinJugar(p);
            avance.retirar(p);
        }
        despuesDeResultado(admin, torneo, p, "REGISTRAR_WO", estabaJugado);
        if (motivo != null && !motivo.isBlank()) {
            auditoria.registrar("MOTIVO_WO", ENTIDAD, partidoId, Map.of("ganador", ganador.name()), motivo.trim());
        }
        return p;
    }

    /** Deshace el resultado para volver a registrarlo. */
    @Transactional
    public Partido reabrir(Actor admin, long partidoId) {
        Partido p = partidos.buscarBloqueando(partidoId).orElseThrow(this::noExiste);
        Torneo torneo = cargarTorneo(p.torneoId());
        exigirEnCurso(torneo);
        antesDeCambiarResultado(p, true);
        if (p.esEliminatoria()) {
            avance.exigirSiguientesSinJugar(p);
            avance.retirar(p);
        }
        p.reabrir();
        partidos.guardar(p);
        if (p.tipoFase() == TipoFase.GRUPOS) {
            avance.limpiarClasificados(torneo.id());
        }
        auditoria.registrar("REABRIR_PARTIDO", ENTIDAD, partidoId, Map.of("torneoId", torneo.id()), null);
        return p;
    }

    private void antesDeCambiarResultado(Partido p, boolean estabaJugado) {
        if (estabaJugado && p.tipoFase() == TipoFase.GRUPOS && AvanceDeLlaves.eliminatoriaEmpezada(partidos.deTorneo(p.torneoId()))) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "Ya hay partidos de eliminatoria jugados: no se puede cambiar un resultado de la fase de grupos.");
        }
    }

    private void despuesDeResultado(Actor admin, Torneo torneo, Partido p, String accion, boolean correccion) {
        partidos.guardar(p);
        if (p.esEliminatoria()) {
            avance.propagar(p);
        } else if (p.tipoFase() == TipoFase.GRUPOS) {
            avance.resolverClasificados(torneo);
        }
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("torneoId", torneo.id());
        datos.put("marcador", p.golesLocal() + "-" + p.golesVisitante());
        if (p.penalesLocal() != null) {
            datos.put("penales", p.penalesLocal() + "-" + p.penalesVisitante());
        }
        datos.put("correccion", correccion);
        auditoria.registrar(accion, ENTIDAD, p.id(), datos, null);
        Map<Long, EquipoPartido> equipos = consultas.equiposInscritos(torneo.id());
        String local = equipos.get(p.localId()).nombre();
        String visitante = equipos.get(p.visitanteId()).nombre();
        avisarEquipos(p, torneo, "PARTIDO_RESULTADO", "Resultado del partido",
                "%s %d - %d %s%s".formatted(local, p.golesLocal(), p.golesVisitante(), visitante, p.wo() ? " (W.O.)" : ""));
    }

    // ===================================================================================
    // Eventos
    // ===================================================================================

    @Transactional
    public EventoPartido agregarEvento(Actor admin, long partidoId, DatosEvento d) {
        Partido p = partidos.buscarBloqueando(partidoId).orElseThrow(this::noExiste);
        Torneo torneo = cargarTorneo(p.torneoId());
        exigirEnCurso(torneo);
        if (!p.listoParaJugar() || p.wo() || p.estado() == EstadoPartido.BYE) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "Este partido no admite eventos.");
        }
        if (d.minuto() < 0 || d.minuto() > 130) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "El minuto debe estar entre 0 y 130.");
        }
        if (!Objects.equals(p.localId(), d.inscripcionId()) && !Objects.equals(p.visitanteId(), d.inscripcionId())) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "El equipo del evento no juega este partido.");
        }
        if (!torneos.jugadorEnInscripcion(d.inscripcionId(), d.jugadorId())) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "El jugador no está en la plantilla de ese equipo en el torneo.");
        }
        validarRelacionado(partidoId, d);

        EventoPartido guardado = partidos.insertarEvento(new EventoPartido(null, partidoId, d.inscripcionId(), d.jugadorId(), d.tipo(), d.minuto(),
                d.eventoRelacionadoId(), d.observacion() == null || d.observacion().isBlank() ? null : d.observacion().trim()));
        if (p.jugado()) {
            exigirEventosCoherentes(p);
        }
        auditoria.registrar("REGISTRAR_EVENTO", ENTIDAD, partidoId,
                Map.of("tipo", d.tipo().name(), "minuto", d.minuto(), "jugadorId", d.jugadorId(), "eventoId", guardado.id()), null);
        return guardado;
    }

    @Transactional
    public void eliminarEvento(Actor admin, long partidoId, long eventoId) {
        Partido p = partidos.buscarBloqueando(partidoId).orElseThrow(this::noExiste);
        exigirEnCurso(cargarTorneo(p.torneoId()));
        EventoPartido evento = partidos.buscarEvento(eventoId).filter(e -> e.partidoId() == partidoId)
                .orElseThrow(() -> new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "El evento no existe en este partido."));
        boolean tieneAsistencia = partidos.eventosDe(partidoId).stream().anyMatch(e -> Objects.equals(e.eventoRelacionadoId(), eventoId));
        if (tieneAsistencia) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "Elimina primero la asistencia ligada a este gol.");
        }
        partidos.eliminarEvento(eventoId);
        auditoria.registrar("ELIMINAR_EVENTO", ENTIDAD, partidoId, Map.of("tipo", evento.tipo().name(), "minuto", evento.minuto()), null);
    }

    /** Una asistencia se liga a un gol del mismo equipo hecho por otro jugador; los demás eventos no se ligan. */
    private void validarRelacionado(long partidoId, DatosEvento d) {
        if (d.tipo() != TipoEvento.ASISTENCIA) {
            if (d.eventoRelacionadoId() != null) {
                throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Solo las asistencias se ligan a un gol.");
            }
            return;
        }
        if (d.eventoRelacionadoId() == null) {
            return;
        }
        EventoPartido gol = partidos.buscarEvento(d.eventoRelacionadoId()).filter(e -> e.partidoId() == partidoId)
                .orElseThrow(() -> new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "El gol ligado a la asistencia no existe en este partido."));
        if (gol.tipo() != TipoEvento.GOL) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Solo un gol de jugada tiene asistencia.");
        }
        if (gol.inscripcionId() != d.inscripcionId() || gol.jugadorId() == d.jugadorId()) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "La asistencia debe ser de otro jugador del mismo equipo que anotó.");
        }
    }

    /**
     * Los goles detallados de cada lado no pueden superar el marcador. Puede haber menos (goles sin detallar), pero nunca más.
     */
    private void exigirEventosCoherentes(Partido p) {
        int[] goles = golesPorLado(p, partidos.eventosDe(p.id()));
        if (goles[0] > p.golesLocal() || goles[1] > p.golesVisitante()) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Los goles registrados como eventos (" + goles[0] + "-" + goles[1]
                    + ") superan el marcador (" + p.golesLocal() + "-" + p.golesVisitante() + ").");
        }
    }

    /** Goles de {@code [local, visitante]} según los eventos: el autogol suma al rival de quien lo hizo. */
    public static int[] golesPorLado(Partido p, List<EventoPartido> eventos) {
        int local = 0;
        int visitante = 0;
        for (EventoPartido e : eventos) {
            if (!e.tipo().esGol()) {
                continue;
            }
            boolean deLocal = Objects.equals(e.inscripcionId(), p.localId());
            boolean cuentaParaLocal = e.tipo() == TipoEvento.AUTOGOL ? !deLocal : deLocal;
            if (cuentaParaLocal) {
                local++;
            } else {
                visitante++;
            }
        }
        return new int[] {local, visitante};
    }

    // ===================================================================================
    // Interno
    // ===================================================================================

    private void validarProgramacion(Torneo torneo, Partido p, DatosProgramacion d) {
        exigirTorneoActivo(torneo);
        if (p.jugado()) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "El partido ya tiene resultado.");
        }
        if (p.bye()) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "Un partido con descanso no se programa.");
        }
        if (d.inicio() == null) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Indica la fecha y hora de inicio.");
        }
        if (d.duracionMin() < 20 || d.duracionMin() > 240) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "La duración del partido debe estar entre 20 y 240 minutos.");
        }
        VistaCancha cancha = canchas.detallePublico(d.canchaId());
        if (cancha.sede().id() != torneo.sedeId()) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "La cancha no pertenece a la sede del torneo.");
        }
        Set<Long> permitidas = torneos.canchas(torneo.id());
        if (!permitidas.isEmpty() && !permitidas.contains(d.canchaId())) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "La cancha no está entre las canchas del torneo.");
        }
    }

    private void validarFigura(Partido p, Long jugadorId) {
        if (jugadorId == null) {
            return;
        }
        boolean valido = torneos.inscripcionDeJugador(p.torneoId(), jugadorId)
                .map(i -> Objects.equals(i, p.localId()) || Objects.equals(i, p.visitanteId())).orElse(false);
        if (!valido) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "La figura del partido debe ser un jugador de alguno de los dos equipos.");
        }
    }

    private void exigirEnCurso(Torneo torneo) {
        if (torneo.estado() != EstadoTorneo.EN_CURSO) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "Inicia el torneo para registrar resultados y eventos.");
        }
    }

    private static void exigirTorneoActivo(Torneo torneo) {
        if (torneo.estado() == EstadoTorneo.FINALIZADO || torneo.estado() == EstadoTorneo.CANCELADO) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "El torneo ya terminó.");
        }
    }

    private String descripcion(Torneo torneo, Partido p) {
        Map<Long, EquipoPartido> equipos = consultas.equiposInscritos(torneo.id());
        String local = nombre(equipos, p.localId(), p.etiquetaLocal());
        String visitante = nombre(equipos, p.visitanteId(), p.etiquetaVisitante());
        return "%s: %s vs %s".formatted(torneo.nombre(), local, visitante);
    }

    private static String nombre(Map<Long, EquipoPartido> equipos, Long inscripcionId, String etiqueta) {
        if (inscripcionId != null && equipos.containsKey(inscripcionId)) {
            return equipos.get(inscripcionId).nombre();
        }
        return etiqueta != null ? etiqueta : "Por definir";
    }

    private void avisarEquipos(Partido p, Torneo torneo, String tipo, String titulo, String mensaje) {
        List<Long> inscripciones = new java.util.ArrayList<>();
        if (p.localId() != null) {
            inscripciones.add(p.localId());
        }
        if (p.visitanteId() != null) {
            inscripciones.add(p.visitanteId());
        }
        torneos.responsablesDeInscripciones(inscripciones).stream().distinct()
                .forEach(usuarioId -> notificador.notificar(usuarioId, tipo, titulo, mensaje, "partido", p.id()));
    }

    private Partido cargar(long id) {
        return partidos.buscar(id).orElseThrow(this::noExiste);
    }

    private Torneo cargarTorneo(long id) {
        return torneos.buscar(id).orElseThrow(() -> new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "El torneo no existe."));
    }

    private ExcepcionNegocio noExiste() {
        return new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "El partido no existe.");
    }
}
