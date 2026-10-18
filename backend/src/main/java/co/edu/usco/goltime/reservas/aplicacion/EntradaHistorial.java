package co.edu.usco.goltime.reservas.aplicacion;

import java.time.Instant;
import java.util.Map;

import co.edu.usco.goltime.compartido.dominio.Rol;

/**
 * Una línea del historial de una reserva: quién hizo qué, cuándo, por qué y los datos antes y después.
 * {@code actorNombre} solo se llena al consultar.
 */
public record EntradaHistorial(Long id, long reservaId, String accion, Long actorId, String actorNombre, Rol actorRol,
        String motivo, Map<String, Object> datosAnteriores, Map<String, Object> datosNuevos, Instant fecha) {
}
