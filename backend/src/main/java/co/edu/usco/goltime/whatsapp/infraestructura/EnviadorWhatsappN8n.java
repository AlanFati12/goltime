package co.edu.usco.goltime.whatsapp.infraestructura;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import co.edu.usco.goltime.whatsapp.aplicacion.EnviadorWhatsapp;

/**
 * Entrega los avisos al webhook de n8n ({@code WHATSAPP_URL_AVISOS}), que los manda por Evolution API. El cuerpo
 * va firmado con HMAC-SHA256 en {@code X-Firma-Goltime} para que n8n descarte lo que no venga de la API. Sin URL
 * o sin secreto no envía nada (Goltime funciona igual sin el bot).
 */
@Component
class EnviadorWhatsappN8n implements EnviadorWhatsapp {

    static final String CABECERA_FIRMA = "X-Firma-Goltime";
    private static final Logger LOG = LoggerFactory.getLogger(EnviadorWhatsappN8n.class);

    private final PropiedadesWhatsapp propiedades;
    private final ObjectMapper json;
    private final RestClient cliente;

    EnviadorWhatsappN8n(PropiedadesWhatsapp propiedades, ObjectMapper json) {
        this.propiedades = propiedades;
        this.json = json;
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(3));
        fabrica.setReadTimeout(Duration.ofSeconds(10));
        this.cliente = RestClient.builder().requestFactory(fabrica).build();
    }

    @Override
    public void enviar(AvisoWhatsapp aviso) {
        if (vacio(propiedades.urlAvisos()) || vacio(propiedades.secretoAvisos())) {
            LOG.debug("Avisos por WhatsApp sin configurar; no se envía {} de la reserva {}", aviso.tipo(), aviso.reservaId());
            return;
        }
        try {
            Map<String, Object> cuerpo = new LinkedHashMap<>();
            cuerpo.put("numero", aviso.numero());
            cuerpo.put("mensaje", aviso.mensaje());
            cuerpo.put("tipo", aviso.tipo());
            cuerpo.put("reservaId", aviso.reservaId());
            String texto = json.writeValueAsString(cuerpo);
            cliente.post().uri(propiedades.urlAvisos()).contentType(MediaType.APPLICATION_JSON)
                    .header(CABECERA_FIRMA, "sha256=" + firmar(texto, propiedades.secretoAvisos()))
                    .body(texto).retrieve().toBodilessEntity();
        } catch (RuntimeException | JsonProcessingException ex) {
            LOG.warn("No se pudo enviar el aviso {} de la reserva {} por WhatsApp: {}", aviso.tipo(), aviso.reservaId(), ex.getMessage());
        }
    }

    static String firmar(String cuerpo, String secreto) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(cuerpo.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("HmacSHA256 no disponible", ex);
        }
    }

    private static boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }
}
