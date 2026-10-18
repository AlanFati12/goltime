package co.edu.usco.goltime.usuarios.infraestructura;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import co.edu.usco.goltime.usuarios.aplicacion.ServicioPerfil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Perfil del usuario autenticado. El id sale siempre del token, nunca de la petición (evita IDOR). */
@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "Perfil", description = "Datos y contraseña del usuario autenticado")
public class ControladorPerfil {

    private final ServicioPerfil perfil;

    public ControladorPerfil(ServicioPerfil perfil) {
        this.perfil = perfil;
    }

    @GetMapping
    @Operation(summary = "Mi perfil")
    public UsuarioResumen obtener(@AuthenticationPrincipal Jwt jwt) {
        return UsuarioResumen.de(perfil.obtener(idDe(jwt)));
    }

    @PutMapping
    @Operation(summary = "Editar mis datos")
    public UsuarioResumen actualizar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SolicitudPerfil solicitud) {
        return UsuarioResumen.de(perfil.actualizar(idDe(jwt), solicitud.nombres(), solicitud.apellidos(),
                solicitud.telefono(), solicitud.documento()));
    }

    @PostMapping("/clave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cambiar mi contraseña", description = "Cierra todas las sesiones; hay que volver a ingresar.")
    public void cambiarClave(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SolicitudCambioClave solicitud) {
        perfil.cambiarClave(idDe(jwt), solicitud.claveActual(), solicitud.claveNueva(), solicitud.confirmacion());
    }

    private static long idDe(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }

    public record SolicitudPerfil(
            @NotBlank @Size(max = 100) String nombres,
            @NotBlank @Size(max = 100) String apellidos,
            @NotBlank @Size(max = 30) String telefono,
            @Size(max = 30) String documento) {
    }

    public record SolicitudCambioClave(
            @NotBlank String claveActual,
            @NotBlank @Size(max = 200) String claveNueva,
            @NotBlank String confirmacion) {
    }
}
