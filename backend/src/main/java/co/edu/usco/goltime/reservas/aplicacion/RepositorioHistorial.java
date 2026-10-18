package co.edu.usco.goltime.reservas.aplicacion;

import java.util.List;

public interface RepositorioHistorial {

    void registrar(EntradaHistorial entrada);

    /** Historial en orden cronológico, con el nombre del actor. */
    List<EntradaHistorial> deReserva(long reservaId);
}
