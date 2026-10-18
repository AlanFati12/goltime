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
 * Crea el primer administrador a partir de {@code ADMIN_CORREO} y {@code ADMIN_CLAVE}.
 * Si el correo ya existe no hace nada; si faltan las variables solo avisa en el log.
 */
@Component
class AdministradorInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdministradorInicial.class);

    private final ServicioAdministradores administradores;
    private final RepositorioUsuarios usuarios;
    private final String correo;
    private final String clave;

    AdministradorInicial(ServicioAdministradores administradores, RepositorioUsuarios usuarios,
            @Value("${goltime.administrador-inicial.correo:}") String correo,
            @Value("${goltime.administrador-inicial.clave:}") String clave) {
        this.administradores = administradores;
        this.usuarios = usuarios;
        this.correo = correo;
        this.clave = clave;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (correo.isBlank() || clave.isBlank()) {
            log.warn("ADMIN_CORREO / ADMIN_CLAVE no definidos: no se crea administrador inicial.");
            return;
        }
        if (usuarios.existePorCorreo(correo)) {
            return;
        }
        administradores.crear("Administrador", "Goltime", correo, null, clave);
        log.info("Administrador inicial creado: {}", correo);
    }
}
