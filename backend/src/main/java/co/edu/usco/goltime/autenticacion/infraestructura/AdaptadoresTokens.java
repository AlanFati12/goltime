package co.edu.usco.goltime.autenticacion.infraestructura;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import co.edu.usco.goltime.autenticacion.aplicacion.RepositorioTokensRefresco;
import co.edu.usco.goltime.autenticacion.dominio.TokenRefresco;

/** Adaptadores JPA de los puertos de tokens. */
final class AdaptadoresTokens {

    private AdaptadoresTokens() {
    }

    @Repository
    static class Refresco implements RepositorioTokensRefresco {

        private final RepositorioTokensRefrescoJpa jpa;

        Refresco(RepositorioTokensRefrescoJpa jpa) {
            this.jpa = jpa;
        }

        @Override
        public TokenRefresco guardar(TokenRefresco token) {
            TokenRefrescoEntidad e = token.id() == null ? new TokenRefrescoEntidad() : jpa.findById(token.id()).orElseThrow();
            e.usuarioId = token.usuarioId();
            e.familiaId = token.familiaId();
            e.tokenHash = token.tokenHash();
            e.expiraEn = token.expiraEn();
            e.revocado = token.revocado();
            e.reemplazadoPor = token.reemplazadoPor();
            e.userAgent = token.userAgent();
            e.ip = token.ip();
            e.recordar = token.recordar();
            return aDominio(jpa.saveAndFlush(e));
        }

        @Override
        public Optional<TokenRefresco> buscarPorHashBloqueando(String hash) {
            return jpa.buscarPorHashBloqueando(hash).map(Refresco::aDominio);
        }

        @Override
        public void revocarFamilia(UUID familiaId) {
            jpa.revocarFamilia(familiaId);
        }

        @Override
        public void revocarTodosDe(long usuarioId) {
            jpa.revocarTodosDe(usuarioId);
        }

        private static TokenRefresco aDominio(TokenRefrescoEntidad e) {
            return new TokenRefresco(e.id, e.usuarioId, e.familiaId, e.tokenHash, e.expiraEn, e.recordar,
                    e.userAgent, e.ip, e.revocado, e.reemplazadoPor);
        }
    }
}
