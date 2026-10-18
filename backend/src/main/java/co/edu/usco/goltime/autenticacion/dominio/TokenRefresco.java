package co.edu.usco.goltime.autenticacion.dominio;

import java.time.Instant;
import java.util.UUID;

/**
 * Refresh token (guardado como hash). Los tokens de una misma cadena de rotación comparten
 * {@code familiaId}: si se reutiliza uno ya rotado se revoca toda la familia.
 */
public class TokenRefresco {

    private Long id;
    private final long usuarioId;
    private final UUID familiaId;
    private final String tokenHash;
    private final Instant expiraEn;
    private final boolean recordar;
    private final String userAgent;
    private final String ip;
    private boolean revocado;
    private Long reemplazadoPor;

    public TokenRefresco(Long id, long usuarioId, UUID familiaId, String tokenHash, Instant expiraEn,
            boolean recordar, String userAgent, String ip, boolean revocado, Long reemplazadoPor) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.familiaId = familiaId;
        this.tokenHash = tokenHash;
        this.expiraEn = expiraEn;
        this.recordar = recordar;
        this.userAgent = userAgent;
        this.ip = ip;
        this.revocado = revocado;
        this.reemplazadoPor = reemplazadoPor;
    }

    public static TokenRefresco nuevo(long usuarioId, UUID familiaId, String tokenHash, Instant expiraEn,
            boolean recordar, String userAgent, String ip) {
        return new TokenRefresco(null, usuarioId, familiaId, tokenHash, expiraEn, recordar, userAgent, ip, false, null);
    }

    public boolean estaExpirado(Instant ahora) {
        return !expiraEn.isAfter(ahora);
    }

    /** Un token ya rotado o revocado que vuelve a presentarse indica robo o reutilización. */
    public boolean fueUsado() {
        return revocado || reemplazadoPor != null;
    }

    public void rotarA(long idNuevo) {
        this.revocado = true;
        this.reemplazadoPor = idNuevo;
    }

    public void revocar() {
        this.revocado = true;
    }

    public Long id() { return id; }
    public long usuarioId() { return usuarioId; }
    public UUID familiaId() { return familiaId; }
    public String tokenHash() { return tokenHash; }
    public Instant expiraEn() { return expiraEn; }
    public boolean recordar() { return recordar; }
    public String userAgent() { return userAgent; }
    public String ip() { return ip; }
    public boolean revocado() { return revocado; }
    public Long reemplazadoPor() { return reemplazadoPor; }
}
