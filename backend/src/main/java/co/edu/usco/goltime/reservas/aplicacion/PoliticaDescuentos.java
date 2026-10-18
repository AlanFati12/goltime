package co.edu.usco.goltime.reservas.aplicacion;

import java.time.Instant;
import java.util.Optional;

/**
 * Puerto por el que las reservas piden un descuento sin conocer de dónde viene (promociones o premios). Lo implementa el módulo
 * de promociones. Un descuento nunca supera el precio base y solo se aplica uno por reserva.
 */
public interface PoliticaDescuentos {

    enum Origen {
        PROMOCION, PREMIO
    }

    /**
     * @param codigo  código de promoción escrito por el cliente (puede ser nulo)
     * @param premioId premio de fidelidad que el cliente quiere usar (puede ser nulo)
     * @param primeraReserva el cliente no tiene reservas previas sin cancelar
     */
    record Consulta(long clienteId, long sedeId, long canchaId, Instant inicio, Instant fin, int precioBase, String codigo, Long premioId,
            boolean primeraReserva) {
    }

    record Descuento(int monto, Origen origen, long referenciaId, String descripcion) {
    }

    /**
     * Calcula el descuento sin consumirlo. Si el cliente escribió un código o eligió un premio que no es válido responde con
     * un error de negocio; sin ellos aplica la mejor promoción automática, si la hay.
     */
    Optional<Descuento> evaluar(Consulta consulta);

    /** Marca el descuento como usado por la reserva. Vuelve a validar (cupos) y falla si ya no es aplicable. */
    void consumir(Descuento descuento, long reservaId, long clienteId);

    /** Reservas completadas del cliente que cuentan para fidelidad (sin partidos de torneo). */
    interface Fidelidad {
        int reservasCompletadas(long clienteId);

        /** Reservas del cliente que siguen vigentes o se jugaron (no canceladas, rechazadas ni expiradas). */
        int reservasNoCanceladas(long clienteId);
    }
}
