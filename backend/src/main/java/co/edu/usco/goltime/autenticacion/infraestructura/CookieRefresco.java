package co.edu.usco.goltime.autenticacion.infraestructura;

import java.time.Duration;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import co.edu.usco.goltime.compartido.infraestructura.seguridad.PropiedadesSeguridad;

/**
 * Cookie del refresh token: HttpOnly, SameSite=Strict, Secure fuera de desarrollo y limitada a las
 * rutas de autenticación. Sin "recordarme" es una cookie de sesión (se borra al cerrar el navegador).
 */
@Component
class CookieRefresco {

    static final String NOMBRE = "goltime_refresco";
    private static final String RUTA = "/api/v1/autenticacion";

    private final boolean segura;

    CookieRefresco(PropiedadesSeguridad seguridad) {
        this.segura = seguridad.cookieSegura();
    }

    ResponseCookie crear(String valor, boolean recordar, Duration duracion) {
        ResponseCookie.ResponseCookieBuilder cookie = base(valor);
        if (recordar) {
            cookie.maxAge(duracion);
        }
        return cookie.build();
    }

    ResponseCookie borrar() {
        return base("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String valor) {
        return ResponseCookie.from(NOMBRE, valor)
                .httpOnly(true)
                .secure(segura)
                .sameSite("Strict")
                .path(RUTA);
    }
}
