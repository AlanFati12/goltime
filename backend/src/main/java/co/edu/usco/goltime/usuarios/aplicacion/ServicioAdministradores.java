package co.edu.usco.goltime.usuarios.aplicacion;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.usco.goltime.auditoria.aplicacion.RegistroAuditoria;
import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.usuarios.dominio.Usuario;

/** Alta y consulta de administradores. El registro público nunca crea administradores. */
@Service
public class ServicioAdministradores {

    private final RepositorioUsuarios usuarios;
    private final RegistroAuditoria auditoria;

    public ServicioAdministradores(RepositorioUsuarios usuarios, RegistroAuditoria auditoria) {
        this.usuarios = usuarios;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return usuarios.listarAdministradores();
    }

    /**
     * Autoriza a una persona como administradora por su correo. No lleva contraseña: entra con la cuenta de Google de
     * ese correo y la primera vez queda vinculada.
     */
    @Transactional
    public Usuario crear(String nombres, String apellidos, String correo, String telefono) {
        if (usuarios.existePorCorreo(correo)) {
            throw new ExcepcionNegocio(CodigoError.CORREO_YA_REGISTRADO, "Ya existe una cuenta con ese correo.");
        }
        Usuario admin = Usuario.nuevoAdministrador(nombres, apellidos, correo, telefono);
        Usuario guardado = usuarios.guardar(admin);
        auditoria.registrar("CREAR_ADMINISTRADOR", "usuarios", guardado.id(), Map.of("correo", guardado.correo()), null);
        return guardado;
    }
}
