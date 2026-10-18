package co.edu.usco.goltime.torneos.aplicacion;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.usco.goltime.auditoria.aplicacion.RegistroAuditoria;
import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.notificaciones.aplicacion.Notificador;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.Equipo;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.EstadoInscripcion;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.Inscripcion;
import co.edu.usco.goltime.torneos.aplicacion.RepositorioTorneos.SolicitudDeCliente;
import co.edu.usco.goltime.torneos.dominio.EstadoTorneo;
import co.edu.usco.goltime.torneos.dominio.Torneo;
import co.edu.usco.goltime.usuarios.aplicacion.RepositorioUsuarios;

/**
 * Inscripción de equipos por parte de clientes. Solo la puede pedir un cliente al que el administrador habilitó
 * ({@code puede_inscribir_equipos}) y mientras el torneo tiene las inscripciones abiertas. La solicitud crea el equipo (con el cliente como
 * responsable) y su plantilla, y queda pendiente hasta que el administrador la apruebe o la rechace.
 */
@Service
public class ServicioSolicitudes {

    private static final String ENTIDAD = "torneos";

    private final RepositorioTorneos torneos;
    private final RepositorioPartidos partidos;
    private final RepositorioEquipos equipos;
    private final ServicioEquipos servicioEquipos;
    private final RepositorioUsuarios usuarios;
    private final Notificador notificador;
    private final RegistroAuditoria auditoria;

    public ServicioSolicitudes(RepositorioTorneos torneos, RepositorioPartidos partidos, RepositorioEquipos equipos, ServicioEquipos servicioEquipos,
            RepositorioUsuarios usuarios, Notificador notificador, RegistroAuditoria auditoria) {
        this.torneos = torneos;
        this.partidos = partidos;
        this.equipos = equipos;
        this.servicioEquipos = servicioEquipos;
        this.usuarios = usuarios;
        this.notificador = notificador;
        this.auditoria = auditoria;
    }

    /** Un jugador de la solicitud: siempre se crea nuevo (para reutilizar uno existente el administrador inscribe el equipo). */
    public record JugadorSolicitado(String nombres, String apellidos, String documento, String posicion, Integer dorsal) {
    }

    public record DatosSolicitud(String nombreEquipo, String color, String delegadoTelefono, List<JugadorSolicitado> jugadores) {
    }

    public record MisSolicitudes(boolean habilitado, List<SolicitudDeCliente> solicitudes) {
    }

    @Transactional(readOnly = true)
    public MisSolicitudes mias(long clienteId) {
        return new MisSolicitudes(habilitado(clienteId), torneos.solicitudesDe(clienteId));
    }

