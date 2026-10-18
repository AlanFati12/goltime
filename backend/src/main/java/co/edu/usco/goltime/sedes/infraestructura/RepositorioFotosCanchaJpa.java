package co.edu.usco.goltime.sedes.infraestructura;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

interface RepositorioFotosCanchaJpa extends JpaRepository<FotoCanchaEntidad, Long> {

    List<FotoCanchaEntidad> findByCanchaIdOrderByOrdenAscIdAsc(long canchaId);

    Optional<FotoCanchaEntidad> findByIdAndCanchaId(long id, long canchaId);
}
