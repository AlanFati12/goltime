package co.edu.usco.goltime.autenticacion.infraestructura;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.usco.goltime.autenticacion.aplicacion.ServicioVinculacionWhatsapp;
import co.edu.usco.goltime.autenticacion.aplicacion.ServicioVinculacionWhatsapp.CodigoGenerado;
import co.edu.usco.goltime.compartido.aplicacion.LectorConfiguracion;
import co.edu.usco.goltime.usuarios.infraestructura.UsuarioResumen;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Vincular el WhatsApp de la cuenta con el bot (ADR 0018). El id sale siempre del token (evita IDOR). */
@RestController
@RequestMapping("/api/v1/me/whatsapp")
@Tag(name = "Perfil: WhatsApp", description = "Código para que el bot de WhatsApp reconozca la cuenta")
public class ControladorVinculacionWhatsapp {

    private final ServicioVinculacionWhatsapp vinculacion;
    private final LectorConfiguracion configuracion;

    public ControladorVinculacionWhatsapp(ServicioVinculacionWhatsapp vinculacion, LectorConfiguracion configuracion) {
        this.vinculacion = vinculacion;
        this.configuracion = configuracion;
    }

    /**
     * @param numeroBot el WhatsApp del bot (vacío si el administrador aún no lo configuró)
     * @param enlace    abre el chat con el bot y el código ya escrito (solo si hay número)
     */
    public record RespuestaCodigo(String codigo, Instant expiraEn, String numeroBot, String enlace) {
    }

    @PostMapping("/codigo")
    @Operation(summary = "Generar un código para vincular mi WhatsApp",
            description = "Vence a los 10 minutos y sirve una vez. Se envía al bot desde el WhatsApp que se quiere vincular.")
    public RespuestaCodigo generar(@AuthenticationPrincipal Jwt jwt) {
        CodigoGenerado c = vinculacion.generarCodigo(Long.parseLong(jwt.getSubject()));
        String numero = configuracion.texto("whatsapp_numero_negocio").map(String::trim).filter(t -> !t.isEmpty()).orElse(null);
        String enlace = numero == null ? null
                : "https://wa.me/" + numero.replaceAll("\\D", "") + "?text="
                        + URLEncoder.encode("Hola GolBot, mi código para vincular mi cuenta es " + c.codigo(), StandardCharsets.UTF_8)
                                .replace("+", "%20");
        return new RespuestaCodigo(c.codigo(), c.expiraEn(), numero, enlace);
    }

    @DeleteMapping
    @Operation(summary = "Desvincular mi WhatsApp del bot")
    public UsuarioResumen desvincular(@AuthenticationPrincipal Jwt jwt) {
        return UsuarioResumen.de(vinculacion.desvincular(Long.parseLong(jwt.getSubject())));
    }
}
