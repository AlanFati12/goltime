package co.edu.usco.goltime.sedes.infraestructura;

import java.io.IOException;
import java.util.List;
import java.util.Set;

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

import co.edu.usco.goltime.sedes.aplicacion.ServicioCanchas;
import co.edu.usco.goltime.sedes.aplicacion.ServicioFotos;
import co.edu.usco.goltime.sedes.aplicacion.ServicioTarifas;
import co.edu.usco.goltime.sedes.aplicacion.ServicioTarifas.DatosTarifa;
import co.edu.usco.goltime.sedes.dominio.Cancha.DatosCancha;
import co.edu.usco.goltime.sedes.dominio.MinutoDelDia;
import co.edu.usco.goltime.sedes.infraestructura.DtosSedes.PrevisualizacionEstado;
import co.edu.usco.goltime.sedes.infraestructura.DtosSedes.RespuestaCancha;
import co.edu.usco.goltime.sedes.infraestructura.DtosSedes.RespuestaFoto;
import co.edu.usco.goltime.sedes.infraestructura.DtosSedes.RespuestaTarifa;
import co.edu.usco.goltime.sedes.infraestructura.DtosSedes.SolicitudCancha;
import co.edu.usco.goltime.sedes.infraestructura.DtosSedes.SolicitudEstadoCancha;
import co.edu.usco.goltime.sedes.infraestructura.DtosSedes.SolicitudHorario;
import co.edu.usco.goltime.sedes.infraestructura.DtosSedes.SolicitudPrevisualizarCancha;
import co.edu.usco.goltime.sedes.infraestructura.DtosSedes.SolicitudTarifa;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: canchas", description = "Canchas, dependencias entre canchas, horario propio, tarifas y fotos")
public class ControladorCanchasAdmin {

    private final ServicioCanchas canchas;
    private final ServicioTarifas tarifas;
    private final ServicioFotos fotos;

    public ControladorCanchasAdmin(ServicioCanchas canchas, ServicioTarifas tarifas, ServicioFotos fotos) {
        this.canchas = canchas;
        this.tarifas = tarifas;
        this.fotos = fotos;
    }

    @GetMapping("/canchas")
    @Operation(summary = "Listar canchas de todos los estados, opcionalmente de una sede")
    public List<RespuestaCancha> listar(@RequestParam(required = false) Long sedeId) {
        return canchas.listarAdmin(sedeId).stream().map(MapeadorSedes::aRespuesta).toList();
    }

    @GetMapping("/canchas/{id}")
    public RespuestaCancha detalle(@PathVariable long id) {
        return MapeadorSedes.aRespuesta(canchas.detalleAdmin(id));
    }

