package co.edu.usco.goltime.whatsapp.infraestructura;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.usco.goltime.compartido.aplicacion.LectorConfiguracion;
import co.edu.usco.goltime.whatsapp.aplicacion.ConfiguracionBot;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Lo que la web pública necesita saber del bot: si hay uno atendiendo y el enlace para abrir su chat. */
@RestController
@RequestMapping("/api/v1/publico/whatsapp")
@Tag(name = "WhatsApp", description = "Disponibilidad del bot para mostrarlo en la web")
public class ControladorPublicoWhatsapp {

    private final PropiedadesWhatsapp propiedades;
    private final ConfiguracionBot bot;
    private final LectorConfiguracion configuracion;

    public ControladorPublicoWhatsapp(PropiedadesWhatsapp propiedades, ConfiguracionBot bot, LectorConfiguracion configuracion) {
        this.propiedades = propiedades;
        this.bot = bot;
        this.configuracion = configuracion;
    }

    /**
     * @param disponible el bot está integrado, activo y tiene número configurado
     * @param enlace     abre el chat con el bot con un saludo ya escrito (solo si está disponible)
     */
    public record Disponibilidad(boolean disponible, String enlace) {
    }

    @GetMapping
    @Operation(summary = "¿Hay bot de WhatsApp? Enlace a su chat")
    public Disponibilidad disponibilidad() {
        String numero = configuracion.texto("whatsapp_numero_negocio").map(String::trim).filter(t -> !t.isEmpty()).orElse(null);
        if (numero == null || !propiedades.integracionHabilitada() || !bot.activo()) {
            return new Disponibilidad(false, null);
        }
        String saludo = URLEncoder.encode("Hola GolBot, quiero reservar una cancha", StandardCharsets.UTF_8).replace("+", "%20");
        return new Disponibilidad(true, "https://wa.me/" + numero.replaceAll("\\D", "") + "?text=" + saludo);
    }
}
