package co.edu.usco.goltime.reservas.aplicacion;

import java.util.List;
import java.util.Optional;

/** Clientes de mostrador: personas sin cuenta para las que el admin crea reservas. */
public interface RepositorioMostrador {

    /**
     * Reutiliza al cliente de mostrador con el mismo teléfono; si no existe, lo crea. Devuelve su id. Nunca reutiliza
     * a alguien atado a un @lid: su teléfono es solo de contacto y no está verificado.
     */
    long buscarOCrear(String nombre, String telefono);

    List<ClienteMostrador> buscar(String texto, int limite);

    /** El cliente sin cuenta con ese teléfono (E.164), si ya existe y no está atado a un @lid. */
    Optional<ClienteMostrador> buscarPorTelefono(String telefono);

    Optional<ClienteMostrador> buscarPorLid(String whatsappLid);

    /** Alguien sin cuenta que escribe desde un @lid: queda atado a él, con un teléfono de contacto. */
    long crearConLid(String nombre, String telefono, String whatsappLid);

    /** El @lid del cliente sin cuenta, si reservó desde uno. */
    Optional<String> lidDe(long id);

    record ClienteMostrador(long id, String nombre, String telefono) {
    }
}
