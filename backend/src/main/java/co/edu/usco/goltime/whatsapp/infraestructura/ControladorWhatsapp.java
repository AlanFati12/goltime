package co.edu.usco.goltime.whatsapp.infraestructura;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.usco.goltime.compartido.dominio.FormatoTexto;
import co.edu.usco.goltime.reservas.aplicacion.Vistas.PrevisualizacionReprogramacion;
import co.edu.usco.goltime.reservas.aplicacion.Vistas.VistaReserva;
import co.edu.usco.goltime.reservas.dominio.EstadoReserva;
import co.edu.usco.goltime.reservas.dominio.Reserva;
import co.edu.usco.goltime.whatsapp.aplicacion.RemitenteWhatsapp;
import co.edu.usco.goltime.whatsapp.aplicacion.ServicioBotWhatsapp;
import co.edu.usco.goltime.whatsapp.aplicacion.ServicioBotWhatsapp.Franja;
import co.edu.usco.goltime.whatsapp.aplicacion.ServicioConsultasBot;
import co.edu.usco.goltime.whatsapp.aplicacion.ServicioContextoBot;
import co.edu.usco.goltime.whatsapp.aplicacion.ServicioContextoBot.Contexto;
import co.edu.usco.goltime.whatsapp.aplicacion.VistasBot.Cancha;
import co.edu.usco.goltime.whatsapp.aplicacion.VistasBot.Cotizacion;
import co.edu.usco.goltime.whatsapp.aplicacion.VistasBot.Disponibilidad;
import co.edu.usco.goltime.whatsapp.aplicacion.VistasBot.Partido;
import co.edu.usco.goltime.whatsapp.aplicacion.VistasBot.ResumenPartido;
import co.edu.usco.goltime.whatsapp.aplicacion.VistasBot.Tabla;
import co.edu.usco.goltime.whatsapp.aplicacion.VistasBot.Torneo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * API del bot de WhatsApp (ADR 0018). La usa solo n8n, con la clave de integración. El número de quien escribe
 * llega en {@code X-WhatsApp-Numero}, puesto por n8n desde el mensaje (nunca por el modelo de lenguaje): con él se
 * decide de quién son las reservas. Las horas van y vienen en hora de Bogotá.
 */
@RestController
@RequestMapping("/api/v1/integracion/whatsapp")
@Tag(name = "Integración: bot de WhatsApp", description = "Consultas y reservas del bot (n8n), con clave de integración")
public class ControladorWhatsapp {

    public static final String CABECERA_NUMERO = "X-WhatsApp-Numero";
    /** Si WhatsApp no compartió el teléfono, n8n manda el identificador anónimo (@lid) de quien escribe. */
    public static final String CABECERA_LID = "X-WhatsApp-Lid";

    private final ServicioContextoBot contexto;
    private final ServicioBotWhatsapp bot;
    private final ServicioConsultasBot consultas;

    public ControladorWhatsapp(ServicioContextoBot contexto, ServicioBotWhatsapp bot, ServicioConsultasBot consultas) {
        this.contexto = contexto;
        this.bot = bot;
        this.consultas = consultas;
    }

    // ---------- Contexto y vinculación ----------

    @GetMapping("/contexto")
    @Operation(summary = "Quién escribe, fecha y hora de Bogotá, sedes y políticas vigentes")
    public Contexto contexto(@RequestHeader(value = CABECERA_NUMERO, required = false) String numero,
            @RequestHeader(value = CABECERA_LID, required = false) String lid) {
        return contexto.de(quien(numero, lid));
    }

    @PostMapping("/vinculacion/confirmar")
    @Operation(summary = "Vincula este WhatsApp con la cuenta que generó el código en su perfil de la web")
    public Vinculado confirmar(@RequestHeader(value = CABECERA_NUMERO, required = false) String numero,
            @RequestHeader(value = CABECERA_LID, required = false) String lid, @Valid @RequestBody SolicitudCodigo s) {
        return new Vinculado(bot.vincular(quien(numero, lid), s.codigo()));
    }

    // ---------- Canchas, horarios y precios ----------

