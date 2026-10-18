package co.edu.usco.goltime.usuarios.dominio;

import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;

/**
 * Política de contraseñas: mínimo 8 caracteres, al menos una letra y un número.
 * El máximo de 72 bytes es el límite real de BCrypt (más allá se ignoraría en silencio).
 */
public final class PoliticaClave {

    public static final int LONGITUD_MINIMA = 8;
    public static final int LONGITUD_MAXIMA_BYTES = 72;

    private PoliticaClave() {
    }

    public static void validar(String clave) {
        if (clave == null || clave.length() < LONGITUD_MINIMA) {
            throw invalida("La contraseña debe tener al menos " + LONGITUD_MINIMA + " caracteres.");
        }
        if (clave.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > LONGITUD_MAXIMA_BYTES) {
            throw invalida("La contraseña es demasiado larga.");
        }
        if (clave.chars().noneMatch(Character::isLetter)) {
            throw invalida("La contraseña debe incluir al menos una letra.");
        }
        if (clave.chars().noneMatch(Character::isDigit)) {
            throw invalida("La contraseña debe incluir al menos un número.");
        }
    }

    private static ExcepcionNegocio invalida(String mensaje) {
        return new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, mensaje);
    }
}
