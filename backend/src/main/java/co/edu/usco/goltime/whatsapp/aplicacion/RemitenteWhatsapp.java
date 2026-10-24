package co.edu.usco.goltime.whatsapp.aplicacion;

import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.compartido.dominio.LidWhatsapp;
import co.edu.usco.goltime.compartido.dominio.NumeroTelefono;

/**
 * Quién escribe, tal como lo entrega WhatsApp (n8n lo toma del mensaje, nunca del modelo de lenguaje): su teléfono
 * o, si WhatsApp no lo comparte, su identificador anónimo @lid. Exactamente uno de los dos.
 */
public record RemitenteWhatsapp(String telefono, String lid) {

    public RemitenteWhatsapp {
        if ((telefono == null) == (lid == null)) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Falta el número o el identificador de WhatsApp de quien escribe.");
        }
    }

    /** Con teléfono se usa el teléfono; el @lid solo cuando WhatsApp no compartió el número. */
    public static RemitenteWhatsapp de(String numero, String lid) {
        if (numero != null && !numero.isBlank()) {
            return new RemitenteWhatsapp(NumeroTelefono.de(numero).valor(), null);
        }
        if (lid != null && !lid.isBlank()) {
            return new RemitenteWhatsapp(null, LidWhatsapp.de(lid).valor());
        }
        return new RemitenteWhatsapp(null, null);
    }

    public boolean porLid() {
        return lid != null;
    }
}
