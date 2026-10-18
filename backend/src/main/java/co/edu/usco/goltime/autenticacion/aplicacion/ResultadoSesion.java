package co.edu.usco.goltime.autenticacion.aplicacion;

import java.time.Duration;

import co.edu.usco.goltime.usuarios.dominio.Usuario;

/**
 * Resultado de ingresar o refrescar. El refresh token en texto plano solo existe aquí: el
 * controlador lo envía en la cookie y nunca se vuelve a mostrar.
 */
public record ResultadoSesion(TokenAcceso acceso, String refrescoPlano, boolean recordar,
        Duration duracionRefresco, Usuario usuario) {
}
