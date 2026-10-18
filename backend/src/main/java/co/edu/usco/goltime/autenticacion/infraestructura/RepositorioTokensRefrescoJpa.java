package co.edu.usco.goltime.autenticacion.infraestructura;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

interface RepositorioTokensRefrescoJpa extends JpaRepository<TokenRefrescoEntidad, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TokenRefrescoEntidad t where t.tokenHash = :hash")
    Optional<TokenRefrescoEntidad> buscarPorHashBloqueando(String hash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update TokenRefrescoEntidad t set t.revocado = true where t.familiaId = :familia and t.revocado = false")
    int revocarFamilia(UUID familia);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update TokenRefrescoEntidad t set t.revocado = true where t.usuarioId = :usuario and t.revocado = false")
    int revocarTodosDe(long usuario);
}
