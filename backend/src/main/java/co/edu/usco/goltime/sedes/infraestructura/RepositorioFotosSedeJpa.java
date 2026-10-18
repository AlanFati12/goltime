package co.edu.usco.goltime.sedes.infraestructura;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

interface RepositorioFotosSedeJpa extends JpaRepository<FotoSedeEntidad, Long> {

    List<FotoSedeEntidad> findBySedeIdOrderByOrdenAscIdAsc(long sedeId);

    Optional<FotoSedeEntidad> findByIdAndSedeId(long id, long sedeId);
}
