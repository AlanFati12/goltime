package co.edu.usco.goltime.notificaciones.dominio;

import java.time.Instant;

/** Notificación in-app de un usuario; puede enlazar a una entidad (ej. una reserva). */
public record Notificacion(Long id, long usuarioId, String tipo, String titulo, String mensaje, boolean leida,
        String entidad, Long entidadId, Instant creadoEn) {
}
