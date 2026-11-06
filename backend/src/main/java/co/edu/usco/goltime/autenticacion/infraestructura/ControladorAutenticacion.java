package co.edu.usco.goltime.autenticacion.infraestructura;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import co.edu.usco.goltime.autenticacion.aplicacion.ResultadoSesion;
import co.edu.usco.goltime.autenticacion.aplicacion.PropiedadesGoogle;
import co.edu.usco.goltime.autenticacion.aplicacion.ServicioSesion;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.usuarios.infraestructura.UsuarioResumen;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Endpoints públicos de autenticación. El access token viaja en el cuerpo (el front lo guarda en
 * memoria) y el refresh token solo en una cookie HttpOnly.
 */
@RestController
@RequestMapping("/api/v1/autenticacion")
@Tag(name = "Autenticación", description = "Ingreso con Google y renovación de sesión")
public class ControladorAutenticacion {

    private final ServicioSesion sesion;
    private final PropiedadesGoogle google;
    private final CookieRefresco cookie;

    ControladorAutenticacion(ServicioSesion sesion, PropiedadesGoogle google, CookieRefresco cookie) {
        this.sesion = sesion;
        this.google = google;
        this.cookie = cookie;
    }

    @GetMapping("/configuracion")
    @Operation(summary = "Datos públicos para mostrar el botón de Google",
            description = "El ID de cliente de Google es público. En desarrollo también dice si se aceptan credenciales de prueba.")
    public ConfiguracionIngreso configuracion() {
        return new ConfiguracionIngreso(google.clientId() == null ? "" : google.clientId().trim(), google.modoPrueba());
    }

    @PostMapping("/google")
    @Operation(summary = "Entrar con Google",
            description = "Recibe el token de identidad que entrega el botón de Google, lo verifica y devuelve el access token "
                    + "(la cuenta se crea si es la primera vez). El refresh token queda en una cookie HttpOnly.")
    public ResponseEntity<RespuestaSesion> ingresarConGoogle(@Valid @RequestBody SolicitudGoogle s,
            @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent,
            HttpServletRequest peticion) {
        return responder(sesion.ingresarConGoogle(s.credencial(), s.recordarme(), userAgent, peticion.getRemoteAddr()));
    }

    @PostMapping("/refrescar")
    @Operation(summary = "Renovar el access token usando la cookie de refresh (rota el refresh token)")
    public ResponseEntity<RespuestaSesion> refrescar(
            @CookieValue(name = CookieRefresco.NOMBRE, required = false) String refresco,
            @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent,
            HttpServletRequest peticion) {
        try {
            return responder(sesion.refrescar(refresco, userAgent, peticion.getRemoteAddr()));
        } catch (ExcepcionNegocio ex) {
            // Si el refresh es inválido, se borra la cookie para no reintentar con ella.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.SET_COOKIE, cookie.borrar().toString())
                    .body(null);
        }
    }

    @PostMapping("/salir")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cerrar sesión: revoca el refresh token y borra la cookie")
    public ResponseEntity<Void> salir(@CookieValue(name = CookieRefresco.NOMBRE, required = false) String refresco) {
        sesion.salir(refresco);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie.borrar().toString()).build();
    }

    private ResponseEntity<RespuestaSesion> responder(ResultadoSesion r) {
        RespuestaSesion cuerpo = new RespuestaSesion(r.acceso().valor(), "Bearer", r.acceso().expiraEnSegundos(),
                UsuarioResumen.de(r.usuario()));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.crear(r.refrescoPlano(), r.recordar(), r.duracionRefresco()).toString())
                .body(cuerpo);
    }

    public record RespuestaSesion(String tokenAcceso, String tipo, long expiraEnSegundos, UsuarioResumen usuario) {
    }

    public record SolicitudGoogle(@NotBlank @Size(max = 4000) String credencial, boolean recordarme) {
    }

    public record ConfiguracionIngreso(String googleClientId, boolean modoPrueba) {
    }
}
