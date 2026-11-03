package co.edu.usco.goltime.whatsapp.infraestructura;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parámetros del bot ({@code goltime.whatsapp.*}), siempre por variables de entorno. Sin
 * {@code claveIntegracion} la integración queda cerrada: toda petición del bot recibe 401.
 */
@ConfigurationProperties("goltime.whatsapp")
public record PropiedadesWhatsapp(String claveIntegracion, String urlAvisos, String secretoAvisos) {

    static final int MINIMO_CLAVE = 32;

    public boolean integracionHabilitada() {
        return claveIntegracion != null && claveIntegracion.length() >= MINIMO_CLAVE;
    }
}