    @Transactional
    public Inscripcion solicitar(long clienteId, long torneoId, DatosSolicitud d) {
        if (!habilitado(clienteId)) {
            throw new ExcepcionNegocio(CodigoError.ACCESO_DENEGADO, "Tu cuenta no está habilitada para inscribir equipos. Pídele al administrador que la habilite.");
        }
        Torneo torneo = torneos.buscarBloqueando(torneoId).filter(t -> t.estado().esPublico())
                .orElseThrow(() -> new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "El torneo no existe."));
        if (torneo.estado() != EstadoTorneo.INSCRIPCIONES || !partidos.deTorneo(torneoId).isEmpty()) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "Este torneo no tiene las inscripciones abiertas.");
        }
        if (torneo.cupoEquipos() != null && torneos.contarAprobadas(torneoId) >= torneo.cupoEquipos()) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "El torneo ya tiene todos sus cupos ocupados.");
        }
        List<JugadorSolicitado> jugadores = d.jugadores() == null ? List.of() : d.jugadores();
        if (jugadores.size() < torneo.jugadoresPorEquipo()) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "El equipo necesita al menos " + torneo.jugadoresPorEquipo() + " jugadores.");
        }
        Equipo equipo = servicioEquipos.crear(new ServicioEquipos.DatosEquipo(d.nombreEquipo(), d.color(), null, d.delegadoTelefono(), clienteId)).equipo();
        for (JugadorSolicitado j : jugadores) {
            servicioEquipos.crearEnPlantilla(equipo.id(), new ServicioEquipos.DatosJugador(j.nombres(), j.apellidos(), j.documento(), j.posicion(), j.dorsal()));
        }
        Inscripcion inscripcion = torneos.insertarInscripcion(torneoId, equipo.id(), EstadoInscripcion.SOLICITADA, null);
        torneos.copiarPlantilla(inscripcion.id(), torneoId, equipo.id());
        auditoria.registrar("SOLICITAR_INSCRIPCION", ENTIDAD, torneoId, Map.of("equipo", equipo.nombre(), "jugadores", jugadores.size()), null);
        notificador.notificarAdministradores("INSCRIPCION_SOLICITADA", "Nueva solicitud de inscripción",
                "%s pidió inscribir a %s en el torneo %s.".formatted(nombreCliente(clienteId), equipo.nombre(), torneo.nombre()), "torneo", torneoId);
        return inscripcion;
    }

    @Transactional
    public Inscripcion aprobar(long torneoId, long inscripcionId) {
        Torneo torneo = torneos.buscarBloqueando(torneoId).orElseThrow(() -> new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "El torneo no existe."));
        Inscripcion i = pendiente(torneoId, inscripcionId);
        if (!torneo.estado().permiteInscripciones() || !partidos.deTorneo(torneoId).isEmpty()) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "El torneo ya no admite nuevos equipos.");
        }
        if (torneo.cupoEquipos() != null && torneos.contarAprobadas(torneoId) >= torneo.cupoEquipos()) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "El torneo ya tiene todos sus cupos ocupados.");
        }
        torneos.actualizarInscripcion(i.id(), EstadoInscripcion.APROBADA, i.semilla());
        Equipo equipo = equipos.buscar(i.equipoId()).orElseThrow();
        auditoria.registrar("APROBAR_INSCRIPCION", ENTIDAD, torneoId, Map.of("equipo", equipo.nombre()), null);
        avisar(equipo, "INSCRIPCION_APROBADA", "Inscripción aprobada", "%s quedó inscrito en el torneo %s. ¡Mucha suerte!".formatted(equipo.nombre(), torneo.nombre()), torneoId);
        return new Inscripcion(i.id(), i.torneoId(), i.equipoId(), EstadoInscripcion.APROBADA, i.semilla());
    }

    @Transactional
    public void rechazar(long torneoId, long inscripcionId, String motivo) {
        Torneo torneo = torneos.buscar(torneoId).orElseThrow(() -> new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "El torneo no existe."));
        Inscripcion i = pendiente(torneoId, inscripcionId);
        if (motivo == null || motivo.isBlank()) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Indica el motivo del rechazo.");
        }
        torneos.actualizarInscripcion(i.id(), EstadoInscripcion.RECHAZADA, i.semilla());
        torneos.vaciarPlantilla(i.id());
        Equipo equipo = equipos.buscar(i.equipoId()).orElseThrow();
        auditoria.registrar("RECHAZAR_INSCRIPCION", ENTIDAD, torneoId, Map.of("equipo", equipo.nombre()), motivo.trim());
        avisar(equipo, "INSCRIPCION_RECHAZADA", "Inscripción no aprobada",
                "La inscripción de %s en %s no fue aprobada. Motivo: %s".formatted(equipo.nombre(), torneo.nombre(), motivo.trim()), torneoId);
    }

    /** El cliente retira su propia solicitud pendiente. */
    @Transactional
    public void retirar(long clienteId, long inscripcionId) {
        Inscripcion i = torneos.buscarInscripcion(inscripcionId).filter(x -> equipos.buscar(x.equipoId()).map(e -> Long.valueOf(clienteId).equals(e.usuarioResponsableId())).orElse(false))
                .orElseThrow(() -> new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "La solicitud no existe."));
        if (i.estado() != EstadoInscripcion.SOLICITADA) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "Solo se puede retirar una solicitud pendiente.");
        }
        torneos.actualizarInscripcion(i.id(), EstadoInscripcion.RETIRADA, i.semilla());
        torneos.vaciarPlantilla(i.id());
        auditoria.registrar("RETIRAR_SOLICITUD", ENTIDAD, i.torneoId(), Map.of("inscripcionId", i.id()), null);
    }

    private Inscripcion pendiente(long torneoId, long inscripcionId) {
        Inscripcion i = torneos.buscarInscripcion(inscripcionId).filter(x -> x.torneoId() == torneoId)
                .orElseThrow(() -> new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "La solicitud no existe en este torneo."));
        if (i.estado() != EstadoInscripcion.SOLICITADA) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "Esa solicitud ya fue resuelta.");
        }
        return i;
    }

    private boolean habilitado(long clienteId) {
        return usuarios.buscarPorId(clienteId).map(u -> u.activo() && u.puedeInscribirEquipos()).orElse(false);
    }

    private String nombreCliente(long clienteId) {
        return usuarios.buscarPorId(clienteId).map(u -> u.nombres() + " " + u.apellidos()).orElse("Un cliente");
    }

    private void avisar(Equipo equipo, String tipo, String titulo, String mensaje, long torneoId) {
        if (equipo.usuarioResponsableId() != null) {
            notificador.notificar(equipo.usuarioResponsableId(), tipo, titulo, mensaje, "torneo", torneoId);
        }
    }
}
