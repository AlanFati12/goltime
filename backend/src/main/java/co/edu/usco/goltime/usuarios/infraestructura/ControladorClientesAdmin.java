package co.edu.usco.goltime.usuarios.infraestructura;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.usco.goltime.compartido.dominio.Pagina;
import co.edu.usco.goltime.compartido.dominio.SolicitudPagina;
import co.edu.usco.goltime.usuarios.aplicacion.FiltroClientes;
import co.edu.usco.goltime.usuarios.aplicacion.ServicioGestionClientes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/admin/clientes")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: clientes", description = "Gestión de clientes por el administrador")
public class ControladorClientesAdmin {

    private final ServicioGestionClientes clientes;

    public ControladorClientesAdmin(ServicioGestionClientes clientes) {
        this.clientes = clientes;
    }

    @GetMapping
    @Operation(summary = "Listar clientes con búsqueda y filtro de estado")
    public Pagina<UsuarioResumen> listar(
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return clientes.listar(new FiltroClientes(busqueda, activo), new SolicitudPagina(pagina, tamano))
                .mapear(UsuarioResumen::de);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de un cliente")
    public UsuarioResumen detalle(@PathVariable long id) {
        return UsuarioResumen.de(clientes.detalle(id));
    }

    @PostMapping("/{id}/desactivar/previsualizar")
    @Operation(summary = "Previsualiza el efecto de desactivar: reservas futuras que se cancelarían")
    public PrevisualizacionDesactivacion previsualizar(@PathVariable long id) {
        int futuras = clientes.previsualizarDesactivacion(id);
        return new PrevisualizacionDesactivacion(futuras, futuras > 0);
    }

    @PostMapping("/{id}/desactivar")
    @Operation(summary = "Desactivar cliente (motivo obligatorio)",
            description = "Con reservas futuras exige confirmarConflictos=true; si no, responde 409 CONFLICTOS_SIN_CONFIRMAR.")
    public UsuarioResumen desactivar(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
            @Valid @RequestBody SolicitudDesactivacion solicitud) {
        return UsuarioResumen.de(clientes.desactivar(id, Long.parseLong(jwt.getSubject()), solicitud.motivo(),
                solicitud.confirmarConflictos()));
    }

    @PostMapping("/{id}/activar")
    @Operation(summary = "Reactivar cliente")
    public UsuarioResumen activar(@PathVariable long id) {
        return UsuarioResumen.de(clientes.activar(id));
    }

    @PatchMapping("/{id}/inscripcion-equipos")
    @Operation(summary = "Habilitar o quitar a un cliente la posibilidad de solicitar inscripción de equipos a torneos")
    public UsuarioResumen inscripcionEquipos(@PathVariable long id, @RequestBody SolicitudInscripcion solicitud) {
        return UsuarioResumen.de(clientes.habilitarInscripcionEquipos(id, solicitud.habilitado()));
    }

    public record PrevisualizacionDesactivacion(int reservasFuturasActivas, boolean requiereConfirmacion) {
    }

    public record SolicitudDesactivacion(@NotBlank @Size(max = 500) String motivo, boolean confirmarConflictos) {
    }

    public record SolicitudInscripcion(boolean habilitado) {
    }
}
