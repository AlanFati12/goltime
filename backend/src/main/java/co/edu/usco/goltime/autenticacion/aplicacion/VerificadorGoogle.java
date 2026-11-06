package co.edu.usco.goltime.autenticacion.aplicacion;

/** Puerto de salida: comprueba el token de identidad que entrega el botón «Acceder con Google». */
public interface VerificadorGoogle {

    /**
     * Valida el token (firma de Google, emisor, destinatario y vigencia) y devuelve a quién pertenece.
     *
     * @throws co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio si el token no es válido
     */
    IdentidadGoogle verificar(String credencial);
}
