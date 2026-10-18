package co.edu.usco.goltime.usuarios.aplicacion;

import java.util.List;
import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.usco.goltime.auditoria.aplicacion.RegistroAuditoria;
import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.usuarios.dominio.PoliticaClave;
import co.edu.usco.goltime.usuarios.dominio.Usuario;

/** Alta y consulta de administradores. El registro público nunca crea administradores. */
@Service
public class ServicioAdministradores {

    private final RepositorioUsuarios usuarios;
    private final PasswordEncoder codificador;
    private final RegistroAuditoria auditoria;

    public ServicioAdministradores(RepositorioUsuarios usuarios, PasswordEncoder codificador,
            RegistroAuditoria auditoria) {
        this.usuarios = usuarios;
        this.codificador = codificador;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return usuarios.listarAdministradores();
    }

    @Transactional
    public Usuario crear(String nombres, String apellidos, String correo, String telefono, String clave) {
        PoliticaClave.validar(clave);
        if (usuarios.existePorCorreo(correo)) {
            throw new ExcepcionNegocio(CodigoError.CORREO_YA_REGISTRADO, "Ya existe una cuenta con ese correo.");
        }
        Usuario admin = Usuario.nuevoAdministrador(nombres, apellidos, correo, telefono, codificador.encode(clave));
        Usuario guardado = usuarios.guardar(admin);
        auditoria.registrar("CREAR_ADMINISTRADOR", "usuarios", guardado.id(), Map.of("correo", guardado.correo()), null);
        return guardado;
    }
}
