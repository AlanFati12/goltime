package co.edu.usco.goltime.whatsapp.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import co.edu.usco.goltime.reservas.dominio.AvisoReservaEvento;
import co.edu.usco.goltime.reservas.dominio.OrigenReserva;

class AvisosWhatsappTest {

    private static AvisoReservaEvento evento(String tipo, String motivo) {
        // 20:00 en Bogotá
        return new AvisoReservaEvento(1L, "GT-2026-000001", tipo, null, 2L, OrigenReserva.WHATSAPP, "+573001234567", "Cancha 1",
                Instant.parse("2026-11-20T01:00:00Z"), motivo);
    }

    @Test
    void cadaAvisoTieneSuTextoConLaHoraDeBogota() {
        assertThat(AvisosWhatsapp.mensaje(evento("RESERVA_CONFIRMADA", null)))
                .contains("GT-2026-000001").contains("confirmada").contains("19/11/2026 20:00").contains("Cancha 1");
        assertThat(AvisosWhatsapp.mensaje(evento("RESERVA_RECHAZADA", "Cancha en mantenimiento"))).endsWith("Motivo: Cancha en mantenimiento");
        assertThat(AvisosWhatsapp.mensaje(evento("RESERVA_CANCELADA_ADMIN", " "))).doesNotContain("Motivo");
    }

    @Test
    void losAvisosQueNoSonDeWhatsappNoTienenTexto() {
        assertThat(AvisosWhatsapp.mensaje(evento("RESERVA_SOLICITADA", null))).isNull();
    }
}
