package co.edu.usco.goltime.usuarios.dominio;

/** El usuario cambió o restableció su contraseña: sus sesiones deben cerrarse. */
public record ClaveCambiadaEvento(long usuarioId) {
}
