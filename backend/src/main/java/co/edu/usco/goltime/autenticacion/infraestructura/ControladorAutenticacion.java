package co.edu.usco.goltime.autenticacion.infraestructura;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import co.edu.usco.goltime.autenticacion.aplicacion.ResultadoSesion;
import co.edu.usco.goltime.autenticacion.aplicacion.ServicioRecuperacionClave;
import co.edu.usco.goltime.autenticacion.aplicacion.ServicioRegistro;
import co.edu.usco.goltime.autenticacion.aplicacion.ServicioRegistro.DatosRegistro;
import co.edu.usco.goltime.autenticacion.aplicacion.ServicioSesion;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.usuarios.infraestructura.UsuarioResumen;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Endpoints públicos de autenticación. El access token viaja en el cuerpo (el front lo guarda en
 * memoria) y el refresh token solo en una cookie HttpOnly.
 */
@RestController
@RequestMapping("/api/v1/autenticacion")
@Tag(name = "Autenticación", description = "Registro, ingreso, renovación de sesión y recuperación de contraseña")
public class ControladorAutenticacion {

    private final ServicioSesion sesion;
    private final ServicioRegistro registro;
    private final ServicioRecuperacionClave recuperacion;
    private final CookieRefresco cookie;

    ControladorAutenticacion(ServicioSesion sesion, ServicioRegistro registro, ServicioRecuperacionClave recuperacion,
            CookieRefresco cookie) {
        this.sesion = sesion;
        this.registro = registro;
        this.recuperacion = recuperacion;
        this.cookie = cookie;
    }

    @PostMapping("/registrar")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registro público de un cliente",
            description = "Siempre crea usuarios con rol CLIENTE. Envía un correo para verificar la cuenta.")
    public UsuarioResumen registrar(@Valid @RequestBody SolicitudRegistro s) {
        return UsuarioResumen.de(registro.registrar(new DatosRegistro(s.nombres(), s.apellidos(), s.correo(),
                s.telefono(), s.documento(), s.clave(), s.confirmacionClave(), s.aceptaTerminos())));
    }

    @PostMapping("/ingresar")
    @Operation(summary = "Iniciar sesión",
            description = "Devuelve el access token y deja el refresh token en una cookie HttpOnly.")
    public ResponseEntity<RespuestaSesion> ingresar(@Valid @RequestBody SolicitudIngreso s,
            @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent,
            HttpServletRequest peticion) {
        ResultadoSesion resultado = sesion.ingresar(s.correo(), s.clave(), s.recordarme(), userAgent,
                peticion.getRemoteAddr());
        return responder(resultado);
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

    @PostMapping("/olvide-clave")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Solicitar correo de recuperación",
            description = "Responde 202 exista o no el correo, para no revelar qué cuentas existen.")
    public void olvideClave(@Valid @RequestBody SolicitudCorreo s) {
        recuperacion.solicitar(s.correo());
    }

    @PostMapping("/restablecer-clave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Crear una contraseña nueva con el token del correo (un solo uso)")
    public void restablecerClave(@Valid @RequestBody SolicitudRestablecer s) {
        recuperacion.restablecer(s.token(), s.claveNueva(), s.confirmacion());
    }

    @PostMapping("/verificar-correo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Verificar el correo con el token enviado al registrarse")
    public void verificarCorreo(@Valid @RequestBody SolicitudToken s) {
        registro.verificarCorreo(s.token());
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

    public record SolicitudRegistro(
            @NotBlank @Size(max = 100) String nombres,
            @NotBlank @Size(max = 100) String apellidos,
            @NotBlank @Email @Size(max = 255) String correo,
            @NotBlank @Size(max = 30) String telefono,
            @Size(max = 30) String documento,
            @NotBlank @Size(max = 200) String clave,
            @NotBlank @Size(max = 200) String confirmacionClave,
            @AssertTrue(message = "Debes aceptar los términos y el tratamiento de datos.") boolean aceptaTerminos) {
    }

    public record SolicitudIngreso(
            @NotBlank @Email String correo,
            @NotBlank @Size(max = 200) String clave,
            boolean recordarme) {
    }

    public record SolicitudCorreo(@NotBlank @Email String correo) {
    }

    public record SolicitudToken(@NotBlank @Size(max = 200) String token) {
    }

    public record SolicitudRestablecer(
            @NotBlank @Size(max = 200) String token,
            @NotBlank @Size(max = 200) String claveNueva,
            @NotBlank @Size(max = 200) String confirmacion) {
    }
}
