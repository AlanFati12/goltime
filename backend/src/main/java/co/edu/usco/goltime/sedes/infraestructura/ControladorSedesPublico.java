package co.edu.usco.goltime.sedes.infraestructura;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.usco.goltime.compartido.aplicacion.AlmacenArchivos;
import co.edu.usco.goltime.compartido.dominio.TipoImagen;
import co.edu.usco.goltime.sedes.aplicacion.ServicioCanchas;
import co.edu.usco.goltime.sedes.aplicacion.ServicioCotizacion;
import co.edu.usco.goltime.sedes.aplicacion.ServicioFestivos;
import co.edu.usco.goltime.sedes.aplicacion.ServicioSedes;
import co.edu.usco.goltime.sedes.infraestructura.DtosSedes.RespuestaCancha;
import co.edu.usco.goltime.sedes.infraestructura.DtosSedes.RespuestaCotizacion;
import co.edu.usco.goltime.sedes.infraestructura.DtosSedes.RespuestaFestivo;
import co.edu.usco.goltime.sedes.infraestructura.DtosSedes.RespuestaSede;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Consulta pública (sin sesión) de sedes, canchas, precios, festivos y fotos. */
@RestController
@RequestMapping("/api/v1/publico")
@Tag(name = "Público: sedes y canchas", description = "Catálogo de sedes y canchas visible sin iniciar sesión")
public class ControladorSedesPublico {

    private final ServicioSedes sedes;
    private final ServicioCanchas canchas;
    private final ServicioCotizacion cotizacion;
    private final ServicioFestivos festivos;
    private final AlmacenArchivos archivos;

    public ControladorSedesPublico(ServicioSedes sedes, ServicioCanchas canchas, ServicioCotizacion cotizacion,
            ServicioFestivos festivos, AlmacenArchivos archivos) {
        this.sedes = sedes;
        this.canchas = canchas;
        this.cotizacion = cotizacion;
        this.festivos = festivos;
        this.archivos = archivos;
    }

    @GetMapping("/sedes")
    @Operation(summary = "Sedes activas con sus canchas reservables")
    public List<RespuestaSede> sedes() {
        return sedes.listarPublicas().stream().map(MapeadorSedes::aRespuesta).toList();
    }

    @GetMapping("/sedes/{id}")
    @Operation(summary = "Detalle de una sede activa")
    public RespuestaSede sede(@PathVariable long id) {
        return MapeadorSedes.aRespuesta(sedes.detallePublico(id));
    }

    @GetMapping("/canchas")
    @Operation(summary = "Canchas reservables, filtradas por sede y/o modalidad (ej. F5)")
    public List<RespuestaCancha> canchas(@RequestParam(required = false) Long sedeId,
            @RequestParam(required = false) String modalidad) {
        return canchas.listarPublicas(sedeId, modalidad).stream().map(MapeadorSedes::aRespuesta).toList();
    }

    @GetMapping("/canchas/{id}")
    @Operation(summary = "Detalle de una cancha: características, tarifas, horario efectivo y fotos")
    public RespuestaCancha cancha(@PathVariable long id) {
        return MapeadorSedes.aRespuesta(canchas.detallePublico(id));
    }

    @GetMapping("/canchas/{id}/cotizacion")
    @Operation(summary = "Precio de jugar un intervalo, con desglose por franja",
            description = "Calculado siempre por el servidor. Las fechas van en ISO-8601 UTC (ej. 2026-11-20T00:00:00Z).")
    public RespuestaCotizacion cotizar(@PathVariable long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fin) {
        return MapeadorSedes.aRespuesta(cotizacion.cotizar(id, inicio, fin));
    }

    @GetMapping("/festivos")
    @Operation(summary = "Festivos de Colombia de un año")
    public List<RespuestaFestivo> festivos(@RequestParam(required = false) Integer anio) {
        int año = anio != null ? anio : LocalDate.now().getYear();
        return festivos.delAnio(año).stream().map(MapeadorSedes::aRespuesta).toList();
    }

    @GetMapping("/archivos/{nombre}")
    @Operation(summary = "Imagen subida (foto de sede o cancha)")
    public ResponseEntity<byte[]> archivo(@PathVariable String nombre) {
        return TipoImagen.dePorExtension(nombre)
                .flatMap(tipo -> archivos.leer(nombre).map(contenido -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(tipo.tipoMime()))
                        .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable())
                        .body(contenido)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
