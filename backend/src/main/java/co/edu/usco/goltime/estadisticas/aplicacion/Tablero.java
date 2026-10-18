package co.edu.usco.goltime.estadisticas.aplicacion;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Lo que el administrador necesita ver al abrir el panel: el día de hoy, el dinero por cobrar, los próximos días, el estado de las
 * canchas en este momento, los pendientes de torneos y los clientes. Todo se calcula en el servidor con la hora de Bogotá.
 */
public final class Tablero {

    private Tablero() {
    }

    /** Reservas de hoy (sin las de torneo, que van aparte). {@code valorReservado} suma las confirmadas, completadas y no asistidas. */
    public record Hoy(int pendientes, int confirmadas, int completadas, int noAsistio, int canceladas, int partidosTorneo, long valorReservado,
            long recaudado, double horasReservadas) {
    }

    /** Reservas ya jugadas con saldo, y dinero que hay que devolver a clientes por cancelaciones con pago. */
    public record Cobros(int reservasConSaldo, long saldoPendiente, int reembolsosPendientes, long montoReembolsos, List<ReservaConSaldo> mayoresSaldos) {
    }

    public record ReservaConSaldo(long reservaId, String codigo, String cliente, Instant inicio, long saldo) {
    }

    /** Un día de la próxima semana: reservas activas (pendientes + confirmadas) y horas confirmadas. */
    public record DiaProximo(LocalDate fecha, int reservas, int pendientes, double horasConfirmadas) {
    }

    /** Estado de una cancha ahora mismo. */
    public record EstadoCancha(long canchaId, String cancha, String sede, String estado, boolean enUso, Instant ocupadaHasta, Instant proximoInicio,
            int reservasHoy) {
    }

    /** Tareas pendientes de los torneos. */
    public record Torneos(int enCurso, int conInscripcionesAbiertas, int solicitudesPendientes, int partidosSinProgramar, int partidosSinResultado) {
    }

    public record Clientes(int activos, int nuevosSemana, int nuevosMes) {
    }

    public record Resumen(Instant generadoEn, Hoy hoy, int pendientesPorVencer, Cobros cobros, List<DiaProximo> proximosDias, List<EstadoCancha> canchas,
            Torneos torneos, Clientes clientes) {
    }

    /** Puerto de lectura del tablero. Los límites del día y del mes llegan ya calculados en la hora de Bogotá. */
    public interface Consultas {

        Hoy hoy(Instant inicioDia, Instant finDia);

        int pendientesPorVencer(Instant ahora, Instant limite);

        Cobros cobros(Instant ahora, int limite);

        List<DiaProximo> proximosDias(LocalDate desde, int dias);

        List<EstadoCancha> canchas(Instant ahora, Instant inicioDia, Instant finDia);

        Torneos torneos(Instant ahora);

        Clientes clientes(Instant haceUnaSemana, Instant inicioMes);
    }
}
