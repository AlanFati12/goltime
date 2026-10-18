package co.edu.usco.goltime.compartido.infraestructura.seguridad;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import co.edu.usco.goltime.compartido.aplicacion.ContextoActual;
import co.edu.usco.goltime.compartido.dominio.Actor;
import co.edu.usco.goltime.compartido.dominio.Rol;

/** Resuelve el actor desde el JWT del contexto de seguridad y la IP desde la petición HTTP. */
@Component
public class ContextoActualSpring implements ContextoActual {

    @Override
    public Optional<Actor> actor() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion != null && autenticacion.getPrincipal() instanceof Jwt jwt) {
            try {
                Rol rol = Rol.valueOf(jwt.getClaimAsString(ConfiguracionSeguridad.RECLAMO_ROL));
                return Optional.of(new Actor(Long.valueOf(jwt.getSubject()), rol));
            } catch (IllegalArgumentException | NullPointerException ex) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> ip() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
            return Optional.ofNullable(atributos.getRequest().getRemoteAddr());
        }
        return Optional.empty();
    }
}
