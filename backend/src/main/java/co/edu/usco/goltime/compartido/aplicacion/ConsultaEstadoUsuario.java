package co.edu.usco.goltime.compartido.aplicacion;

import java.util.Optional;

/**
 * Puerto que permite a la capa de seguridad comprobar, en cada petición, que el usuario sigue
 * activo y que su token no fue invalidado (desactivación o cambio de contraseña).
 */
public interface ConsultaEstadoUsuario {

    Optional<EstadoUsuario> estadoDe(long usuarioId);

    record EstadoUsuario(boolean activo, int versionToken) {
    }
}
