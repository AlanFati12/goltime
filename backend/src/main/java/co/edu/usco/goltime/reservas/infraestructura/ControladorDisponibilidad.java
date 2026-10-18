package co.edu.usco.goltime.reservas.infraestructura;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.usco.goltime.compartido.dominio.ZonaHoraria;
import co.edu.usco.goltime.reservas.aplicacion.ServicioDisponibilidad;
import co.edu.usco.goltime.reservas.infraestructura.DtosReservas.RespuestaDisponibilidad;
import co.edu.usco.goltime.reservas.infraestructura.DtosReservas.RespuestaResumenDia;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/publico/disponibilidad")
@Tag(name = "Público: disponibilidad", description = "Horarios libres y ocupados por cancha")
public class ControladorDisponibilidad {

    private final ServicioDisponibilidad disponibilidad;

    public ControladorDisponibilidad(ServicioDisponibilidad disponibilidad) {
        this.disponibilidad = disponibilidad;
    }

    @GetMapping
    @Operation(summary = "Disponibilidad de un día",
            description = "Por cancha reservable: cada bloque del horario con su estado (DISPONIBLE, OCUPADO, BLOQUEADO o NO_RESERVABLE) y su precio. "
                    + "Sin fecha usa hoy (hora de Bogotá).")
    public RespuestaDisponibilidad dia(@RequestParam(required = false) Long sedeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) String modalidad) {
        LocalDate dia = fecha != null ? fecha : ZonaHoraria.fechaLocal(java.time.Instant.now());
        return MapeadorReservas.aRespuesta(disponibilidad.delDia(sedeId, dia, modalidad));
    }

    @GetMapping("/semana")
    @Operation(summary = "Horarios libres de cada uno de los próximos 7 días")
    public List<RespuestaResumenDia> semana(@RequestParam(required = false) Long sedeId, @RequestParam(required = false) String modalidad) {
        return disponibilidad.proximosSieteDias(sedeId, modalidad).stream().map(MapeadorReservas::aRespuesta).toList();
    }
}
