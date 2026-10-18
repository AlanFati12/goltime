package co.edu.usco.goltime.autenticacion.aplicacion;

import co.edu.usco.goltime.usuarios.dominio.Usuario;

/** Puerto de salida: emite el access token (JWT) de un usuario. */
public interface EmisorTokenAcceso {

    TokenAcceso emitir(Usuario usuario);
}
