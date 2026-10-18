package co.edu.usco.goltime.sedes.infraestructura;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import co.edu.usco.goltime.sedes.aplicacion.ServicioFotos;
import co.edu.usco.goltime.sedes.aplicacion.ServicioSedes;
import co.edu.usco.goltime.sedes.aplicacion.Vistas.VistaSede;
import co.edu.usco.goltime.sedes.dominio.HorarioSemanal;
import co.edu.usco.goltime.sedes.dominio.Sede.DatosSede;
import co.edu.usco.goltime.sedes.infraestructura.DtosSedes.PrevisualizacionEstado;
import co.edu.usco.goltime.sedes.infraestructura.DtosSedes.RespuestaFoto;
import co.edu.usco.goltime.sedes.infraestructura.DtosSedes.RespuestaSede;
import co.edu.usco.goltime.sedes.infraestructura.DtosSedes.SolicitudEstadoSede;
import co.edu.usco.goltime.sedes.infraestructura.DtosSedes.SolicitudHorario;
import co.edu.usco.goltime.sedes.infraestructura.DtosSedes.SolicitudPrevisualizarSede;
import co.edu.usco.goltime.sedes.infraestructura.DtosSedes.SolicitudSede;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/sedes")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: sedes", description = "Gestión de sedes, horarios y fotos")
public class ControladorSedesAdmin {

    private final ServicioSedes sedes;
    private final ServicioFotos fotos;

    public ControladorSedesAdmin(ServicioSedes sedes, ServicioFotos fotos) {
        this.sedes = sedes;
        this.fotos = fotos;
    }

    @GetMapping
    @Operation(summary = "Todas las sedes (activas e inactivas) con sus canchas")
    public List<RespuestaSede> listar() {
        return sedes.listarAdmin().stream().map(MapeadorSedes::aRespuesta).toList();
    }

    @GetMapping("/{id}")
    public RespuestaSede detalle(@PathVariable long id) {
        return MapeadorSedes.aRespuesta(sedes.detalleAdmin(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear sede", description = "Sin horario, se crea con el predeterminado (L-S 8:00-23:00, D 8:00-20:00).")
    public RespuestaSede crear(@Valid @RequestBody SolicitudSede s) {
        HorarioSemanal horario = s.horario() == null || s.horario().isEmpty() ? null : MapeadorSedes.aHorario(s.horario());
        long id = sedes.crear(datos(s), horario).id();
        return MapeadorSedes.aRespuesta(sedes.detalleAdmin(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar los datos de la sede")
    public RespuestaSede actualizar(@PathVariable long id, @Valid @RequestBody SolicitudSede s) {
        sedes.actualizar(id, datos(s));
        return MapeadorSedes.aRespuesta(sedes.detalleAdmin(id));
    }

    @PutMapping("/{id}/horario")
    @Operation(summary = "Definir el horario de operación semanal de la sede")
    public RespuestaSede horario(@PathVariable long id, @Valid @RequestBody SolicitudHorario s) {
        sedes.definirHorario(id, MapeadorSedes.aHorario(s.dias()));
        return MapeadorSedes.aRespuesta(sedes.detalleAdmin(id));
    }

    @PostMapping("/{id}/estado/previsualizar")
    @Operation(summary = "Previsualiza las reservas futuras que se cancelarían al desactivar la sede")
    public PrevisualizacionEstado previsualizar(@PathVariable long id, @Valid @RequestBody SolicitudPrevisualizarSede s) {
        int futuras = s.activa() ? 0 : sedes.previsualizarDesactivacion(id);
        return new PrevisualizacionEstado(futuras, futuras > 0);
    }

    @PostMapping("/{id}/estado")
    @Operation(summary = "Activar o desactivar la sede",
            description = "Desactivar exige motivo; con reservas futuras exige confirmarConflictos=true (409 CONFLICTOS_SIN_CONFIRMAR).")
    public RespuestaSede estado(@PathVariable long id, @Valid @RequestBody SolicitudEstadoSede s) {
        sedes.cambiarEstado(id, s.activa(), s.motivo(), s.confirmarConflictos());
        return MapeadorSedes.aRespuesta(sedes.detalleAdmin(id));
    }

    @PostMapping(value = "/{id}/fotos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Subir una foto (JPG, PNG o WebP, máx. 5 MB, hasta 10 por sede)")
    public RespuestaFoto subirFoto(@PathVariable long id, @RequestParam("archivo") MultipartFile archivo) throws IOException {
        var foto = fotos.agregarASede(id, archivo.getBytes());
        return new RespuestaFoto(foto.id(), MapeadorSedes.RUTA_ARCHIVOS + foto.archivo(), foto.orden());
    }

    @DeleteMapping("/{id}/fotos/{fotoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void quitarFoto(@PathVariable long id, @PathVariable long fotoId) {
        fotos.quitarDeSede(id, fotoId);
    }

    private static DatosSede datos(SolicitudSede s) {
        return new DatosSede(s.nombre(), s.direccion(), s.ciudad(), s.telefono(), s.whatsapp(), s.descripcion(),
                s.servicios(), s.latitud(), s.longitud());
    }
}
