package co.edu.usco.goltime.reservas.infraestructura;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

import co.edu.usco.goltime.compartido.dominio.ZonaHoraria;
import co.edu.usco.goltime.reservas.aplicacion.Vistas.VistaReserva;
import co.edu.usco.goltime.reservas.dominio.Reserva;

/** Genera el CSV del listado de reservas (UTF-8 con BOM para que Excel respete las tildes, hora de Bogotá). */
final class ExportadorCsv {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZonaHoraria.BOGOTA);
    private static final String[] ENCABEZADO = {"Código", "Estado", "Origen", "Sede", "Cancha", "Cliente", "Correo", "Teléfono", "Equipo", "Inicio",
            "Fin", "Duración (min)", "Valor (COP)", "Pagado (COP)", "Estado de pago", "Creada"};

    private ExportadorCsv() {
    }

    static byte[] aCsv(List<VistaReserva> reservas) {
        StringBuilder csv = new StringBuilder("﻿");
        csv.append(String.join(",", ENCABEZADO)).append("\r\n");
        for (VistaReserva v : reservas) {
            Reserva r = v.reserva();
            var rel = v.relacionados();
            csv.append(String.join(",", celda(r.codigo()), celda(r.estado().name()), celda(r.origen().name()), celda(rel.sedeNombre()),
                    celda(rel.canchaNombre()), celda(rel.clienteNombre()), celda(rel.clienteCorreo()), celda(rel.clienteTelefono()),
                    celda(r.nombreEquipo()), celda(fecha(r.inicio())), celda(fecha(r.fin())), String.valueOf(r.duracionMin()),
                    String.valueOf(r.precio().finalAPagar()), String.valueOf(r.montoPagado()), celda(r.estadoPago().name()),
                    celda(fecha(r.creadoEn())))).append("\r\n");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String fecha(Instant instante) {
        return instante == null ? "" : FECHA_HORA.format(instante);
    }

    /** Escapa comillas y neutraliza fórmulas (=, +, -, @) para evitar inyección de fórmulas al abrir en Excel. */
    static String celda(String valor) {
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
