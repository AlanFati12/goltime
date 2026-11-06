package co.edu.usco.goltime.usuarios.infraestructura;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import co.edu.usco.goltime.usuarios.aplicacion.RepositorioUsuarios;
import co.edu.usco.goltime.usuarios.aplicacion.ServicioAdministradores;

/**
 * Autoriza al primer administrador a partir de {@code ADMIN_CORREO}: queda creado sin contraseña y se vincula con
 * su cuenta de Google la primera vez que entra con ese correo. Si el correo ya existe no hace nada; si falta la
 * variable solo avisa en el log.
 */
@Component
class AdministradorInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdministradorInicial.class);

    private final ServicioAdministradores administradores;
    private final RepositorioUsuarios usuarios;
    private final String correo;

    AdministradorInicial(ServicioAdministradores administradores, RepositorioUsuarios usuarios,
            @Value("${goltime.administrador-inicial.correo:}") String correo) {
        this.administradores = administradores;
        this.usuarios = usuarios;
        this.correo = correo;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (correo.isBlank()) {
            log.warn("ADMIN_CORREO no definido: no se crea administrador inicial.");
            return;
        }
        if (usuarios.existePorCorreo(correo)) {
            return;
        }
        administradores.crear("Administrador", "Goltime", correo, null);
        log.info("Administrador inicial creado: {}", correo);
    }
}
