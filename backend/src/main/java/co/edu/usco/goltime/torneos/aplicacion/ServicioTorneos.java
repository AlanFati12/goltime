package co.edu.usco.goltime.torneos.aplicacion;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.usco.goltime.auditoria.aplicacion.RegistroAuditoria;
import co.edu.usco.goltime.compartido.dominio.Actor;
import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.notificaciones.aplicacion.Notificador;
import co.edu.usco.goltime.sedes.aplicacion.ServicioSedes;
import co.edu.usco.goltime.sedes.aplicacion.Vistas.VistaSede;
import co.edu.usco.goltime.torneos.aplicacion.CalculoTablas.TablaPosiciones;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.Equipo;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.EstadoInscripcion;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.Inscripcion;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.InscripcionVista;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.Jugador;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.JugadorConDorsal;
import co.edu.usco.goltime.torneos.dominio.CriterioDesempate;
import co.edu.usco.goltime.torneos.dominio.EstadoTorneo;
import co.edu.usco.goltime.torneos.dominio.FormatoTorneo;
import co.edu.usco.goltime.torneos.dominio.ModoEmparejamiento;
import co.edu.usco.goltime.torneos.dominio.Partido;
import co.edu.usco.goltime.torneos.dominio.ReglasPuntuacion;
import co.edu.usco.goltime.torneos.dominio.TipoFase;
import co.edu.usco.goltime.torneos.dominio.Torneo;

/** Ciclo de vida del torneo, inscripción de equipos y plantillas por torneo. */
@Service
public class ServicioTorneos {

    private static final String ENTIDAD = "torneos";

    private final RepositorioTorneos torneos;
    private final RepositorioPartidos partidos;
    private final RepositorioEquipos equipos;
    private final ServicioSedes sedes;
    private final ServicioEquipos servicioEquipos;
    private final ServicioFixture fixture;
    private final CalculoTablas tablas;
    private final Notificador notificador;
    private final RegistroAuditoria auditoria;

    public ServicioTorneos(RepositorioTorneos torneos, RepositorioPartidos partidos, RepositorioEquipos equipos, ServicioSedes sedes,
            ServicioEquipos servicioEquipos, ServicioFixture fixture, CalculoTablas tablas, Notificador notificador, RegistroAuditoria auditoria) {
        this.torneos = torneos;
        this.partidos = partidos;
        this.equipos = equipos;
        this.sedes = sedes;
        this.servicioEquipos = servicioEquipos;
        this.fixture = fixture;
        this.tablas = tablas;
        this.notificador = notificador;
        this.auditoria = auditoria;
    }

    /** Datos del torneo tal como los envía el administrador. {@code canchaIds} vacío = cualquier cancha de la sede. */
    public record DatosTorneo(String nombre, String descripcion, long sedeId, int jugadoresPorEquipo, FormatoTorneo formato, LocalDate fechaInicio,
            LocalDate fechaFin, Integer cupoEquipos, int valorInscripcion, String premiosTexto, String reglamento, ModoEmparejamiento modoEmparejamiento,
            boolean idaYVuelta, boolean tercerPuesto, Integer numGrupos, Integer clasificadosPorGrupo, int puntosVictoria, int puntosEmpate,
            int puntosDerrota, List<CriterioDesempate> criteriosDesempate, int marcadorWoGoles, Set<Long> canchaIds) {
    }

    // ===================================================================================
    // Crear y editar
    // ===================================================================================

    @Transactional
    public Torneo crear(DatosTorneo d) {
        validarSedeYCanchas(d.sedeId(), d.canchaIds());
        Torneo nuevo = aTorneo(null, d, EstadoTorneo.BORRADOR, null, null, null);
        Torneo guardado = torneos.guardar(nuevo);
        torneos.reemplazarCanchas(guardado.id(), d.canchaIds() == null ? Set.of() : d.canchaIds());
        auditoria.registrar("CREAR_TORNEO", ENTIDAD, guardado.id(), Map.of("nombre", guardado.nombre(), "formato", guardado.formato().name()), null);
        return guardado;
    }

