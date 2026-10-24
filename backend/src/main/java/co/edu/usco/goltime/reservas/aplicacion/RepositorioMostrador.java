package co.edu.usco.goltime.reservas.aplicacion;

import java.util.List;
import java.util.Optional;

/** Clientes de mostrador: personas sin cuenta para las que el admin crea reservas. */
public interface RepositorioMostrador {

    /** Reutiliza al cliente de mostrador con el mismo teléfono; si no existe, lo crea. Devuelve su id. */
    long buscarOCrear(String nombre, String telefono);

    List<ClienteMostrador> buscar(String texto, int limite);

    /** El cliente sin cuenta con ese teléfono (E.164), si ya existe. */
    Optional<ClienteMostrador> buscarPorTelefono(String telefono);

    record ClienteMostrador(long id, String nombre, String telefono) {
    }
}
