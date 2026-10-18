package co.edu.usco.goltime.reservas.aplicacion;

import java.util.List;

import co.edu.usco.goltime.reservas.dominio.Pago;

public interface RepositorioPagos {

    Pago registrar(Pago pago);

    /** Movimientos de la reserva en orden cronológico, con el nombre de quien los registró. */
    List<PagoRegistrado> deReserva(long reservaId);

    record PagoRegistrado(Pago pago, String registradoPorNombre) {
    }
}
