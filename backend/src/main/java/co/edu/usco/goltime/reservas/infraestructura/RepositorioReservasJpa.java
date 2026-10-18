package co.edu.usco.goltime.reservas.infraestructura;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import co.edu.usco.goltime.reservas.dominio.EstadoReserva;

interface RepositorioReservasJpa extends JpaRepository<ReservaEntidad, Long> {

    Optional<ReservaEntidad> findByClienteIdAndClaveIdempotencia(long clienteId, String clave);

    @Query("select count(r) from ReservaEntidad r where r.clienteId = :cliente and r.estado in :estados and r.fin > :ahora")
    int contarFuturas(long cliente, Collection<EstadoReserva> estados, Instant ahora);

    @Query("select r.id from ReservaEntidad r where r.clienteId = :cliente and r.estado in :estados and r.fin > :ahora order by r.inicio")
    List<Long> idsFuturas(long cliente, Collection<EstadoReserva> estados, Instant ahora);

    @Query("select r from ReservaEntidad r where r.clienteId = :cliente and r.estado in :estados and r.fin > :ahora")
    Page<ReservaEntidad> proximas(long cliente, Collection<EstadoReserva> estados, Instant ahora, Pageable pagina);

    @Query("select r from ReservaEntidad r where r.clienteId = :cliente "
            + "and (r.estado in :jugadas or (r.estado = :confirmada and r.fin <= :ahora))")
    Page<ReservaEntidad> historial(long cliente, Collection<EstadoReserva> jugadas, EstadoReserva confirmada, Instant ahora, Pageable pagina);

    @Query("select r from ReservaEntidad r where r.clienteId = :cliente and r.estado in :estados")
    Page<ReservaEntidad> conEstados(long cliente, Collection<EstadoReserva> estados, Pageable pagina);

    @Query("select r.id from ReservaEntidad r where r.estado = :estado and r.expiraEn <= :ahora order by r.expiraEn")
    List<Long> idsPendientesVencidas(EstadoReserva estado, Instant ahora, Pageable limite);

    @Query("select r.id from ReservaEntidad r where r.estado = :estado and r.fin <= :ahora order by r.fin")
    List<Long> idsTerminadas(EstadoReserva estado, Instant ahora, Pageable limite);

    @Query("select r.id from ReservaEntidad r where r.estado = :estado and r.inicio > :ahora and r.inicio <= :hasta "
            + "and r.recordatorioEnviadoEn is null order by r.inicio")
    List<Long> idsPorRecordar(EstadoReserva estado, Instant ahora, Instant hasta, Pageable limite);
}
