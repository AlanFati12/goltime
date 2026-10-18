package co.edu.usco.goltime.sedes.infraestructura;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

interface RepositorioFestivosJpa extends JpaRepository<FestivoEntidad, Long> {

    List<FestivoEntidad> findByFechaBetweenOrderByFechaAsc(LocalDate desde, LocalDate hasta);

    boolean existsByFecha(LocalDate fecha);
}
