package co.edu.usco.goltime.usuarios.aplicacion;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.usuarios.dominio.ClaveCambiadaEvento;
import co.edu.usco.goltime.usuarios.dominio.PoliticaClave;
import co.edu.usco.goltime.usuarios.dominio.Usuario;

/** Casos de uso del propio usuario: ver y editar su perfil y cambiar su contraseña. */
@Service
public class ServicioPerfil {

    private final RepositorioUsuarios usuarios;
    private final PasswordEncoder codificador;
    private final ApplicationEventPublisher eventos;

    public ServicioPerfil(RepositorioUsuarios usuarios, PasswordEncoder codificador,
            ApplicationEventPublisher eventos) {
        this.usuarios = usuarios;
        this.codificador = codificador;
        this.eventos = eventos;
    }

    @Transactional(readOnly = true)
    public Usuario obtener(long usuarioId) {
        return cargar(usuarioId);
    }

    @Transactional
    public Usuario actualizar(long usuarioId, String nombres, String apellidos, String telefono, String documento) {
        Usuario usuario = cargar(usuarioId);
        usuario.actualizarPerfil(nombres, apellidos, telefono, documento);
        if (usuario.telefono() != null && usuarios.existeTelefonoVerificado(usuario.telefono(), usuario.id())) {
            throw new ExcepcionNegocio(CodigoError.TELEFONO_YA_REGISTRADO, "Ese número de WhatsApp ya está vinculado a otra cuenta.");
        }
        return usuarios.guardar(usuario);
    }

    /**
     * Cambia la contraseña pidiendo la actual. Incrementa el version_token (los tokens de acceso
     * anteriores dejan de servir) y publica un evento para cerrar las demás sesiones.
     */
    @Transactional
    public void cambiarClave(long usuarioId, String claveActual, String claveNueva, String confirmacion) {
        Usuario usuario = cargar(usuarioId);
        if (!codificador.matches(claveActual, usuario.claveHash())) {
            throw new ExcepcionNegocio(CodigoError.CREDENCIALES_INVALIDAS, "La contraseña actual no es correcta.");
        }
        if (!claveNueva.equals(confirmacion)) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "La confirmación no coincide con la nueva contraseña.");
        }
        PoliticaClave.validar(claveNueva);
        usuario.cambiarClave(codificador.encode(claveNueva));
        usuarios.guardar(usuario);
        eventos.publishEvent(new ClaveCambiadaEvento(usuarioId));
    }

    private Usuario cargar(long usuarioId) {
        return usuarios.buscarPorId(usuarioId)
                .orElseThrow(() -> new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "El usuario no existe."));
    }
}
