package co.edu.usco.goltime.compartido.dominio;

import java.util.regex.Pattern;

/**
 * Identificador anónimo de una cuenta de WhatsApp ({@code 164295400784036@lid}). WhatsApp lo envía en vez del
 * teléfono cuando no lo comparte. Es estable para cada cuenta, así que sirve para reconocer a quien escribe.
 * Se guarda solo la parte numérica.
 */
public record LidWhatsapp(String valor) {

    private static final Pattern VALIDO = Pattern.compile("\\d{6,30}");

    public LidWhatsapp {
        if (valor == null || !VALIDO.matcher(valor).matches()) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "El identificador de WhatsApp no es válido.");
        }
    }

    /** Acepta {@code 164295400784036} o {@code 164295400784036@lid}. */
    public static LidWhatsapp de(String texto) {
        if (texto == null) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "El identificador de WhatsApp no es válido.");
        }
        String limpio = texto.trim();
        if (limpio.endsWith("@lid")) {
            limpio = limpio.substring(0, limpio.length() - 4);
        }
        return new LidWhatsapp(limpio);
    }

    /** Destino para Evolution API: el chat {@code <lid>@lid}. */
    public String jid() {
        return valor + "@lid";
    }
}
