package co.edu.usco.goltime.compartido.dominio;

import java.util.regex.Pattern;

/**
 * Número de celular en formato internacional E.164 ({@code +573001234567}). Es la única forma en que
 * se guarda un teléfono de cliente, para que {@code 3001234567}, {@code +57 300 123 4567} y
 * {@code 573001234567} sean el mismo número (también para el bot de WhatsApp, que identifica por él).
 * Sin indicativo se asume Colombia.
 */
public record NumeroTelefono(String valor) {

    public static final String INDICATIVO_COLOMBIA = "57";

    private static final Pattern SEPARADORES = Pattern.compile("[\\s().\\-]");
    private static final Pattern E164 = Pattern.compile("\\+[1-9]\\d{7,14}");
    private static final Pattern CELULAR_COLOMBIA = Pattern.compile("3\\d{9}");

    public NumeroTelefono {
        if (valor == null || !E164.matcher(valor).matches()) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "El número de teléfono no es válido.");
        }
        if (valor.startsWith("+" + INDICATIVO_COLOMBIA) && !CELULAR_COLOMBIA.matcher(valor.substring(3)).matches()) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS,
                    "El celular colombiano debe tener 10 dígitos y empezar por 3, por ejemplo +57 300 123 4567.");
        }
    }

    /** Convierte lo que escribió la persona al formato E.164; lanza {@code DATOS_INVALIDOS} si no se puede. */
    public static NumeroTelefono de(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "El número de WhatsApp es obligatorio.");
        }
        String limpio = SEPARADORES.matcher(texto.trim()).replaceAll("");
        if (limpio.startsWith("00")) {
            limpio = "+" + limpio.substring(2);
        }
        if (limpio.startsWith("+")) {
            return new NumeroTelefono(limpio);
        }
        if (!limpio.chars().allMatch(Character::isDigit)) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "El número de teléfono solo puede tener dígitos.");
        }
        if (CELULAR_COLOMBIA.matcher(limpio).matches()) {
            return new NumeroTelefono("+" + INDICATIVO_COLOMBIA + limpio);
        }
        if (limpio.startsWith(INDICATIVO_COLOMBIA) && CELULAR_COLOMBIA.matcher(limpio.substring(2)).matches()) {
            return new NumeroTelefono("+" + limpio);
        }
        throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS,
                "Escribe el celular de 10 dígitos o incluye el indicativo del país, por ejemplo +57.");
    }

    /** Igual que {@link #de(String)}, pero un texto vacío es {@code null} (teléfono opcional). */
    public static String normalizarOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : de(texto).valor();
    }

    /** Para mostrar: {@code +57 300 123 4567}; otros países quedan tal cual. */
    public String legible() {
        if (valor.startsWith("+" + INDICATIVO_COLOMBIA)) {
            String n = valor.substring(3);
            return "+57 " + n.substring(0, 3) + " " + n.substring(3, 6) + " " + n.substring(6);
        }
        return valor;
    }

    @Override
    public String toString() {
        return valor;
    }
}
