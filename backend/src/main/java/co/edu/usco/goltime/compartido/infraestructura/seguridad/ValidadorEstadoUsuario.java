package co.edu.usco.goltime.compartido.infraestructura.seguridad;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import co.edu.usco.goltime.compartido.aplicacion.ConsultaEstadoUsuario;
import co.edu.usco.goltime.compartido.aplicacion.ConsultaEstadoUsuario.EstadoUsuario;

/**
 * Rechaza el access token si el usuario ya no existe, fue desactivado o su {@code tv}
 * (version_token) quedó desactualizado. Así la desactivación y el cambio de contraseña
 * surten efecto de inmediato, sin esperar a que el token expire.
 */
public class ValidadorEstadoUsuario implements OAuth2TokenValidator<Jwt> {

    public static final String RECLAMO_VERSION = "tv";

    private static final OAuth2Error SESION_INVALIDA =
            new OAuth2Error("invalid_token", "La sesión ya no es válida.", null);

    private final ConsultaEstadoUsuario consulta;

    public ValidadorEstadoUsuario(ConsultaEstadoUsuario consulta) {
        this.consulta = consulta;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        Long id = identificador(jwt.getSubject());
        Number version = jwt.getClaim(RECLAMO_VERSION);
        if (id == null || version == null) {
            return OAuth2TokenValidatorResult.failure(SESION_INVALIDA);
        }
        boolean vigente = consulta.estadoDe(id)
                .filter(EstadoUsuario::activo)
                .filter(estado -> estado.versionToken() == version.intValue())
                .isPresent();
        return vigente ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(SESION_INVALIDA);
    }

    private static Long identificador(String sujeto) {
        try {
            return Long.valueOf(sujeto);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
