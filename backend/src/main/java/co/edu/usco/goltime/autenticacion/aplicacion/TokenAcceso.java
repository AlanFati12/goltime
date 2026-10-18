package co.edu.usco.goltime.autenticacion.aplicacion;

/** Access token JWT emitido y su vigencia en segundos. */
public record TokenAcceso(String valor, long expiraEnSegundos) {
}
