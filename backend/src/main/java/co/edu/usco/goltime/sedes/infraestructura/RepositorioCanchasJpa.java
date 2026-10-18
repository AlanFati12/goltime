package co.edu.usco.goltime.sedes.infraestructura;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface RepositorioCanchasJpa extends JpaRepository<CanchaEntidad, Long> {

    List<CanchaEntidad> findAllByOrderBySedeIdAscOrdenAscNombreAsc();

    List<CanchaEntidad> findBySedeIdOrderByOrdenAscNombreAsc(long sedeId);

    @Query("select count(c) > 0 from CanchaEntidad c where c.sedeId = :sedeId and lower(c.nombre) = lower(:nombre) and (:excluir is null or c.id <> :excluir)")
    boolean existeNombre(long sedeId, String nombre, Long excluir);

    @Query("select c from CanchaEntidad c where :componente member of c.componentes")
    List<CanchaEntidad> compuestasQueUsan(Long componente);
}
