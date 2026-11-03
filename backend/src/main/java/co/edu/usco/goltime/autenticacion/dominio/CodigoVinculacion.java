package co.edu.usco.goltime.autenticacion.dominio;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

/**
 * Código para vincular el WhatsApp con la cuenta (ADR 0018). El cliente lo genera en su perfil y se lo envía al bot.
 * Seis caracteres sin los que se confunden (0/O, 1/I/L): unos 887 millones de combinaciones, de un solo uso y por
 * 10 minutos, con pocos intentos por WhatsApp; adivinar uno ajeno no es práctico.
 */
public final class CodigoVinculacion {

    public static final Duration VIGENCIA = Duration.ofMinutes(10);
    public static final int LONGITUD = 6;
    static final String ALFABETO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private CodigoVinculacion() {
    }

    public static String generar() {
        StringBuilder codigo = new StringBuilder(LONGITUD);
        for (int i = 0; i < LONGITUD; i++) {
            codigo.append(ALFABETO.charAt(ALEATORIO.nextInt(ALFABETO.length())));
        }
        return codigo.toString();
    }

    /** Lo que escribió la persona («x7k 2pq», «X7K-2PQ») en su forma canónica, o vacío si no puede ser un código. */
    public static Optional<String> normalizar(String texto) {
        if (texto == null) {
            return Optional.empty();
        }
        String limpio = texto.toUpperCase(Locale.ROOT).replaceAll("[\\s\\-.]", "");
        if (limpio.length() != LONGITUD || !limpio.chars().allMatch(c -> ALFABETO.indexOf(c) >= 0)) {
            return Optional.empty();
        }
        return Optional.of(limpio);
    }

    /** Hash que se guarda (nunca el código). */
    public static String hash(String codigoCanonico) {
        return GeneradorTokens.hash("vinculacion-whatsapp:" + codigoCanonico);
    }
}
