package co.edu.usco.goltime.sedes.infraestructura;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

interface RepositorioTarifasJpa extends JpaRepository<TarifaEntidad, Long> {

    List<TarifaEntidad> findByCanchaIdOrderByMinutoInicioAscIdAsc(long canchaId);
}
