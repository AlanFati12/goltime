package co.edu.usco.goltime.autenticacion.dominio;

import java.time.Duration;
import java.time.Instant;

/**
 * Código de un solo uso que se envía al correo de una cuenta para probar que quien escribe desde un
 * número de WhatsApp también es dueño de la cuenta (ADR 0018). Solo se guarda su hash.
 */
public class VerificacionTelefono {

    public static final Duration VIGENCIA = Duration.ofMinutes(10);
    public static final int MAX_INTENTOS = 5;

    private final Long id;
    private final long usuarioId;
    private final String telefono;
    private final String whatsappLid;
    private final String codigoHash;
    private int intentos;
    private final Instant expiraEn;
    private Instant usadoEn;

    /** Exactamente uno de {@code telefono} y {@code whatsappLid}: desde dónde se pidió el código. */
    public VerificacionTelefono(Long id, long usuarioId, String telefono, String whatsappLid, String codigoHash, int intentos, Instant expiraEn,
            Instant usadoEn) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.telefono = telefono;
        this.whatsappLid = whatsappLid;
        this.codigoHash = codigoHash;
        this.intentos = intentos;
        this.expiraEn = expiraEn;
        this.usadoEn = usadoEn;
    }

    public static VerificacionTelefono nueva(long usuarioId, String telefono, String codigoHash, Instant ahora) {
        return new VerificacionTelefono(null, usuarioId, telefono, null, codigoHash, 0, ahora.plus(VIGENCIA), null);
    }

    /** Código pedido desde un chat sin teléfono visible (@lid), con el correo de la cuenta. */
    public static VerificacionTelefono nuevaDesdeLid(long usuarioId, String whatsappLid, String codigoHash, Instant ahora) {
        return new VerificacionTelefono(null, usuarioId, null, whatsappLid, codigoHash, 0, ahora.plus(VIGENCIA), null);
    }

    public boolean estaVigente(Instant ahora) {
        return usadoEn == null && intentos < MAX_INTENTOS && expiraEn.isAfter(ahora);
    }

    /** Un código equivocado gasta un intento; al quinto el código deja de servir. */
    public void registrarIntentoFallido() {
        intentos++;
    }

    public void marcarUsada(Instant ahora) {
        this.usadoEn = ahora;
    }

    public Long id() { return id; }
    public long usuarioId() { return usuarioId; }
    public String telefono() { return telefono; }
    public String whatsappLid() { return whatsappLid; }
    public String codigoHash() { return codigoHash; }
    public int intentos() { return intentos; }
    public Instant expiraEn() { return expiraEn; }
    public Instant usadoEn() { return usadoEn; }
}
