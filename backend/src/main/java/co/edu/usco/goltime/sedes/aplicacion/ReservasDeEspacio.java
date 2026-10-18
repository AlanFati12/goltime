package co.edu.usco.goltime.sedes.aplicacion;

/**
 * Puerto hacia el módulo de reservas (Fase 3): permite previsualizar y cancelar las reservas futuras
 * afectadas al desactivar una sede o sacar una cancha de servicio, sin que {@code sedes} dependa de
 * {@code reservas}.
 */
public interface ReservasDeEspacio {

    int contarFuturasActivasEnCancha(long canchaId);

    int contarFuturasActivasEnSede(long sedeId);

    /** Cancela las reservas futuras activas y devuelve cuántas canceló. */
    int cancelarFuturasEnCancha(long canchaId, String motivo);

    int cancelarFuturasEnSede(long sedeId, String motivo);
}