    @PostMapping("/sedes/{sedeId}/canchas")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear una cancha en la sede",
            description = "`componentes` son los ids de las canchas de las que depende (cancha compuesta, ej. F8 = dos F5).")
    public RespuestaCancha crear(@PathVariable long sedeId, @Valid @RequestBody SolicitudCancha s) {
        return MapeadorSedes.aRespuesta(canchas.crear(sedeId, datos(s), componentes(s)));
    }

    @PutMapping("/canchas/{id}")
    @Operation(summary = "Editar la cancha y las canchas de las que depende")
    public RespuestaCancha actualizar(@PathVariable long id, @Valid @RequestBody SolicitudCancha s) {
        return MapeadorSedes.aRespuesta(canchas.actualizar(id, datos(s), componentes(s)));
    }

    @PutMapping("/canchas/{id}/horario")
    @Operation(summary = "Definir un horario propio para la cancha (reemplaza al de la sede en los días indicados)")
    public RespuestaCancha horario(@PathVariable long id, @Valid @RequestBody SolicitudHorario s) {
        return MapeadorSedes.aRespuesta(canchas.definirHorarioPropio(id, MapeadorSedes.aHorario(s.dias())));
    }

    @DeleteMapping("/canchas/{id}/horario")
    @Operation(summary = "Quitar el horario propio: la cancha vuelve a usar el de la sede")
    public RespuestaCancha quitarHorario(@PathVariable long id) {
        return MapeadorSedes.aRespuesta(canchas.quitarHorarioPropio(id));
    }

    @PostMapping("/canchas/{id}/estado/previsualizar")
    @Operation(summary = "Previsualiza las reservas futuras que se cancelarían al cambiar el estado")
    public PrevisualizacionEstado previsualizar(@PathVariable long id, @Valid @RequestBody SolicitudPrevisualizarCancha s) {
        int futuras = canchas.previsualizarCambioDeEstado(id, s.estado());
        return new PrevisualizacionEstado(futuras, futuras > 0);
    }

    @PostMapping("/canchas/{id}/estado")
    @Operation(summary = "Cambiar el estado (ACTIVA, MANTENIMIENTO, INACTIVA)",
            description = "Salir de servicio exige motivo; con reservas futuras exige confirmarConflictos=true (409 CONFLICTOS_SIN_CONFIRMAR).")
    public RespuestaCancha estado(@PathVariable long id, @Valid @RequestBody SolicitudEstadoCancha s) {
        return MapeadorSedes.aRespuesta(canchas.cambiarEstado(id, s.estado(), s.motivo(), s.confirmarConflictos()));
    }

    // ----- Tarifas -----

    @GetMapping("/canchas/{id}/tarifas")
    public List<RespuestaTarifa> tarifas(@PathVariable long id) {
        return tarifas.listar(id).stream().map(MapeadorSedes::aRespuesta).toList();
    }

    @PostMapping("/canchas/{id}/tarifas")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear una franja de tarifa (días + rango horario + precio por hora)")
    public RespuestaTarifa crearTarifa(@PathVariable long id, @Valid @RequestBody SolicitudTarifa s) {
        return MapeadorSedes.aRespuesta(tarifas.crear(id, datosTarifa(s)));
    }

    @PutMapping("/tarifas/{tarifaId}")
    public RespuestaTarifa actualizarTarifa(@PathVariable long tarifaId, @Valid @RequestBody SolicitudTarifa s) {
        return MapeadorSedes.aRespuesta(tarifas.actualizar(tarifaId, datosTarifa(s)));
    }

    @DeleteMapping("/tarifas/{tarifaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarTarifa(@PathVariable long tarifaId) {
        tarifas.eliminar(tarifaId);
    }

    // ----- Fotos -----

    @PostMapping(value = "/canchas/{id}/fotos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Subir una foto (JPG, PNG o WebP, máx. 5 MB, hasta 10 por cancha)")
    public RespuestaFoto subirFoto(@PathVariable long id, @RequestParam("archivo") MultipartFile archivo) throws IOException {
        var foto = fotos.agregarACancha(id, archivo.getBytes());
        return new RespuestaFoto(foto.id(), MapeadorSedes.RUTA_ARCHIVOS + foto.archivo(), foto.orden());
    }

    @DeleteMapping("/canchas/{id}/fotos/{fotoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void quitarFoto(@PathVariable long id, @PathVariable long fotoId) {
        fotos.quitarDeCancha(id, fotoId);
    }

    // ----- Conversión -----

    private static DatosCancha datos(SolicitudCancha s) {
        int bloque = s.duracionBloqueMin() != null ? s.duracionBloqueMin() : 60;
        int minima = s.duracionMinMin() != null ? s.duracionMinMin() : bloque;
        int maxima = s.duracionMaxMin() != null ? s.duracionMaxMin() : Math.max(minima, bloque * 2);
        int capacidad = s.capacidadTotal() != null ? s.capacidadTotal() : s.jugadoresPorEquipo() * 2 + 2;
        return new DatosCancha(s.nombre(), "F" + s.jugadoresPorEquipo(), s.jugadoresPorEquipo(), capacidad, s.superficie(),
                s.largoM(), s.anchoM(), Boolean.TRUE.equals(s.techada()), s.iluminacion() == null || s.iluminacion(),
                s.precioBaseHora(), bloque, minima, maxima, s.descripcion(), s.orden() == null ? 0 : s.orden());
    }

    private static Set<Long> componentes(SolicitudCancha s) {
        return s.componentes() == null ? Set.of() : s.componentes();
    }

    private static DatosTarifa datosTarifa(SolicitudTarifa s) {
        return new DatosTarifa(MapeadorSedes.aDias(s.diasSemana()), MinutoDelDia.analizar(s.horaInicio()),
                MinutoDelDia.analizar(s.horaFin()), s.aplicaFestivo(), s.precioHora(), s.vigenteDesde(), s.vigenteHasta());
    }
}
