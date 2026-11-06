package co.edu.usco.goltime.sedes.aplicacion;

import java.util.List;
import java.util.Optional;

import co.edu.usco.goltime.sedes.dominio.Tarifa;

public interface RepositorioTarifas {

    List<Tarifa> deCancha(long canchaId);

    /** Tarifas de varias canchas en una sola consulta (clave: id de cancha; sin entrada si no tiene tarifas). */
    java.util.Map<Long, List<Tarifa>> deCanchas(java.util.Collection<Long> canchaIds);

    Optional<Tarifa> buscarPorId(long id);

    Tarifa guardar(Tarifa tarifa);

    void eliminar(long id);
}
