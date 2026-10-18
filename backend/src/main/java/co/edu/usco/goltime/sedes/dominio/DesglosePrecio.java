package co.edu.usco.goltime.sedes.dominio;

import java.time.Instant;
import java.util.List;

/**
 * Resultado de calcular el precio de un intervalo: total en COP (sin decimales) y los tramos que lo
 * componen. Sirve para mostrar el desglose y para guardar la instantánea del precio en la reserva.
 */
public record DesglosePrecio(long total, List<Tramo> tramos) {

    public enum Origen {
        BASE, TARIFA, FESTIVO
    }

    /** Parte del intervalo con un mismo precio por hora. */
    public record Tramo(Instant inicio, Instant fin, int minutos, int precioHora, Origen origen) {
    }
}
