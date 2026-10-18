package co.edu.usco.goltime.configuracion.infraestructura;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.usco.goltime.compartido.aplicacion.ContextoActual;
import co.edu.usco.goltime.configuracion.aplicacion.ServicioConfiguracion;
import co.edu.usco.goltime.configuracion.aplicacion.ServicioConfiguracion.Parametro;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/v1/admin/configuracion")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: configuración", description = "Políticas del negocio editables")
public class ControladorConfiguracion {

    public record SolicitudValor(@NotNull String valor) {
    }

    private final ServicioConfiguracion configuracion;
    private final ContextoActual contexto;

    public ControladorConfiguracion(ServicioConfiguracion configuracion, ContextoActual contexto) {
        this.configuracion = configuracion;
        this.contexto = contexto;
    }

    @GetMapping
    @Operation(summary = "Parámetros del negocio con su descripción y rango")
    public List<Parametro> listar() {
        return configuracion.listar();
    }

    @PutMapping("/{clave}")
    @Operation(summary = "Cambiar un parámetro (aplica de inmediato y queda auditado)")
    public Parametro actualizar(@PathVariable String clave, @Valid @RequestBody SolicitudValor s) {
        return configuracion.actualizar(clave, s.valor(), contexto.actor().orElseThrow().id());
    }
}
