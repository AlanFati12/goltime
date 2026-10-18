package co.edu.usco.goltime.notificaciones.aplicacion;

import java.util.List;

/**
 * Puerto de entrada para que otros módulos avisen a los usuarios. Hoy deja una notificación in-app;
 * el diseño admite sumar correo, WhatsApp o SMS sin tocar a quienes notifican.
 */
public interface Notificador {

    void notificar(long usuarioId, String tipo, String titulo, String mensaje, String entidad, Long entidadId);

    /** Igual, con archivos que viajan solo por correo (por ejemplo el .ics de una reserva confirmada). */
    default void notificar(long usuarioId, String tipo, String titulo, String mensaje, String entidad, Long entidadId, List<EnviadorCorreo.Adjunto> adjuntos) {
        notificar(usuarioId, tipo, titulo, mensaje, entidad, entidadId);
    }

    /** Notifica a todos los administradores activos. */
    void notificarAdministradores(String tipo, String titulo, String mensaje, String entidad, Long entidadId);
}
