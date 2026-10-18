package co.edu.usco.goltime.usuarios.aplicacion;

/**
 * Puerto hacia el módulo de reservas (Fase 3): permite previsualizar y cancelar las reservas
 * futuras de un cliente al desactivarlo, sin que {@code usuarios} dependa de {@code reservas}.
 */
public interface ReservasDeUsuario {

    int contarFuturasActivas(long usuarioId);

    /** Cancela las reservas futuras activas y devuelve cuántas canceló. */
    int cancelarFuturas(long usuarioId, String motivo);
}
