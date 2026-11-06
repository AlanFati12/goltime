package co.edu.usco.goltime.usuarios.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.usuarios.dominio.Usuario;

/** Casos de uso del propio usuario: ver y editar su perfil. */
@Service
public class ServicioPerfil {

    private final RepositorioUsuarios usuarios;

    public ServicioPerfil(RepositorioUsuarios usuarios) {
        this.usuarios = usuarios;
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

    private Usuario cargar(long usuarioId) {
        return usuarios.buscarPorId(usuarioId)
                .orElseThrow(() -> new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "El usuario no existe."));
    }
}
