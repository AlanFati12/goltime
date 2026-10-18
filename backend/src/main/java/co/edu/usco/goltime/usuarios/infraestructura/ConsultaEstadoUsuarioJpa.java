package co.edu.usco.goltime.usuarios.infraestructura;

import java.util.Optional;

import org.springframework.stereotype.Component;

import co.edu.usco.goltime.compartido.aplicacion.ConsultaEstadoUsuario;

/** Una consulta liviana por petición autenticada: estado activo y version_token del usuario. */
@Component
class ConsultaEstadoUsuarioJpa implements ConsultaEstadoUsuario {

    private final RepositorioUsuariosJpa jpa;

    ConsultaEstadoUsuarioJpa(RepositorioUsuariosJpa jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<EstadoUsuario> estadoDe(long usuarioId) {
        return jpa.estadoDe(usuarioId).map(p -> new EstadoUsuario(p.getActivo(), p.getVersionToken()));
    }
}
