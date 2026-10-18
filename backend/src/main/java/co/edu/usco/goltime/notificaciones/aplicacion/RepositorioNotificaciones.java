package co.edu.usco.goltime.notificaciones.aplicacion;

import co.edu.usco.goltime.compartido.dominio.Pagina;
import co.edu.usco.goltime.compartido.dominio.SolicitudPagina;
import co.edu.usco.goltime.notificaciones.dominio.Notificacion;

public interface RepositorioNotificaciones {

    void guardar(Notificacion notificacion);

    Pagina<Notificacion> listar(long usuarioId, boolean soloNoLeidas, SolicitudPagina pagina);

    long contarNoLeidas(long usuarioId);

    /** Marca como leída solo si pertenece al usuario. Devuelve si existía. */
    boolean marcarLeida(long usuarioId, long notificacionId);

    int marcarTodasLeidas(long usuarioId);
}
