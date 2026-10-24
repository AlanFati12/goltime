package co.edu.usco.goltime.whatsapp.aplicacion;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import co.edu.usco.goltime.autenticacion.aplicacion.ServicioVerificacionTelefono;
import co.edu.usco.goltime.autenticacion.aplicacion.ServicioVerificacionTelefono.Identificacion;
import co.edu.usco.goltime.autenticacion.aplicacion.ServicioVerificacionTelefono.Situacion;
import co.edu.usco.goltime.compartido.dominio.FormatoTexto;
import co.edu.usco.goltime.reservas.dominio.AvisoReservaEvento;
import co.edu.usco.goltime.reservas.dominio.OrigenReserva;
import co.edu.usco.goltime.whatsapp.aplicacion.EnviadorWhatsapp.AvisoWhatsapp;

/**
 * Avisa por WhatsApp los cambios de una reserva, después de confirmar la transacción y sin hacer esperar a quien
 * hizo el cambio. Solo a quien tiene el número vinculado a su cuenta, o a quien reservó por WhatsApp sin cuenta:
 * nunca a un número que alguien escribió en el registro sin demostrar que es suyo. Los textos salen de aquí, no
 * del modelo de lenguaje, así que no cuestan tokens ni pueden inventar nada.
 */
@Component
public class AvisosWhatsapp {

    private final ServicioVerificacionTelefono verificacion;
    private final ConfiguracionBot configuracion;
    private final EnviadorWhatsapp enviador;

    public AvisosWhatsapp(ServicioVerificacionTelefono verificacion, ConfiguracionBot configuracion, EnviadorWhatsapp enviador) {
        this.verificacion = verificacion;
        this.configuracion = configuracion;
        this.enviador = enviador;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void alAvisarDeUnaReserva(AvisoReservaEvento e) {
        String mensaje = mensaje(e);
        if (mensaje == null || e.telefono() == null || !configuracion.activo() || !destinatarioValido(e)) {
            return;
        }
        enviador.enviar(new AvisoWhatsapp(e.telefono(), mensaje, e.tipo(), e.reservaId()));
    }

    private boolean destinatarioValido(AvisoReservaEvento e) {
        if (e.clienteId() == null) {
            return e.origen() == OrigenReserva.WHATSAPP;
        }
        try {
            Identificacion id = verificacion.identificar(e.telefono());
            return id.situacion() == Situacion.CLIENTE && e.clienteId().equals(id.usuarioId());
        } catch (RuntimeException ex) {
            // Un teléfono viejo que no es un celular válido: no se avisa por WhatsApp.
            return false;
        }
    }

    static String mensaje(AvisoReservaEvento e) {
        String cuando = FormatoTexto.fechaHora(e.inicio());
        String donde = e.cancha() == null ? "" : " en " + e.cancha();
        String motivo = e.motivo() == null || e.motivo().isBlank() ? "" : " Motivo: " + e.motivo().trim();
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
}
