package co.edu.usco.goltime.notificaciones.aplicacion;

import java.util.Map;

/**
 * Puerto de salida para enviar correos. Es asíncrono y tolerante a fallos: si el SMTP falla,
 * la operación de negocio que lo pidió no se revierte. Permite agregar WhatsApp/SMS con otros puertos.
 */
public interface EnviadorCorreo {

    void enviar(MensajeCorreo mensaje);

    /** {@code plantilla} es el nombre de la plantilla Thymeleaf (sin extensión). */
    record MensajeCorreo(String destinatario, String asunto, String plantilla, Map<String, Object> variables) {
    }
}
