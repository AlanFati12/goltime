package co.edu.usco.goltime.usuarios.dominio;

/** Un cliente fue desactivado: sus sesiones deben cerrarse. */
public record UsuarioDesactivadoEvento(long usuarioId) {
}