    @GetMapping("/canchas")
    public List<Cancha> canchas(@RequestParam(required = false) Long sedeId, @RequestParam(required = false) String modalidad) {
        bot.verificarActivo();
        return consultas.canchas(sedeId, vacioANulo(modalidad));
    }

    @GetMapping("/disponibilidad")
    @Operation(summary = "Franjas libres de un día por cancha, con el precio por hora")
    public Disponibilidad disponibilidad(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) Long sedeId, @RequestParam(required = false) String modalidad) {
        bot.verificarActivo();
        return consultas.disponibilidad(fecha, sedeId, vacioANulo(modalidad));
    }

    @GetMapping("/cotizacion")
    @Operation(summary = "Precio exacto de jugar en una cancha una franja (calculado por el servidor)")
    public Cotizacion cotizacion(@RequestParam long canchaId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime desde, @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime hasta) {
        bot.verificarActivo();
        return consultas.cotizar(canchaId, new Franja(fecha, desde, hasta));
    }

    // ---------- Reservas de quien escribe ----------

    @PostMapping("/reservas")
    @Operation(summary = "Solicita una reserva (nace pendiente y la aprueba el administrador)",
            description = "Idempotency-Key: el id del mensaje de WhatsApp, para que un reintento no la cree dos veces.")
    public ResponseEntity<ReservaBot> solicitar(@RequestHeader(value = CABECERA_NUMERO, required = false) String numero,
            @RequestHeader(value = CABECERA_LID, required = false) String lid,
            @RequestHeader(value = "Idempotency-Key", required = false) String clave, @Valid @RequestBody SolicitudReserva s) {
        var resultado = bot.solicitar(quien(numero, lid), s.nombre(), s.telefonoContacto(), s.canchaId(), new Franja(s.fecha(), s.desde(), s.hasta()), s.nombreEquipo(), clave);
        return ResponseEntity.status(resultado.creada() ? HttpStatus.CREATED : HttpStatus.OK).body(reserva(resultado.vista()));
    }

    @GetMapping("/reservas")
    @Operation(summary = "Reservas pendientes y confirmadas por jugar de quien escribe")
    public List<ReservaBot> misReservas(@RequestHeader(value = CABECERA_NUMERO, required = false) String numero,
            @RequestHeader(value = CABECERA_LID, required = false) String lid) {
        return bot.misReservas(quien(numero, lid)).stream().map(ControladorWhatsapp::reserva).toList();
    }

    @PostMapping("/reservas/{codigo}/cancelar")
    public ReservaBot cancelar(@RequestHeader(value = CABECERA_NUMERO, required = false) String numero,
            @RequestHeader(value = CABECERA_LID, required = false) String lid, @PathVariable String codigo, @Valid @RequestBody SolicitudMotivo s) {
        return reserva(bot.cancelar(quien(numero, lid), codigo, s.motivo()));
    }

    @PostMapping("/reservas/{codigo}/reprogramar/previsualizar")
    @Operation(summary = "Si el nuevo horario está libre y cuánto cambia el precio, sin cambiar nada")
    public Previsualizacion previsualizar(@RequestHeader(value = CABECERA_NUMERO, required = false) String numero,
            @RequestHeader(value = CABECERA_LID, required = false) String lid, @PathVariable String codigo,
            @Valid @RequestBody SolicitudReprogramacion s) {
        PrevisualizacionReprogramacion p = bot.previsualizarReprogramacion(quien(numero, lid), codigo, s.canchaId(), new Franja(s.fecha(), s.desde(), s.hasta()));
        return new Previsualizacion(p.disponible(), p.precioActual(), p.precioNuevo(), p.diferencia(), FormatoTexto.cop(p.precioNuevo()));
    }

    @PostMapping("/reservas/{codigo}/reprogramar")
    public ReservaBot reprogramar(@RequestHeader(value = CABECERA_NUMERO, required = false) String numero,
            @RequestHeader(value = CABECERA_LID, required = false) String lid, @PathVariable String codigo,
            @Valid @RequestBody SolicitudReprogramacion s) {
        return reserva(bot.reprogramar(quien(numero, lid), codigo, s.canchaId(), new Franja(s.fecha(), s.desde(), s.hasta()), s.motivo()));
    }

    // ---------- Torneos y partidos (solo lo público) ----------

    @GetMapping("/torneos")
    @Operation(summary = "Torneos con inscripciones abiertas o en curso")
    public List<Torneo> torneos() {
        bot.verificarActivo();
        return consultas.torneosActivos();
    }

    @GetMapping("/torneos/{id}/tabla")
    public List<Tabla> tabla(@PathVariable long id) {
        bot.verificarActivo();
        return consultas.tabla(id);
    }

    @GetMapping("/torneos/{id}/partidos")
    @Operation(summary = "Últimos resultados (jugados=true) o próximos partidos del torneo; máximo 10")
    public List<Partido> partidos(@PathVariable long id, @RequestParam(defaultValue = "false") boolean jugados) {
        bot.verificarActivo();
        return consultas.partidos(id, jugados);
    }

    @GetMapping("/partidos/proximos")
    public List<Partido> proximos() {
        bot.verificarActivo();
        return consultas.proximosPartidos();
    }

    @GetMapping("/partidos/{id}/resumen")
    @Operation(summary = "Marcador, goles del partido (jugador y minuto) y expulsiones, con un resumen ya redactado")
    public ResumenPartido resumen(@PathVariable long id) {
        bot.verificarActivo();
        return consultas.resumen(id);
    }

    /** El modelo de lenguaje manda los filtros opcionales vacíos en vez de omitirlos. */
    private static String vacioANulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private static RemitenteWhatsapp quien(String numero, String lid) {
        return RemitenteWhatsapp.de(numero, lid);
    }

    // ---------- DTOs ----------

    public record SolicitudCodigo(@NotBlank @Size(max = 12) String codigo) {
    }

    public record Vinculado(String nombre) {
    }

    /**
     * {@code nombre} solo hace falta la primera vez que reserva alguien sin cuenta; {@code telefonoContacto}, además,
     * si escribe desde un @lid (WhatsApp no compartió su número).
     */
    public record SolicitudReserva(@NotNull Long canchaId, @NotNull LocalDate fecha, @NotNull LocalTime desde,
            @NotNull LocalTime hasta, @Size(max = 150) String nombre, @Size(max = 30) String telefonoContacto, @Size(max = 100) String nombreEquipo) {
    }

    public record SolicitudMotivo(@Size(max = 500) String motivo) {
    }

    public record SolicitudReprogramacion(Long canchaId, @NotNull LocalDate fecha, @NotNull LocalTime desde, @NotNull LocalTime hasta,
            @Size(max = 500) String motivo) {
    }

    public record Previsualizacion(boolean disponible, int precioActual, int precioNuevo, int diferencia, String precioNuevoTexto) {
    }

    /** Reserva tal como la cuenta el bot: fechas en Bogotá, estado en palabras y lo que aún se puede hacer. */
    public record ReservaBot(String codigo, String estado, String cancha, String sede, String fecha, String desde, String hasta, int precio,
            String precioTexto, String expira, boolean puedeCancelar, boolean puedeReprogramar, boolean cancelacionSeriaTardia) {
    }

    static ReservaBot reserva(VistaReserva v) {
        Reserva r = v.reserva();
        var rel = v.relacionados();
        String expira = r.estado() == EstadoReserva.PENDIENTE && r.expiraEn() != null ? FormatoTexto.fechaHora(r.expiraEn()) : null;
        return new ReservaBot(r.codigo(), estado(r.estado()), rel == null ? null : rel.canchaNombre(), rel == null ? null : rel.sedeNombre(),
                FormatoTexto.fechaHora(r.inicio()), FormatoTexto.hora(r.inicio()), FormatoTexto.hora(r.fin()), r.precio().finalAPagar(),
                FormatoTexto.cop(r.precio().finalAPagar()), expira, v.puedeCancelar(), v.puedeReprogramar(), v.cancelacionSeriaTardia());
    }

    private static String estado(EstadoReserva e) {
        return switch (e) {
            case PENDIENTE -> "Pendiente de aprobación";
            case CONFIRMADA -> "Confirmada";
            case RECHAZADA -> "Rechazada";
            case EXPIRADA -> "Expirada";
            case CANCELADA -> "Cancelada";
            case COMPLETADA -> "Jugada";
            case NO_ASISTIO -> "No asistió";
        };
    }
}
