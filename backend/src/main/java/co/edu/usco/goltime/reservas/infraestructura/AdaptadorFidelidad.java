package co.edu.usco.goltime.reservas.infraestructura;

import org.springframework.stereotype.Component;

import co.edu.usco.goltime.reservas.aplicacion.PoliticaDescuentos;
import co.edu.usco.goltime.reservas.aplicacion.RepositorioReservas;

/** Expone a los premios cuántas reservas completadas lleva un cliente, sin que conozcan las tablas de reservas. */
@Component
class AdaptadorFidelidad implements PoliticaDescuentos.Fidelidad {

    private final RepositorioReservas reservas;

    AdaptadorFidelidad(RepositorioReservas reservas) {
        this.reservas = reservas;
    }

    @Override
    public int reservasCompletadas(long clienteId) {
        return reservas.contarCompletadas(clienteId);
    }

    @Override
    public int reservasNoCanceladas(long clienteId) {
        return reservas.contarNoCanceladas(clienteId);
    }
}
