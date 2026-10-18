package co.edu.usco.goltime.autenticacion.infraestructura;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import co.edu.usco.goltime.autenticacion.dominio.TipoTokenVerificacion;

interface RepositorioTokensVerificacionJpa extends JpaRepository<TokenVerificacionEntidad, Long> {

    Optional<TokenVerificacionEntidad> findByTokenHash(String hash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update TokenVerificacionEntidad t set t.usadoEn = :ahora "
            + "where t.usuarioId = :usuario and t.tipo = :tipo and t.usadoEn is null")
    int invalidarPendientes(long usuario, TipoTokenVerificacion tipo, Instant ahora);
}
