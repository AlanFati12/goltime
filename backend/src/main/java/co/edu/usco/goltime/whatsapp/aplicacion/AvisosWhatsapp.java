package co.edu.usco.goltime.whatsapp.aplicacion;

import java.util.Optional;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import co.edu.usco.goltime.autenticacion.aplicacion.ServicioVinculacionWhatsapp;
import co.edu.usco.goltime.compartido.dominio.FormatoTexto;
import co.edu.usco.goltime.reservas.aplicacion.ServicioClientesSinCuenta;
import co.edu.usco.goltime.reservas.dominio.AvisoReservaEvento;
import co.edu.usco.goltime.reservas.dominio.EstadoReserva;
import co.edu.usco.goltime.reservas.dominio.OrigenReserva;
import co.edu.usco.goltime.whatsapp.aplicacion.EnviadorWhatsapp.AvisoWhatsapp;

/**
 * Avisa por WhatsApp los cambios de una reserva, después de confirmar la transacción y sin hacer esperar a quien
 * hizo el cambio. Solo a quien vinculó su WhatsApp a la cuenta (teléfono o @lid), o a quien reservó por WhatsApp sin
 * cuenta (a su chat @lid si escribió desde uno): nunca a un número que alguien escribió en el registro sin demostrar
 * que es suyo. Los textos salen de aquí, no del modelo de lenguaje, así que no cuestan tokens ni pueden inventar nada.
 */
@Component
public class AvisosWhatsapp {

    /** Cambio de estado hecho por el administrador (ADR 0019): {@code RESERVA_ESTADO_COMPLETADA}, etc. */
    private static final String PREFIJO_CAMBIO_ESTADO = "RESERVA_ESTADO_";

    private final ServicioVinculacionWhatsapp verificacion;
    private final ServicioClientesSinCuenta sinCuenta;
    private final ConfiguracionBot configuracion;
    private final EnviadorWhatsapp enviador;

    public AvisosWhatsapp(ServicioVinculacionWhatsapp verificacion, ServicioClientesSinCuenta sinCuenta, ConfiguracionBot configuracion,
            EnviadorWhatsapp enviador) {
        this.verificacion = verificacion;
        this.sinCuenta = sinCuenta;
        this.configuracion = configuracion;
        this.enviador = enviador;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void alAvisarDeUnaReserva(AvisoReservaEvento e) {
        String mensaje = mensaje(e);
        if (mensaje == null || !configuracion.activo()) {
            return;
        }
        destino(e).ifPresent(numero -> enviador.enviar(new AvisoWhatsapp(numero, mensaje, e.tipo(), e.reservaId())));
    }

    /** Teléfono (+57…) o chat @lid; vacío si no le corresponde aviso por WhatsApp. */
    private Optional<String> destino(AvisoReservaEvento e) {
        if (e.clienteId() != null) {
            return verificacion.destinoWhatsapp(e.clienteId());
        }
        if (e.origen() == OrigenReserva.WHATSAPP && e.clienteMostradorId() != null) {
            return sinCuenta.destinoWhatsapp(e.clienteMostradorId(), e.telefono());
        }
        return Optional.empty();
    }

    static String mensaje(AvisoReservaEvento e) {
        String cuando = FormatoTexto.fechaHora(e.inicio());
        String donde = e.cancha() == null ? "" : " en " + e.cancha();
        String motivo = e.motivo() == null || e.motivo().isBlank() ? "" : " Motivo: " + e.motivo().trim();
        if (e.tipo().startsWith(PREFIJO_CAMBIO_ESTADO)) {
            return estado(e.tipo().substring(PREFIJO_CAMBIO_ESTADO.length()))
                    .map(estado -> "El administrador marcó tu reserva %s del %s como %s.%s".formatted(e.codigo(), cuando, estado.etiqueta(), motivo))
                    .orElse(null);
        }
        return switch (e.tipo()) {
            case "RESERVA_CONFIRMADA" -> "✅ ¡Tu reserva %s quedó confirmada! Te esperamos el %s%s.".formatted(e.codigo(), cuando, donde);
            case "RESERVA_RECHAZADA" -> "Tu solicitud %s para el %s no fue aprobada.%s".formatted(e.codigo(), cuando, motivo);
            case "RESERVA_EXPIRADA" -> "Tu solicitud %s para el %s no alcanzó a ser aprobada y el horario se liberó. Si quieres, la pedimos de nuevo."
                    .formatted(e.codigo(), cuando);
            case "RESERVA_CANCELADA_ADMIN" -> "El administrador canceló tu reserva %s del %s.%s".formatted(e.codigo(), cuando, motivo);
            case "RESERVA_REPROGRAMADA_ADMIN" -> "El administrador movió tu reserva %s al %s%s.".formatted(e.codigo(), cuando, donde);
            case "RESERVA_RECORDATORIO" -> "⏰ Recuerda tu partido: reserva %s, %s%s.".formatted(e.codigo(), cuando, donde);
            default -> null;
        };
    }

    private static Optional<EstadoReserva> estado(String nombre) {
        try {
            return Optional.of(EstadoReserva.valueOf(nombre));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
