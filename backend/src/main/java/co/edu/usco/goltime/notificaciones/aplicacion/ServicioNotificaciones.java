package co.edu.usco.goltime.notificaciones.aplicacion;

import java.time.Clock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.compartido.dominio.Pagina;
import co.edu.usco.goltime.compartido.dominio.SolicitudPagina;
import co.edu.usco.goltime.notificaciones.dominio.Notificacion;
import co.edu.usco.goltime.usuarios.aplicacion.RepositorioUsuarios;
import co.edu.usco.goltime.usuarios.dominio.Usuario;

/** Notificaciones in-app: crear (desde otros módulos) y consultar o marcar como leídas (por su dueño). */
@Service
public class ServicioNotificaciones implements Notificador {

    private final RepositorioNotificaciones notificaciones;
    private final RepositorioUsuarios usuarios;
    private final Clock reloj;

    public ServicioNotificaciones(RepositorioNotificaciones notificaciones, RepositorioUsuarios usuarios, Clock reloj) {
        this.notificaciones = notificaciones;
        this.usuarios = usuarios;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public void notificar(long usuarioId, String tipo, String titulo, String mensaje, String entidad, Long entidadId) {
        notificaciones.guardar(new Notificacion(null, usuarioId, tipo, titulo, recortar(mensaje, 500), false, entidad,
                entidadId, reloj.instant()));
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
