package co.edu.usco.goltime.sedes.aplicacion;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import co.edu.usco.goltime.sedes.dominio.Festivo;

public interface RepositorioFestivos {

    List<Festivo> delAnio(int anio);

    /** Fechas festivas dentro del rango (ambos extremos incluidos). */
    Set<LocalDate> fechasEntre(LocalDate desde, LocalDate hasta);

    boolean existe(LocalDate fecha);

    Optional<Festivo> buscarPorId(long id);

    Festivo guardar(Festivo festivo);

    void eliminar(long id);
}
