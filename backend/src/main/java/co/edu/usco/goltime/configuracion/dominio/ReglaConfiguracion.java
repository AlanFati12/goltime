package co.edu.usco.goltime.configuracion.dominio;

import java.util.Map;
import java.util.Optional;

import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;

/**
 * Qué valores admite cada clave editable. Una clave que no está aquí no se puede modificar desde la interfaz (por ejemplo las que
 * el sistema agrega en una migración y todavía no tienen pantalla). Valida y normaliza el texto recibido.
 */
public record ReglaConfiguracion(Tipo tipo, int minimo, int maximo) {

    public enum Tipo {
        ENTERO, BOOLEANO, TEXTO
    }

    private static final Map<String, ReglaConfiguracion> REGLAS = Map.ofEntries(
            Map.entry("horas_minimas_cancelacion", new ReglaConfiguracion(Tipo.ENTERO, 0, 168)),
            Map.entry("max_reprogramaciones", new ReglaConfiguracion(Tipo.ENTERO, 0, 10)),
            Map.entry("antelacion_minima_horas", new ReglaConfiguracion(Tipo.ENTERO, 0, 72)),
            Map.entry("antelacion_maxima_dias", new ReglaConfiguracion(Tipo.ENTERO, 1, 365)),
            Map.entry("max_reservas_activas_cliente", new ReglaConfiguracion(Tipo.ENTERO, 1, 50)),
            Map.entry("horas_recordatorio", new ReglaConfiguracion(Tipo.ENTERO, 0, 72)),
            Map.entry("horas_expiracion_aprobacion", new ReglaConfiguracion(Tipo.ENTERO, 1, 168)),
            Map.entry("horas_expiracion_antes_inicio", new ReglaConfiguracion(Tipo.ENTERO, 0, 72)),
            Map.entry("completar_reservas_automaticamente", new ReglaConfiguracion(Tipo.BOOLEANO, 0, 0)),
            Map.entry("modalidades_permitidas", new ReglaConfiguracion(Tipo.TEXTO, 0, 100)),
            Map.entry("contacto_negocio", new ReglaConfiguracion(Tipo.TEXTO, 0, 1000)),
            Map.entry("terminos_y_condiciones", new ReglaConfiguracion(Tipo.TEXTO, 0, 1000)));

    public static Optional<ReglaConfiguracion> de(String clave) {
        return Optional.ofNullable(REGLAS.get(clave));
    }

    /** Devuelve el valor en su forma canónica o lanza un error de negocio si no es válido para la clave. */
    public String validar(String clave, String valor) {
        String v = valor == null ? "" : valor.trim();
        switch (tipo) {
            case ENTERO -> {
                if (!v.matches("-?\\d{1,6}")) {
                    throw invalido(clave, "debe ser un número entero");
                }
                int n = Integer.parseInt(v);
                if (n < minimo || n > maximo) {
                    throw invalido(clave, "debe estar entre " + minimo + " y " + maximo);
                }
                return String.valueOf(n);
            }
            case BOOLEANO -> {
                if (!v.equalsIgnoreCase("true") && !v.equalsIgnoreCase("false")) {
                    throw invalido(clave, "debe ser verdadero o falso");
                }
                return v.toLowerCase();
            }
            default -> {
                if (v.length() > maximo) {
                    throw invalido(clave, "no puede superar " + maximo + " caracteres");
                }
                if ("modalidades_permitidas".equals(clave) && !v.matches("(?:\\d{1,2})(?:,\\d{1,2})*")) {
                    throw invalido(clave, "debe ser una lista de números separados por coma, por ejemplo 5,6,7");
                }
                return v;
            }
        }
    }

    private static ExcepcionNegocio invalido(String clave, String motivo) {
        return new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "El valor de «" + clave + "» " + motivo + ".");
    }
}
