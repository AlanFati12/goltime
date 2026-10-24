package co.edu.usco.goltime.whatsapp.aplicacion;

import java.util.List;

/**
 * Respuestas del bot: cortas, con horas y fechas en hora de Bogotá ya formateadas y precios en COP con su
 * texto. Así el modelo de lenguaje no calcula ni convierte nada y gasta pocos tokens.
 */
public final class VistasBot {

    private VistasBot() {
    }

    public record Cancha(long id, String nombre, String modalidad, long sedeId, String sede, int duracionMinMin, int duracionMaxMin,
            int precioBaseHora) {
    }

    /** Horas seguidas libres de una cancha; {@code precioHora} es el más bajo y el más alto dentro de la franja. */
    public record FranjaLibre(String desde, String hasta, int precioHoraMin, int precioHoraMax) {
    }

    public record DisponibilidadCancha(long canchaId, String cancha, String modalidad, String sede, int duracionMinMin, int duracionMaxMin,
            List<FranjaLibre> franjasLibres) {
    }

    public record Disponibilidad(String fecha, String dia, List<DisponibilidadCancha> canchas) {
    }

    public record Cotizacion(long canchaId, String cancha, String sede, String fecha, String desde, String hasta, long total, String totalTexto) {
    }

    public record Torneo(long id, String nombre, String estado, String sede, String modalidad, String fechaInicio, String fechaFin,
            int equiposInscritos, Integer cupoEquipos, int valorInscripcion) {
    }

    public record FilaTabla(int posicion, String equipo, int jugados, int ganados, int empatados, int perdidos, int golesFavor,
            int golesContra, int diferencia, int puntos) {
    }

    public record Tabla(String fase, String grupo, List<FilaTabla> filas) {
    }

    public record Partido(long id, String torneo, String fase, String fecha, String local, String visitante, String marcador,
            String estado, String cancha) {
    }

    public record Gol(Integer minuto, String jugador, String equipo, String tipo) {
    }

    public record Expulsion(Integer minuto, String jugador, String equipo) {
    }

    /** {@code resumen} ya viene redactado (marcador y goles) para que el bot lo repita sin inventar. */
    public record ResumenPartido(Partido partido, List<Gol> goles, List<Expulsion> expulsiones, String figura, String resumen) {
    }
}
