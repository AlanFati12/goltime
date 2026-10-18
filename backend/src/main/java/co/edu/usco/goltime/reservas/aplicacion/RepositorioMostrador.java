package co.edu.usco.goltime.reservas.aplicacion;

import java.util.List;

/** Clientes de mostrador: personas sin cuenta para las que el admin crea reservas. */
public interface RepositorioMostrador {

    /** Reutiliza al cliente de mostrador con el mismo teléfono; si no existe, lo crea. Devuelve su id. */
    long buscarOCrear(String nombre, String telefono);

    List<ClienteMostrador> buscar(String texto, int limite);

    record ClienteMostrador(long id, String nombre, String telefono) {
    }
}