    /**
     * Edita el torneo. Con el fixture ya generado no se pueden cambiar el formato ni la estructura (grupos, ida y vuelta,
     * tercer puesto, modo de emparejamiento, jugadores por equipo): hay que descartar primero los partidos.
     */
    @Transactional
    public Torneo actualizar(long id, DatosTorneo d) {
        Torneo actual = cargarBloqueando(id);
        if (actual.estado() == EstadoTorneo.FINALIZADO || actual.estado() == EstadoTorneo.CANCELADO) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "Un torneo " + actual.estado().name().toLowerCase() + " ya no se puede editar.");
        }
        if (actual.sedeId() != d.sedeId() && (hayInscritos(id) || !partidos.deTorneo(id).isEmpty())) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "Un torneo con equipos o partidos no puede cambiar de sede.");
        }
        validarSedeYCanchas(d.sedeId(), d.canchaIds());
        Torneo editado = aTorneo(id, d, actual.estado(), null, null, null);
        if (!partidos.deTorneo(id).isEmpty() && cambiaEstructura(actual, editado)) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO,
                    "El fixture ya está generado: no se puede cambiar el formato ni la estructura. Descarta los partidos primero.");
        }
        if (d.cupoEquipos() != null && d.cupoEquipos() < torneos.contarAprobadas(id)) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "El cupo no puede ser menor que los equipos ya inscritos.");
        }
        Torneo guardado = torneos.guardar(editado);
        torneos.reemplazarCanchas(id, d.canchaIds() == null ? Set.of() : d.canchaIds());
        Map<String, Object> cambios = new LinkedHashMap<>();
        if (!actual.nombre().equals(guardado.nombre())) {
            cambios.put("nombre", Map.of("antes", actual.nombre(), "despues", guardado.nombre()));
        }
        if (actual.formato() != guardado.formato()) {
            cambios.put("formato", Map.of("antes", actual.formato().name(), "despues", guardado.formato().name()));
        }
        auditoria.registrar("ACTUALIZAR_TORNEO", ENTIDAD, id, cambios, null);
        return guardado;
    }

    private static boolean cambiaEstructura(Torneo a, Torneo b) {
        return a.formato() != b.formato() || a.idaYVuelta() != b.idaYVuelta() || a.tercerPuesto() != b.tercerPuesto()
                || !Objects.equals(a.numGrupos(), b.numGrupos()) || !Objects.equals(a.clasificadosPorGrupo(), b.clasificadosPorGrupo())
                || a.modoEmparejamiento() != b.modoEmparejamiento() || a.jugadoresPorEquipo() != b.jugadoresPorEquipo();
    }

    // ===================================================================================
    // Estado
    // ===================================================================================

    @Transactional
    public Torneo cambiarEstado(Actor admin, long id, EstadoTorneo destino, String motivo) {
        Torneo torneo = cargarBloqueando(id);
        torneo.exigirTransicion(destino);
        List<Partido> todos = partidos.deTorneo(id);
        Torneo resultado = torneo.conEstado(destino);
        switch (destino) {
            case EN_CURSO -> {
                if (todos.isEmpty()) {
                    throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "Genera el fixture antes de iniciar el torneo.");
                }
            }
            case FINALIZADO -> {
                long pendientes = todos.stream().filter(p -> !p.bye() && !p.jugado()).count();
                if (pendientes > 0) {
                    throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "Faltan " + pendientes + " partidos por jugar para finalizar el torneo.");
                }
                resultado = conPodio(resultado, todos);
            }
            case CANCELADO -> {
                if (motivo == null || motivo.isBlank()) {
                    throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Indica el motivo de la cancelación.");
                }
                fixture.liberarProgramaciones(admin, id, "Torneo cancelado: " + motivo.trim());
            }
            default -> {
                // INSCRIPCIONES: sin condiciones adicionales
            }
        }
        Torneo guardado = torneos.guardar(resultado);
        auditoria.registrar("CAMBIAR_ESTADO_TORNEO", ENTIDAD, id, Map.of("estado", Map.of("antes", torneo.estado().name(), "despues", destino.name())),
                motivo);
        if (destino == EstadoTorneo.CANCELADO || destino == EstadoTorneo.FINALIZADO) {
            avisarAEquipos(guardado, destino == EstadoTorneo.CANCELADO ? "TORNEO_CANCELADO" : "TORNEO_FINALIZADO",
                    destino == EstadoTorneo.CANCELADO ? "Torneo cancelado" : "Torneo finalizado",
                    destino == EstadoTorneo.CANCELADO ? "El torneo %s fue cancelado. Motivo: %s".formatted(guardado.nombre(), motivo.trim())
                            : "El torneo %s terminó. ¡Gracias por participar!".formatted(guardado.nombre()));
        }
        return guardado;
    }

    /** Campeón, subcampeón y tercero (equipos, no inscripciones) según el formato. */
    private Torneo conPodio(Torneo torneo, List<Partido> todos) {
        Map<Long, Long> equipoDe = torneos.inscripciones(torneo.id()).stream().filter(i -> i.inscripcion().estado() == EstadoInscripcion.APROBADA)
                .collect(Collectors.toMap(i -> i.inscripcion().id(), i -> i.inscripcion().equipoId()));
        Long campeon = null;
        Long subcampeon = null;
        Long tercero = null;
        if (torneo.formato() == FormatoTorneo.LIGA) {
            List<TablaPosiciones> t = tablas.calcular(torneo, partidos.fases(torneo.id()), partidos.grupos(torneo.id()), todos,
                    partidos.puntosDisciplina(torneo.id()), List.copyOf(equipoDe.keySet()));
            var filas = t.get(0).filas();
            campeon = filas.size() > 0 ? equipoDe.get(filas.get(0).inscripcionId()) : null;
            subcampeon = filas.size() > 1 ? equipoDe.get(filas.get(1).inscripcionId()) : null;
            tercero = filas.size() > 2 ? equipoDe.get(filas.get(2).inscripcionId()) : null;
        } else {
            int maxRonda = todos.stream().filter(p -> p.tipoFase() == TipoFase.ELIMINATORIA).mapToInt(Partido::ronda).max().orElse(0);
            for (Partido p : todos) {
                if (p.tipoFase() != TipoFase.ELIMINATORIA || p.ronda() != maxRonda) {
                    continue;
                }
                if (p.numeroLlave() == 1) { // la final
                    campeon = equipoDe.get(p.ganadorId());
                    subcampeon = equipoDe.get(p.perdedorId());
                } else if (p.ganadorId() != null) { // tercer puesto
                    tercero = equipoDe.get(p.ganadorId());
                }
            }
        }
        return torneo.conPodio(campeon, subcampeon, tercero);
    }

    // ===================================================================================
    // Inscripciones
    // ===================================================================================

    /** Inscribe un equipo (aprobado de inmediato) y copia su plantilla habitual al torneo. */
    @Transactional
    public InscripcionVista inscribirEquipo(Actor admin, long torneoId, long equipoId, Integer semilla) {
        Torneo torneo = cargarBloqueando(torneoId);
        exigirInscripcionesAbiertas(torneo);
        Equipo equipo = equipos.buscar(equipoId).orElseThrow(() -> new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "El equipo no existe."));
        if (!equipo.activo()) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "El equipo está desactivado.");
        }
        var previa = torneos.buscarInscripcion(torneoId, equipoId);
        if (previa.isPresent() && previa.get().estado() == EstadoInscripcion.APROBADA) {
            throw new ExcepcionNegocio(CodigoError.YA_EXISTE, "El equipo ya está inscrito en este torneo.");
        }
        if (torneo.cupoEquipos() != null && torneos.contarAprobadas(torneoId) >= torneo.cupoEquipos()) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "El torneo ya tiene todos sus cupos ocupados.");
        }
        if (semilla != null && semilla < 1) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "La semilla debe ser un número positivo.");
        }
        Inscripcion inscripcion;
        if (previa.isPresent()) {
            torneos.actualizarInscripcion(previa.get().id(), EstadoInscripcion.APROBADA, semilla);
            inscripcion = new Inscripcion(previa.get().id(), torneoId, equipoId, EstadoInscripcion.APROBADA, semilla);
        } else {
            inscripcion = torneos.insertarInscripcion(torneoId, equipoId, EstadoInscripcion.APROBADA, semilla);
        }
        torneos.copiarPlantilla(inscripcion.id(), torneoId, equipoId);
        auditoria.registrar("INSCRIBIR_EQUIPO", ENTIDAD, torneoId, Map.of("equipoId", equipoId, "equipo", equipo.nombre()), null);
        if (equipo.usuarioResponsableId() != null) {
            notificador.notificar(equipo.usuarioResponsableId(), "TORNEO_INSCRIPCION", "Tu equipo quedó inscrito",
                    "%s fue inscrito en el torneo %s.".formatted(equipo.nombre(), torneo.nombre()), "torneo", torneoId);
        }
        return vista(inscripcion.id());
    }

    @Transactional
    public InscripcionVista actualizarSemilla(long torneoId, long inscripcionId, Integer semilla) {
        Torneo torneo = cargarBloqueando(torneoId);
        exigirInscripcionesAbiertas(torneo);
        Inscripcion i = cargarInscripcion(torneoId, inscripcionId);
        if (semilla != null && semilla < 1) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "La semilla debe ser un número positivo.");
        }
        torneos.actualizarInscripcion(i.id(), i.estado(), semilla);
        return vista(i.id());
    }

    /** Retira al equipo del torneo. Solo antes de generar el fixture. */
    @Transactional
    public void retirarEquipo(long torneoId, long inscripcionId) {
        Torneo torneo = cargarBloqueando(torneoId);
        exigirInscripcionesAbiertas(torneo);
        Inscripcion i = cargarInscripcion(torneoId, inscripcionId);
        torneos.eliminarInscripcion(i.id());
        auditoria.registrar("RETIRAR_EQUIPO", ENTIDAD, torneoId, Map.of("equipoId", i.equipoId()), null);
    }

    // ----- Plantillas -----

    @Transactional(readOnly = true)
    public List<JugadorConDorsal> plantilla(long torneoId, long inscripcionId) {
        cargarInscripcion(torneoId, inscripcionId);
        return torneos.plantillaDe(inscripcionId);
    }

    @Transactional
    public List<JugadorConDorsal> agregarJugador(long torneoId, long inscripcionId, long jugadorId, Integer dorsal) {
        Torneo torneo = cargarBloqueando(torneoId);
        exigirPlantillaEditable(torneo);
        cargarInscripcion(torneoId, inscripcionId);
        Jugador jugador = equipos.buscarJugador(jugadorId).orElseThrow(() -> new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "El jugador no existe."));
        if (!jugador.activo()) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "El jugador está desactivado.");
        }
        torneos.inscripcionDeJugador(torneoId, jugadorId).ifPresent(otra -> {
            throw new ExcepcionNegocio(CodigoError.YA_EXISTE, otra == inscripcionId ? "El jugador ya está en la plantilla del torneo."
                    : "El jugador ya juega con otro equipo de este torneo.");
        });
        validarDorsal(inscripcionId, dorsal, null);
        torneos.agregarJugador(inscripcionId, torneoId, jugadorId, dorsal);
        auditoria.registrar("AGREGAR_JUGADOR_TORNEO", ENTIDAD, torneoId, Map.of("jugadorId", jugadorId, "inscripcionId", inscripcionId), null);
        return torneos.plantillaDe(inscripcionId);
    }

    /** Crea un jugador nuevo en el equipo (queda también en su plantilla habitual) y lo suma a la plantilla del torneo. */
    @Transactional
    public List<JugadorConDorsal> agregarJugadorNuevo(long torneoId, long inscripcionId, ServicioEquipos.DatosJugador datos) {
        Torneo torneo = cargarBloqueando(torneoId);
        exigirPlantillaEditable(torneo);
        Inscripcion inscripcion = cargarInscripcion(torneoId, inscripcionId);
        Jugador jugador = servicioEquipos.crearEnPlantilla(inscripcion.equipoId(), datos);
        return agregarJugador(torneoId, inscripcionId, jugador.id(), datos.dorsal());
    }

    @Transactional
    public List<JugadorConDorsal> cambiarDorsal(long torneoId, long inscripcionId, long jugadorId, Integer dorsal) {
        Torneo torneo = cargarBloqueando(torneoId);
        exigirPlantillaEditable(torneo);
        cargarInscripcion(torneoId, inscripcionId);
        if (!torneos.jugadorEnInscripcion(inscripcionId, jugadorId)) {
            throw new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "El jugador no está en la plantilla.");
        }
        validarDorsal(inscripcionId, dorsal, jugadorId);
        torneos.actualizarDorsal(inscripcionId, jugadorId, dorsal);
        return torneos.plantillaDe(inscripcionId);
    }

    @Transactional
    public List<JugadorConDorsal> quitarJugador(long torneoId, long inscripcionId, long jugadorId) {
        Torneo torneo = cargarBloqueando(torneoId);
        exigirPlantillaEditable(torneo);
        cargarInscripcion(torneoId, inscripcionId);
        if (!torneos.jugadorEnInscripcion(inscripcionId, jugadorId)) {
            throw new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "El jugador no está en la plantilla.");
        }
        if (torneos.jugadorTieneEventos(torneoId, jugadorId)) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "El jugador ya tiene eventos en partidos de este torneo y no se puede quitar.");
        }
        torneos.quitarJugador(inscripcionId, jugadorId);
        auditoria.registrar("QUITAR_JUGADOR_TORNEO", ENTIDAD, torneoId, Map.of("jugadorId", jugadorId, "inscripcionId", inscripcionId), null);
        return torneos.plantillaDe(inscripcionId);
    }

    // ===================================================================================
    // Interno
    // ===================================================================================

    private Torneo cargarBloqueando(long id) {
        return torneos.buscarBloqueando(id).orElseThrow(() -> new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "El torneo no existe."));
    }

    private Inscripcion cargarInscripcion(long torneoId, long inscripcionId) {
        return torneos.buscarInscripcion(inscripcionId).filter(i -> i.torneoId() == torneoId)
                .orElseThrow(() -> new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "La inscripción no existe en este torneo."));
    }

    private InscripcionVista vista(long inscripcionId) {
        Inscripcion i = torneos.buscarInscripcion(inscripcionId).orElseThrow();
        return torneos.inscripciones(i.torneoId()).stream().filter(v -> v.inscripcion().id() == inscripcionId).findFirst().orElseThrow();
    }

    private boolean hayInscritos(long torneoId) {
        return torneos.contarAprobadas(torneoId) > 0;
    }

    private void exigirInscripcionesAbiertas(Torneo torneo) {
        if (!torneo.estado().permiteInscripciones()) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "El torneo ya no admite cambios en los equipos inscritos.");
        }
        if (!partidos.deTorneo(torneo.id()).isEmpty()) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "El fixture ya está generado: descártalo para cambiar los equipos inscritos.");
        }
    }

    private static void exigirPlantillaEditable(Torneo torneo) {
        if (torneo.estado() == EstadoTorneo.FINALIZADO || torneo.estado() == EstadoTorneo.CANCELADO) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "El torneo ya terminó: las plantillas no se pueden modificar.");
        }
    }

    private void validarDorsal(long inscripcionId, Integer dorsal, Long jugadorId) {
        if (dorsal == null) {
            return;
        }
        if (dorsal < 0 || dorsal > 99) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "El dorsal debe estar entre 0 y 99.");
        }
        if (torneos.dorsalEnUso(inscripcionId, dorsal, jugadorId)) {
            throw new ExcepcionNegocio(CodigoError.YA_EXISTE, "El dorsal " + dorsal + " ya lo usa otro jugador del equipo en este torneo.");
        }
    }

    private void validarSedeYCanchas(long sedeId, Set<Long> canchaIds) {
        VistaSede sede = sedes.detalleAdmin(sedeId);
        if (canchaIds == null || canchaIds.isEmpty()) {
            return;
        }
        Set<Long> deLaSede = sede.canchas().stream().map(c -> c.cancha().id()).collect(Collectors.toSet());
        if (!deLaSede.containsAll(canchaIds)) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Alguna cancha elegida no pertenece a la sede del torneo.");
        }
    }

    private void avisarAEquipos(Torneo torneo, String tipo, String titulo, String mensaje) {
        List<Long> inscripciones = torneos.inscripciones(torneo.id()).stream().map(i -> i.inscripcion().id()).toList();
        torneos.responsablesDeInscripciones(inscripciones).stream().distinct()
                .forEach(usuarioId -> notificador.notificar(usuarioId, tipo, titulo, mensaje, "torneo", torneo.id()));
    }

    private static Torneo aTorneo(Long id, DatosTorneo d, EstadoTorneo estado, Long campeon, Long subcampeon, Long tercero) {
        ReglasPuntuacion reglas = new ReglasPuntuacion(d.puntosVictoria(), d.puntosEmpate(), d.puntosDerrota(), d.criteriosDesempate(), d.marcadorWoGoles());
        return new Torneo(id, d.nombre(), d.descripcion(), d.sedeId(), d.jugadoresPorEquipo(), d.formato(), estado, d.fechaInicio(), d.fechaFin(),
                d.cupoEquipos(), d.valorInscripcion(), d.premiosTexto(), d.reglamento(),
                d.modoEmparejamiento() == null ? ModoEmparejamiento.ALEATORIO : d.modoEmparejamiento(), d.idaYVuelta(), d.tercerPuesto(),
                d.numGrupos(), d.clasificadosPorGrupo(), reglas, campeon, subcampeon, tercero);
    }
}
