package co.edu.usco.goltime.notificaciones.aplicacion;

/**
 * Puerto de entrada para que otros módulos avisen a los usuarios. Hoy deja una notificación in-app;
 * el diseño admite sumar correo, WhatsApp o SMS sin tocar a quienes notifican.
 */
public interface Notificador {

    void notificar(long usuarioId, String tipo, String titulo, String mensaje, String entidad, Long entidadId);

    /** Notifica a todos los administradores activos. */
    void notificarAdministradores(String tipo, String titulo, String mensaje, String entidad, Long entidadId);
}
