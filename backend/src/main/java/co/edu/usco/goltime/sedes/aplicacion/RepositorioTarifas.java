package co.edu.usco.goltime.sedes.aplicacion;

import java.util.List;
import java.util.Optional;

import co.edu.usco.goltime.sedes.dominio.Tarifa;

public interface RepositorioTarifas {

    List<Tarifa> deCancha(long canchaId);

    Optional<Tarifa> buscarPorId(long id);

    Tarifa guardar(Tarifa tarifa);

    void eliminar(long id);
}
