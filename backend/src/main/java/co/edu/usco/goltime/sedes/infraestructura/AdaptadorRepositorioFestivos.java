package co.edu.usco.goltime.sedes.infraestructura;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import co.edu.usco.goltime.sedes.aplicacion.RepositorioFestivos;
import co.edu.usco.goltime.sedes.dominio.Festivo;

@Repository
class AdaptadorRepositorioFestivos implements RepositorioFestivos {

    private final RepositorioFestivosJpa jpa;

    AdaptadorRepositorioFestivos(RepositorioFestivosJpa jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<Festivo> delAnio(int anio) {
        return jpa.findByFechaBetweenOrderByFechaAsc(LocalDate.of(anio, 1, 1), LocalDate.of(anio, 12, 31))
                .stream().map(AdaptadorRepositorioFestivos::aDominio).toList();
    }

    @Override
    public Set<LocalDate> fechasEntre(LocalDate desde, LocalDate hasta) {
        return jpa.findByFechaBetweenOrderByFechaAsc(desde, hasta).stream().map(f -> f.fecha).collect(Collectors.toSet());
    }

    @Override
    public boolean existe(LocalDate fecha) {
        return jpa.existsByFecha(fecha);
    }

    @Override
    public Optional<Festivo> buscarPorId(long id) {
        return jpa.findById(id).map(AdaptadorRepositorioFestivos::aDominio);
    }

    @Override
    public Festivo guardar(Festivo festivo) {
        FestivoEntidad e = new FestivoEntidad();
        e.fecha = festivo.fecha();
        e.nombre = festivo.nombre();
        return aDominio(jpa.saveAndFlush(e));
    }

    @Override
    public void eliminar(long id) {
        jpa.deleteById(id);
    }

    private static Festivo aDominio(FestivoEntidad e) {
        return new Festivo(e.id, e.fecha, e.nombre);
    }
}
