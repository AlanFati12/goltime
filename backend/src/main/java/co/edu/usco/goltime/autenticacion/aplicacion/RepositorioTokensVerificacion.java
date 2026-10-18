package co.edu.usco.goltime.autenticacion.aplicacion;

import java.time.Instant;
import java.util.Optional;

import co.edu.usco.goltime.autenticacion.dominio.TipoTokenVerificacion;
import co.edu.usco.goltime.autenticacion.dominio.TokenVerificacion;

public interface RepositorioTokensVerificacion {

    TokenVerificacion guardar(TokenVerificacion token);

    Optional<TokenVerificacion> buscarPorHash(String hash);

    /** Anula los tokens pendientes del mismo tipo para que solo el último enlace sea válido. */
    void invalidarPendientes(long usuarioId, TipoTokenVerificacion tipo, Instant ahora);
}
