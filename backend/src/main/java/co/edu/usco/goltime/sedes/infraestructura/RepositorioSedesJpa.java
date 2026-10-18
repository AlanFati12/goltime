package co.edu.usco.goltime.sedes.infraestructura;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface RepositorioSedesJpa extends JpaRepository<SedeEntidad, Long> {

    @Query("select count(s) > 0 from SedeEntidad s where lower(s.nombre) = lower(:nombre) and (:excluir is null or s.id <> :excluir)")
    boolean existeNombre(String nombre, Long excluir);

    List<SedeEntidad> findByActivaTrueOrderByNombreAsc();

    List<SedeEntidad> findAllByOrderByNombreAsc();
}
