package co.edu.usco.goltime.whatsapp.aplicacion;

/**
 * Puerto de salida: entregar un mensaje de WhatsApp. La API no habla con Evolution directamente; se lo pasa a n8n,
 * que es el único que tiene sus credenciales (y así cambiar de proveedor de WhatsApp no toca el backend).
 * Tolerante a fallos: si no se puede entregar, la operación que lo pidió no se revierte.
 */
public interface EnviadorWhatsapp {

    void enviar(AvisoWhatsapp aviso);

    /** {@code numero} en E.164; {@code tipo} y {@code reservaId} sirven para rastrear el aviso en n8n. */
    record AvisoWhatsapp(String numero, String mensaje, String tipo, Long reservaId) {
    }
}
