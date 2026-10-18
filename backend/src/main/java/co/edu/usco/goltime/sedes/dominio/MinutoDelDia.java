package co.edu.usco.goltime.sedes.dominio;

import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;

/**
 * Las horas del día se manejan como minutos desde la medianoche (0..1440), lo que permite expresar
 * un cierre a las 24:00. Convierte desde y hacia el texto "HH:mm" que usa la API.
 */
public final class MinutoDelDia {

    public static final int FIN_DEL_DIA = 1440;

    private MinutoDelDia() {
    }

    public static int analizar(String texto) {
        if (texto == null || !texto.matches("\\d{1,2}:\\d{2}")) {
            throw invalida(texto);
        }
        String[] partes = texto.split(":");
        int horas = Integer.parseInt(partes[0]);
        int minutos = Integer.parseInt(partes[1]);
        if (minutos > 59 || horas > 24 || (horas == 24 && minutos != 0)) {
            throw invalida(texto);
        }
        return horas * 60 + minutos;
    }

    public static String formatear(int minuto) {
        return "%02d:%02d".formatted(minuto / 60, minuto % 60);
    }

    private static ExcepcionNegocio invalida(String texto) {
        return new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "La hora '" + texto + "' no es válida. Usa el formato HH:mm.");
    }
}
