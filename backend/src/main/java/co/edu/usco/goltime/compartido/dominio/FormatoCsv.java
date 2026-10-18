package co.edu.usco.goltime.compartido.dominio;

/** Reglas comunes para exportar a CSV de forma segura (Excel respeta las tildes con BOM y no ejecuta fórmulas). */
public final class FormatoCsv {

    /** Marca de orden de bytes UTF-8 al inicio del archivo. */
    public static final String BOM = "﻿";

    private FormatoCsv() {
    }

    /** Escapa comillas y neutraliza fórmulas (=, +, -, @) para evitar inyección de fórmulas al abrir en Excel. */
    public static String celda(String valor) {
        if (valor == null) {
            return "";
        }
        String texto = valor;
        if (!texto.isEmpty() && "=+-@\t\r".indexOf(texto.charAt(0)) >= 0) {
            texto = "'" + texto;
        }
        return "\"" + texto.replace("\"", "\"\"") + "\"";
    }
}
