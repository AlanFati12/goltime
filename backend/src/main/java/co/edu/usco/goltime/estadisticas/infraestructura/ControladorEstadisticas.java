package co.edu.usco.goltime.estadisticas.infraestructura;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.usco.goltime.compartido.dominio.FormatoCsv;
import co.edu.usco.goltime.compartido.dominio.ZonaHoraria;
import co.edu.usco.goltime.estadisticas.aplicacion.Estadisticas.ClienteFrecuente;
import co.edu.usco.goltime.estadisticas.aplicacion.Estadisticas.FilaCancha;
import co.edu.usco.goltime.estadisticas.aplicacion.Estadisticas.Filtro;
import co.edu.usco.goltime.estadisticas.aplicacion.Estadisticas.HorasPico;
import co.edu.usco.goltime.estadisticas.aplicacion.Estadisticas.ResumenConComparacion;
import co.edu.usco.goltime.estadisticas.aplicacion.Estadisticas.Serie;
import co.edu.usco.goltime.estadisticas.aplicacion.ServicioEstadisticas;
import co.edu.usco.goltime.estadisticas.dominio.Agrupacion;
import co.edu.usco.goltime.estadisticas.dominio.PeriodoEstadistico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/admin/estadisticas")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: estadísticas", description = "Ingresos, ocupación, horas pico y clientes frecuentes, calculados en el servidor")
public class ControladorEstadisticas {

    private static final int DIAS_POR_DEFECTO = 30;

    private final ServicioEstadisticas estadisticas;

    public ControladorEstadisticas(ServicioEstadisticas estadisticas) {
        this.estadisticas = estadisticas;
    }

    @GetMapping("/resumen")
    @Operation(summary = "Indicadores del periodo comparados con el periodo anterior de igual duración",
            description = "Ingresos reales = solo reservas COMPLETADA (sin torneos); recaudado = pagos menos reembolsos del periodo; "
                    + "ocupación = minutos reservados / minutos abiertos. Por defecto, los últimos 30 días.")
    public ResumenConComparacion resumen(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta, @RequestParam(required = false) Long sedeId,
            @RequestParam(required = false) Long canchaId) {
        return estadisticas.resumen(filtro(desde, hasta, sedeId, canchaId));
    }

    @GetMapping("/serie")
    @Operation(summary = "Ingresos y reservas por día, semana o mes (incluye las cubetas sin movimiento)")
    public Serie serie(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta, @RequestParam(required = false) Long sedeId,
            @RequestParam(required = false) Long canchaId, @RequestParam(defaultValue = "DIA") Agrupacion agrupacion) {
        return estadisticas.serie(filtro(desde, hasta, sedeId, canchaId), agrupacion);
    }

    @GetMapping("/canchas")
    @Operation(summary = "Ingresos, horas y ocupación de cada cancha")
    public List<FilaCancha> canchas(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta, @RequestParam(required = false) Long sedeId,
            @RequestParam(required = false) Long canchaId) {
        return estadisticas.porCancha(filtro(desde, hasta, sedeId, canchaId));
    }

    @GetMapping("/horas-pico")
    @Operation(summary = "Reservas por día de la semana y hora (mapa de calor); cada reserva cuenta en todas las horas que cubre")
    public HorasPico horasPico(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta, @RequestParam(required = false) Long sedeId,
            @RequestParam(required = false) Long canchaId) {
        return estadisticas.horasPico(filtro(desde, hasta, sedeId, canchaId));
    }

    @GetMapping("/clientes-frecuentes")
    @Operation(summary = "Los 10 clientes con más reservas completadas en el periodo")
    public List<ClienteFrecuente> clientes(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta, @RequestParam(required = false) Long sedeId,
            @RequestParam(required = false) Long canchaId) {
        return estadisticas.clientesFrecuentes(filtro(desde, hasta, sedeId, canchaId));
    }

    @GetMapping(value = "/exportar", produces = "text/csv")
    @Operation(summary = "Exporta a CSV el resumen del periodo, la serie de ingresos y el detalle por cancha")
    public ResponseEntity<byte[]> exportar(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta, @RequestParam(required = false) Long sedeId,
            @RequestParam(required = false) Long canchaId, @RequestParam(defaultValue = "DIA") Agrupacion agrupacion) {
        Filtro f = filtro(desde, hasta, sedeId, canchaId);
        var resumen = estadisticas.resumen(f).actual();
        StringBuilder csv = new StringBuilder(FormatoCsv.BOM);
        csv.append("Periodo,").append(f.periodo().desde()).append(',').append(f.periodo().hasta()).append("\r\n\r\n");
        csv.append("Indicador,Valor\r\n");
        fila(csv, "Ingresos reales (COP)", resumen.ingresosReales());
        fila(csv, "Recaudado (COP)", resumen.recaudado());
        fila(csv, "Por cobrar (COP)", resumen.porCobrar());
        fila(csv, "Reservas", resumen.reservas());
        fila(csv, "Completadas", resumen.completadas());
        fila(csv, "Canceladas", resumen.canceladas());
        fila(csv, "No asistió", resumen.noAsistio());
        fila(csv, "Tasa de cancelación (%)", resumen.tasaCancelacion());
        fila(csv, "Ocupación (%)", resumen.ocupacionPct());
        fila(csv, "Ticket promedio (COP)", resumen.ticketPromedio());
        csv.append("\r\n").append(agrupacion.name().charAt(0)).append(agrupacion.name().substring(1).toLowerCase())
                .append(",Ingresos (COP),Completadas,Canceladas\r\n");
        for (var p : estadisticas.serie(f, agrupacion).puntos()) {
            csv.append(p.fecha()).append(',').append(p.ingresos()).append(',').append(p.completadas()).append(',').append(p.canceladas()).append("\r\n");
        }
        csv.append("\r\nCancha,Sede,Completadas,Horas,Ingresos (COP),Ocupación (%)\r\n");
        for (var c : estadisticas.porCancha(f)) {
            csv.append(FormatoCsv.celda(c.cancha())).append(',').append(FormatoCsv.celda(c.sede())).append(',').append(c.completadas()).append(',')
                    .append(c.horas()).append(',').append(c.ingresos()).append(',').append(c.ocupacionPct()).append("\r\n");
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("estadisticas-goltime.csv").build().toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8)).body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void fila(StringBuilder csv, String indicador, Object valor) {
        csv.append(FormatoCsv.celda(indicador)).append(',').append(valor).append("\r\n");
    }

    private static Filtro filtro(LocalDate desde, LocalDate hasta, Long sedeId, Long canchaId) {
        LocalDate fin = hasta != null ? hasta : ZonaHoraria.fechaLocal(Instant.now());
        LocalDate inicio = desde != null ? desde : fin.minusDays(DIAS_POR_DEFECTO - 1);
        return new Filtro(new PeriodoEstadistico(inicio, fin), sedeId, canchaId);
    }
}
