package co.edu.usco.goltime.reservas.infraestructura;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
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

import co.edu.usco.goltime.compartido.aplicacion.ContextoActual;
import co.edu.usco.goltime.compartido.dominio.Actor;
import co.edu.usco.goltime.reservas.aplicacion.ServicioBloqueos;
import co.edu.usco.goltime.reservas.aplicacion.ServicioBloqueos.DatosBloqueo;
import co.edu.usco.goltime.reservas.aplicacion.ServicioBloqueos.PrevisualizacionBloqueo;
import co.edu.usco.goltime.reservas.dominio.Bloqueo;
import co.edu.usco.goltime.reservas.infraestructura.DtosReservas.RespuestaBloqueo;
import co.edu.usco.goltime.reservas.infraestructura.DtosReservas.RespuestaConflicto;
import co.edu.usco.goltime.reservas.infraestructura.DtosReservas.RespuestaPrevisualizacionBloqueo;
import co.edu.usco.goltime.reservas.infraestructura.DtosReservas.SolicitudBloqueo;
import co.edu.usco.goltime.sedes.dominio.MinutoDelDia;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/bloqueos")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: bloqueos", description = "Bloqueos de horario con previsualización y confirmación de conflictos")
public class ControladorBloqueos {

    private final ServicioBloqueos bloqueos;
    private final ContextoActual contexto;

    public ControladorBloqueos(ServicioBloqueos bloqueos, ContextoActual contexto) {
        this.bloqueos = bloqueos;
        this.contexto = contexto;
    }

    @GetMapping
    @Operation(summary = "Listar bloqueos (activos por defecto)")
    public List<RespuestaBloqueo> listar(@RequestParam(required = false) Long sedeId, @RequestParam(defaultValue = "true") boolean soloActivos) {
        return bloqueos.listar(sedeId, soloActivos).stream().map(ControladorBloqueos::aRespuesta).toList();
    }

    @GetMapping("/{id}")
    public RespuestaBloqueo detalle(@PathVariable long id) {
        return aRespuesta(bloqueos.detalle(id));
    }

    @PostMapping("/previsualizar")
    @Operation(summary = "Previsualiza un bloqueo: reservas activas que se verían afectadas y su huella",
            description = "Paso 1 del flujo. Si hay conflictos, el front muestra la lista y, al confirmar, envía la `huella` recibida.")
    public RespuestaPrevisualizacionBloqueo previsualizar(@Valid @RequestBody SolicitudBloqueo s) {
        return aRespuesta(bloqueos.previsualizar(datos(s)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear un bloqueo",
            description = "Con reservas afectadas exige `huellaConfirmada` igual a la de la previsualización (409 CONFLICTOS_SIN_CONFIRMAR si falta o cambió). "
                    + "Al confirmar, en una sola transacción cancela esas reservas (motivo \"Bloqueo de horario: ...\"), avisa a los clientes y ocupa las canchas.")
    public RespuestaBloqueo crear(@Valid @RequestBody SolicitudBloqueo s) {
        return aRespuesta(bloqueos.crear(actor(), datos(s), s.huellaConfirmada()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar un bloqueo (mismo flujo de conflictos que al crearlo)")
    public RespuestaBloqueo actualizar(@PathVariable long id, @Valid @RequestBody SolicitudBloqueo s) {
        return aRespuesta(bloqueos.actualizar(actor(), id, datos(s), s.huellaConfirmada()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar un bloqueo: libera las canchas (queda en la bitácora)")
    public void eliminar(@PathVariable long id) {
        bloqueos.eliminar(actor(), id);
    }

    private Actor actor() {
        return contexto.actor().orElseThrow();
    }

    private static DatosBloqueo datos(SolicitudBloqueo s) {
        boolean recurrente = s.tipo() == Bloqueo.Tipo.RECURRENTE;
        return new DatosBloqueo(s.sedeId(), s.canchaIds() == null ? Set.of() : s.canchaIds(), s.tipo(), s.inicio(), s.fin(), s.todoElDia(),
                s.diasSemana() == null ? Set.of() : s.diasSemana().stream().map(java.time.DayOfWeek::of).collect(Collectors.toSet()),
                recurrente ? MinutoDelDia.analizar(s.horaInicio()) : 0, recurrente ? MinutoDelDia.analizar(s.horaFin()) : 0, s.fechaDesde(),
                s.fechaHasta(), s.motivo(), s.detalle());
    }

    private static RespuestaBloqueo aRespuesta(Bloqueo b) {
        boolean recurrente = b.tipo() == Bloqueo.Tipo.RECURRENTE;
        return new RespuestaBloqueo(b.id(), b.sedeId(), b.canchaIds().stream().sorted().toList(), b.tipo(), b.inicio(), b.fin(), b.todoElDia(),
                b.diasSemana().stream().map(java.time.DayOfWeek::getValue).sorted().toList(),
                recurrente ? MinutoDelDia.formatear(b.minutoInicio()) : null, recurrente ? MinutoDelDia.formatear(b.minutoFin()) : null,
                b.fechaDesde(), b.fechaHasta(), b.motivoTipo(), b.motivoDetalle(), b.motivoTexto(), b.activo());
    }

    private static RespuestaPrevisualizacionBloqueo aRespuesta(PrevisualizacionBloqueo p) {
        return new RespuestaPrevisualizacionBloqueo(p.conflictos().stream().map(c -> new RespuestaConflicto(c.reservaId(), c.codigo(), c.cliente(),
                c.canchaNombre(), c.inicio(), c.fin(), c.precioFinal(), c.conPago(), c.estadoPago(), c.estado())).toList(), p.intervalos(),
                p.canchasAfectadas(), p.huella(), p.requiereConfirmacion());
    }
}
