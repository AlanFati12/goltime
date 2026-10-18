package co.edu.usco.goltime.estadisticas.aplicacion;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import co.edu.usco.goltime.estadisticas.dominio.Agrupacion;
import co.edu.usco.goltime.estadisticas.dominio.PeriodoEstadistico;

/** Resultados de las estadísticas del negocio. Todos los valores salen de las reservas y pagos registrados. */
public final class Estadisticas {

    private Estadisticas() {
    }

    /** Filtro común: periodo y, opcionalmente, una sede o una cancha. */
    public record Filtro(PeriodoEstadistico periodo, Long sedeId, Long canchaId) {
        public Filtro conPeriodo(PeriodoEstadistico otro) {
            return new Filtro(otro, sedeId, canchaId);
        }
    }

    /** Totales crudos de las reservas del periodo (sin las de torneo); los porcentajes se derivan en {@link Resumen}. */
    public record TotalesReservas(int total, int completadas, int canceladas, int noAsistio, long ingresosReales, long porCobrar, double horasJugadas,
            int clientesUnicos) {
    }

    /**
     * Indicadores del periodo. {@code ingresosReales} cuenta solo reservas COMPLETADA (regla 6); {@code recaudado} es el dinero
     * efectivamente cobrado en el periodo (pagos menos reembolsos), aunque la reserva sea de otra fecha.
     */
    public record Resumen(long ingresosReales, long recaudado, long porCobrar, int reservas, Map<String, Integer> porEstado, int completadas,
            int canceladas, int noAsistio, double tasaCancelacion, double tasaNoAsistencia, double horasJugadas, long ticketPromedio, int clientesUnicos,
            double ocupacionPct, int partidosTorneo) {
    }

    public record ResumenConComparacion(PeriodoEstadistico periodo, Resumen actual, PeriodoEstadistico periodoAnterior, Resumen anterior,
            Map<String, Double> variaciones) {
    }

    public record PuntoSerie(LocalDate fecha, long ingresos, int completadas, int canceladas) {
    }

    public record Serie(Agrupacion agrupacion, List<PuntoSerie> puntos) {
    }

    public record FilaCancha(long canchaId, String cancha, String sede, int completadas, double horas, long ingresos, double ocupacionPct) {
    }

    /** Reservas que cubren una hora del día; {@code diaSemana} 1 = lunes ... 7 = domingo. */
    public record CeldaPico(int diaSemana, int hora, int reservas) {
    }

    public record HorasPico(List<CeldaPico> celdas, int maximo) {
    }

    public record ClienteFrecuente(String nombre, String contacto, boolean mostrador, int completadas, int canceladas, int noAsistio, long gasto,
            Instant ultimaReserva) {
    }

    // ----- Puerto de lectura -----

    /** Consultas agregadas de solo lectura. Los minutos son por cancha; la ocupación se arma en el servicio. */
    public interface Consultas {

        TotalesReservas totales(Filtro filtro);

        Map<String, Integer> reservasPorEstado(Filtro filtro);

        long recaudado(Filtro filtro);

        int partidosDeTorneo(Filtro filtro);

        /** Minutos de reservas CONFIRMADA, COMPLETADA o NO_ASISTIO que caen dentro del periodo, por cancha. */
        Map<Long, Double> minutosOcupados(Filtro filtro);

        /** Minutos que cada cancha estuvo abierta en el periodo según su horario efectivo (el propio o el de la sede). */
        Map<Long, Double> minutosDisponibles(Filtro filtro);

        List<PuntoSerie> serie(Filtro filtro, Agrupacion agrupacion);

        List<FilaCancha> porCancha(Filtro filtro);

        List<CeldaPico> horasPico(Filtro filtro);

        List<ClienteFrecuente> clientesFrecuentes(Filtro filtro, int limite);
    }
}
