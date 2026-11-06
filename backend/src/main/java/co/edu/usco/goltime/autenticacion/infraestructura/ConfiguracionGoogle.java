package co.edu.usco.goltime.autenticacion.infraestructura;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import co.edu.usco.goltime.autenticacion.aplicacion.PropiedadesGoogle;
import co.edu.usco.goltime.autenticacion.aplicacion.VerificadorGoogle;
import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;

/** Elige cómo se verifica el ingreso: Google de verdad, el sustituto de pruebas o «sin configurar». */
@Configuration
class ConfiguracionGoogle {

    private static final Logger log = LoggerFactory.getLogger(ConfiguracionGoogle.class);

    @Bean
    VerificadorGoogle verificadorGoogle(PropiedadesGoogle propiedades, Environment entorno) {
        if (propiedades.modoPrueba()) {
            if (entorno.acceptsProfiles(Profiles.of("prod"))) {
                throw new IllegalStateException("GOOGLE_MODO_PRUEBA no puede estar activo en producción: dejaría entrar a cualquiera.");
            }
            log.warn("Ingreso con Google en MODO DE PRUEBA: se aceptan credenciales de mentira. Solo para desarrollo.");
            return new VerificadorGooglePrueba();
        }
        if (propiedades.clientId() == null || propiedades.clientId().isBlank()) {
            log.warn("GOOGLE_CLIENT_ID no está definido: nadie podrá ingresar hasta configurarlo.");
            return credencial -> {
                throw new ExcepcionNegocio(CodigoError.SERVICIO_NO_DISPONIBLE, "El ingreso con Google todavía no está configurado.");
            };
        }
        return new VerificadorGoogleNimbus(propiedades.clientId().trim());
    }
}
