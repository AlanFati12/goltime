package co.edu.usco.goltime.autenticacion.aplicacion;

/**
 * Lo que Google afirma de quien entra, ya verificado: {@code sub} es el identificador estable de la cuenta (no
 * cambia aunque la persona cambie de correo) y {@code correoVerificado} dice si Google confirmó el correo.
 */
public record IdentidadGoogle(String sub, String correo, boolean correoVerificado, String nombres, String apellidos) {
}
