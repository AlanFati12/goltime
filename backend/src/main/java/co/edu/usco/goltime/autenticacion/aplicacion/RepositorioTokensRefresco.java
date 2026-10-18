package co.edu.usco.goltime.autenticacion.aplicacion;

import java.util.Optional;
import java.util.UUID;

import co.edu.usco.goltime.autenticacion.dominio.TokenRefresco;

public interface RepositorioTokensRefresco {

    TokenRefresco guardar(TokenRefresco token);

    /** Busca por hash tomando un bloqueo de escritura para serializar renovaciones simultáneas. */
    Optional<TokenRefresco> buscarPorHashBloqueando(String hash);

    void revocarFamilia(UUID familiaId);

    void revocarTodosDe(long usuarioId);
}
