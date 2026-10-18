package co.edu.usco.goltime.reservas.aplicacion;

import java.util.List;

import co.edu.usco.goltime.reservas.dominio.Reserva;

/** Vistas de lectura de reservas con los datos relacionados y las acciones que permite la política. */
public final class Vistas {

    private Vistas() {
    }

    /**
     * @param historial nulo en listados; presente en el detalle
     */
    public record VistaReserva(Reserva reserva, Relacionados relacionados, boolean puedeCancelar,
            boolean puedeReprogramar, boolean cancelacionSeriaTardia, List<EntradaHistorial> historial) {
    }

    public record ResultadoSolicitud(VistaReserva vista, boolean creada) {
    }

    public record PrevisualizacionReprogramacion(boolean disponible, int precioActual, int precioNuevo, int diferencia) {
    }

    public record Slot(java.time.Instant inicio, java.time.Instant fin, EstadoSlot estado, long precio) {
    }

    public enum EstadoSlot {
        DISPONIBLE, OCUPADO, BLOQUEADO, NO_RESERVABLE
    }

    public record DisponibilidadCancha(long canchaId, String nombre, String modalidad, long sedeId, String sedeNombre,
            int duracionBloqueMin, int duracionMinMin, int duracionMaxMin, boolean cerrado, List<Slot> slots) {
    }

    public record DisponibilidadDia(java.time.LocalDate fecha, List<DisponibilidadCancha> canchas) {
    }

    public record ResumenDia(java.time.LocalDate fecha, int horariosLibres) {
    }
}
