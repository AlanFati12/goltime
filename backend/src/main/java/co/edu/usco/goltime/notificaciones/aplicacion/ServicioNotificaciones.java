package co.edu.usco.goltime.notificaciones.aplicacion;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;

import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.compartido.dominio.Pagina;
import co.edu.usco.goltime.compartido.dominio.SolicitudPagina;
import co.edu.usco.goltime.notificaciones.aplicacion.EnviadorCorreo.Adjunto;
import co.edu.usco.goltime.notificaciones.aplicacion.EnviadorCorreo.MensajeCorreo;
import co.edu.usco.goltime.notificaciones.dominio.Notificacion;
import co.edu.usco.goltime.usuarios.aplicacion.RepositorioUsuarios;
import co.edu.usco.goltime.usuarios.dominio.Usuario;

/** Notificaciones in-app: crear (desde otros módulos) y consultar o marcar como leídas (por su dueño). */
@Service
public class ServicioNotificaciones implements Notificador {

    /** Avisos que además llegan por correo: confirmaciones y cambios que el cliente no puede ignorar. */
    static final Set<String> TIPOS_CON_CORREO = Set.of("RESERVA_CONFIRMADA", "RESERVA_RECHAZADA", "RESERVA_EXPIRADA", "RESERVA_CANCELADA_ADMIN",
            "RESERVA_REPROGRAMADA_ADMIN", "RESERVA_CREADA_ADMIN", "RESERVA_RECORDATORIO", "PREMIO_GANADO", "ESPERA_LIBERADA", "INSCRIPCION_APROBADA",
            "INSCRIPCION_RECHAZADA", "TORNEO_CANCELADO");

    private final RepositorioNotificaciones notificaciones;
    private final RepositorioUsuarios usuarios;
    private final EnviadorCorreo correo;
    private final Clock reloj;

    public ServicioNotificaciones(RepositorioNotificaciones notificaciones, RepositorioUsuarios usuarios, EnviadorCorreo correo, Clock reloj) {
        this.notificaciones = notificaciones;
        this.usuarios = usuarios;
        this.correo = correo;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public void notificar(long usuarioId, String tipo, String titulo, String mensaje, String entidad, Long entidadId) {
        notificar(usuarioId, tipo, titulo, mensaje, entidad, entidadId, List.of());
    }

    @Override
    @Transactional
    public void notificar(long usuarioId, String tipo, String titulo, String mensaje, String entidad, Long entidadId, List<Adjunto> adjuntos) {
        notificaciones.guardar(new Notificacion(null, usuarioId, tipo, titulo, recortar(mensaje, 500), false, entidad,
                entidadId, reloj.instant()));
        if (TIPOS_CON_CORREO.contains(tipo)) {
            usuarios.buscarPorId(usuarioId).filter(u -> u.activo() && u.correoVerificado()).ifPresent(u -> despuesDeConfirmar(() -> correo.enviar(
                    new MensajeCorreo(u.correo(), titulo, "notificacion", Map.of("nombre", u.nombres(), "titulo", titulo, "mensaje", mensaje), adjuntos))));
        }
    }

    /** El correo sale solo si la transacción que generó el aviso termina bien (no se avisa de algo que se revirtió). */
    private static void despuesDeConfirmar(Runnable envio) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    envio.run();
                }
            });
        } else {
            envio.run();
        }
    }

    @Override
    @Transactional
    public void notificarAdministradores(String tipo, String titulo, String mensaje, String entidad, Long entidadId) {
        usuarios.listarAdministradores().stream().filter(Usuario::activo)
                .forEach(a -> notificar(a.id(), tipo, titulo, mensaje, entidad, entidadId));
    }

    @Transactional(readOnly = true)
    public Pagina<Notificacion> listar(long usuarioId, boolean soloNoLeidas, SolicitudPagina pagina) {
        return notificaciones.listar(usuarioId, soloNoLeidas, pagina);
    }

    @Transactional(readOnly = true)
    public long contarNoLeidas(long usuarioId) {
        return notificaciones.contarNoLeidas(usuarioId);
    }

    @Transactional
    public void marcarLeida(long usuarioId, long notificacionId) {
        if (!notificaciones.marcarLeida(usuarioId, notificacionId)) {
            throw new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "La notificación no existe.");
        }
    }

    @Transactional
    public int marcarTodasLeidas(long usuarioId) {
        return notificaciones.marcarTodasLeidas(usuarioId);
    }

    private static String recortar(String texto, int maximo) {
        return texto.length() <= maximo ? texto : texto.substring(0, maximo - 1) + "…";
    }
}
