package co.edu.usco.goltime.reservas.infraestructura;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.usco.goltime.reservas.aplicacion.ServicioAgenda;
import co.edu.usco.goltime.reservas.infraestructura.DtosReservas.RespuestaAgenda;
import co.edu.usco.goltime.reservas.infraestructura.DtosReservas.RespuestaColumnaAgenda;
import co.edu.usco.goltime.reservas.infraestructura.DtosReservas.RespuestaElementoAgenda;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/admin/agenda")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: agenda", description = "Vista de calendario de reservas y bloqueos por cancha")
public class ControladorAgenda {

    private final ServicioAgenda agenda;

    public ControladorAgenda(ServicioAgenda agenda) {
        this.agenda = agenda;
    }

    @GetMapping
    @Operation(summary = "Agenda de una sede entre dos fechas (máximo 31 días)",
            description = "Cada cuadro es una ocupación: una reserva o un bloqueo. Una reserva de una cancha compuesta aparece también en sus componentes.")
    public RespuestaAgenda agenda(@RequestParam long sedeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        var resultado = agenda.delRango(sedeId, desde, hasta);
        return new RespuestaAgenda(
                resultado.canchas().stream().map(v -> new RespuestaColumnaAgenda(v.cancha().id(), v.cancha().nombre(), v.cancha().modalidad(),
                        v.cancha().estado().name(), v.cancha().esCompuesta())).toList(),
                resultado.elementos().stream().map(e -> new RespuestaElementoAgenda(e.tipo(), e.canchaId(), e.inicio(), e.fin(), e.reservaId(),
                        e.codigo(), e.estado(), e.cliente(), e.equipo(), e.precioFinal(), e.estadoPago(), e.origen(), e.canchaReservaNombre(),
                        e.bloqueoId(), e.motivoBloqueo())).toList());
    }
}
