package co.edu.usco.goltime.usuarios.infraestructura;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import co.edu.usco.goltime.usuarios.aplicacion.ServicioAdministradores;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/admin/administradores")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: administradores", description = "Alta y consulta de administradores")
public class ControladorAdministradores {

    private final ServicioAdministradores administradores;

    public ControladorAdministradores(ServicioAdministradores administradores) {
        this.administradores = administradores;
    }

    @GetMapping
    @Operation(summary = "Listar administradores")
    public List<UsuarioResumen> listar() {
        return administradores.listar().stream().map(UsuarioResumen::de).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear otro administrador")
    public UsuarioResumen crear(@Valid @RequestBody SolicitudAdministrador solicitud) {
        return UsuarioResumen.de(administradores.crear(solicitud.nombres(), solicitud.apellidos(),
                solicitud.correo(), solicitud.telefono(), solicitud.clave()));
    }

    public record SolicitudAdministrador(
            @NotBlank @Size(max = 100) String nombres,
            @NotBlank @Size(max = 100) String apellidos,
            @NotBlank @Email @Size(max = 255) String correo,
            @Size(max = 30) String telefono,
            @NotBlank @Size(max = 200) String clave) {
    }
}
