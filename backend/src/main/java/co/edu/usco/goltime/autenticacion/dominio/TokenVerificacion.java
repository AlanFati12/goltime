package co.edu.usco.goltime.autenticacion.dominio;

import java.time.Instant;

/** Token de un solo uso para verificar el correo o restablecer la contraseña (vence a los 30 min). */
public class TokenVerificacion {

    private final Long id;
    private final long usuarioId;
    private final TipoTokenVerificacion tipo;
    private final String tokenHash;
    private final Instant expiraEn;
    private Instant usadoEn;

    public TokenVerificacion(Long id, long usuarioId, TipoTokenVerificacion tipo, String tokenHash,
            Instant expiraEn, Instant usadoEn) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.tipo = tipo;
        this.tokenHash = tokenHash;
        this.expiraEn = expiraEn;
        this.usadoEn = usadoEn;
    }

    public static TokenVerificacion nuevo(long usuarioId, TipoTokenVerificacion tipo, String tokenHash, Instant expiraEn) {
        return new TokenVerificacion(null, usuarioId, tipo, tokenHash, expiraEn, null);
    }

    public boolean estaVigente(Instant ahora) {
        return usadoEn == null && expiraEn.isAfter(ahora);
    }

    public void marcarUsado(Instant ahora) {
        this.usadoEn = ahora;
    }

    public Long id() { return id; }
    public long usuarioId() { return usuarioId; }
    public TipoTokenVerificacion tipo() { return tipo; }
    public String tokenHash() { return tokenHash; }
    public Instant expiraEn() { return expiraEn; }
    public Instant usadoEn() { return usadoEn; }
}
