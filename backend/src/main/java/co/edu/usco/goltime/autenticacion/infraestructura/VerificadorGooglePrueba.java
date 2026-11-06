package co.edu.usco.goltime.autenticacion.infraestructura;

import java.util.Locale;

import co.edu.usco.goltime.autenticacion.aplicacion.IdentidadGoogle;
import co.edu.usco.goltime.autenticacion.aplicacion.VerificadorGoogle;
import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;

/**
 * Sustituto de Google para desarrollar y probar sin cuenta ni internet. Acepta credenciales con la forma
 * {@code prueba|correo|nombres|apellidos} (o {@code noverificado|…} para simular un correo que Google no confirma).
 * Solo se crea con {@code goltime.google.modo-prueba=true}, que el perfil de producción rechaza al arrancar.
 */
final class VerificadorGooglePrueba implements VerificadorGoogle {

    @Override
    public IdentidadGoogle verificar(String credencial) {
        String[] partes = credencial == null ? new String[0] : credencial.split("\\|", -1);
        boolean valida = partes.length >= 2 && (partes[0].equals("prueba") || partes[0].equals("noverificado")) && partes[1].contains("@");
        if (!valida) {
            throw new ExcepcionNegocio(CodigoError.CREDENCIALES_INVALIDAS, "No pudimos verificar tu cuenta de Google. Intenta de nuevo.");
        }
        String correo = partes[1].trim().toLowerCase(Locale.ROOT);
        String nombres = partes.length > 2 && !partes[2].isBlank() ? partes[2] : correo.substring(0, correo.indexOf('@'));
        String apellidos = partes.length > 3 ? partes[3] : "";
        return new IdentidadGoogle("prueba-" + correo, correo, partes[0].equals("prueba"), nombres, apellidos);
    }
}
