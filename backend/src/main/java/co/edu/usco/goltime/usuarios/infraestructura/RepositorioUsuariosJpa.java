package co.edu.usco.goltime.usuarios.infraestructura;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import co.edu.usco.goltime.compartido.dominio.Rol;

interface RepositorioUsuariosJpa extends JpaRepository<UsuarioEntidad, Long>, JpaSpecificationExecutor<UsuarioEntidad> {

    @Query("select u from UsuarioEntidad u where lower(u.correo) = lower(:correo)")
    Optional<UsuarioEntidad> buscarPorCorreo(String correo);

    @Query("select count(u) > 0 from UsuarioEntidad u where lower(u.correo) = lower(:correo)")
    boolean existePorCorreo(String correo);

    List<UsuarioEntidad> findByRolOrderByNombresAsc(Rol rol);

    List<UsuarioEntidad> findByTelefonoOrderByIdAsc(String telefono);

    Optional<UsuarioEntidad> findByWhatsappLid(String whatsappLid);

    @Query("select count(u) > 0 from UsuarioEntidad u where u.telefono = :telefono and u.telefonoVerificado = true"
            + " and (:excluirId is null or u.id <> :excluirId)")
    boolean existeTelefonoVerificado(String telefono, Long excluirId);

    @Query("select u.activo as activo, u.versionToken as versionToken from UsuarioEntidad u where u.id = :id")
    Optional<EstadoProyeccion> estadoDe(long id);

    interface EstadoProyeccion {
        boolean getActivo();

        int getVersionToken();
    }
}
