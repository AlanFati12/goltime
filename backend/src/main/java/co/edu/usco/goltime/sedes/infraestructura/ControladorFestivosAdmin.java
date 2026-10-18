package co.edu.usco.goltime.sedes.infraestructura;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import co.edu.usco.goltime.sedes.aplicacion.ServicioFestivos;
import co.edu.usco.goltime.sedes.infraestructura.DtosSedes.RespuestaFestivo;
import co.edu.usco.goltime.sedes.infraestructura.DtosSedes.SolicitudFestivo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/festivos")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: festivos", description = "Festivos que activan las tarifas especiales")
public class ControladorFestivosAdmin {

    private final ServicioFestivos festivos;

    public ControladorFestivosAdmin(ServicioFestivos festivos) {
        this.festivos = festivos;
    }

    @GetMapping
    public List<RespuestaFestivo> listar(@RequestParam(required = false) Integer anio) {
        return festivos.delAnio(anio != null ? anio : LocalDate.now().getYear()).stream().map(MapeadorSedes::aRespuesta).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Agregar un festivo")
    public RespuestaFestivo crear(@Valid @RequestBody SolicitudFestivo s) {
        return MapeadorSedes.aRespuesta(festivos.crear(s.fecha(), s.nombre()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable long id) {
        festivos.eliminar(id);
    }
}
