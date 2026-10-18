package co.edu.usco.goltime.autenticacion.dominio;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Tokens opacos: se generan aleatorios (256 bits) y solo se guarda su hash SHA-256, de modo que
 * una filtración de la base de datos no permite usarlos.
 */
public final class GeneradorTokens {

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private GeneradorTokens() {
    }

    public static String generar() {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 en hexadecimal (64 caracteres). */
    public static String hash(String token) {
        try {
            byte[] resumen = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumen);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }
}
