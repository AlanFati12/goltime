package co.edu.usco.goltime.usuarios.aplicacion;

import java.util.List;
import java.util.Optional;

import co.edu.usco.goltime.compartido.dominio.Pagina;
import co.edu.usco.goltime.compartido.dominio.SolicitudPagina;
import co.edu.usco.goltime.usuarios.dominio.Usuario;

/** Puerto de salida: persistencia de usuarios. */
public interface RepositorioUsuarios {

    Optional<Usuario> buscarPorId(long id);

    /** El correo se compara sin distinguir mayúsculas. */
    Optional<Usuario> buscarPorCorreo(String correo);

    boolean existePorCorreo(String correo);

    Usuario guardar(Usuario usuario);

    Pagina<Usuario> buscarClientes(FiltroClientes filtro, SolicitudPagina pagina);

    List<Usuario> listarAdministradores();
}
