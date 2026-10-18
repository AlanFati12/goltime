package co.edu.usco.goltime.reservas.aplicacion;

import java.util.List;
import java.util.Optional;

import co.edu.usco.goltime.reservas.dominio.Bloqueo;

public interface RepositorioBloqueos {

    /** Inserta (id nulo) o actualiza el bloqueo y sus canchas elegidas. */
    Bloqueo guardar(Bloqueo bloqueo);

    Optional<Bloqueo> buscarPorId(long id);

    List<Bloqueo> listar(Long sedeId, boolean soloActivos);
}
