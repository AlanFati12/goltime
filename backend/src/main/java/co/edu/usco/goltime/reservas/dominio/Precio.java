package co.edu.usco.goltime.reservas.dominio;

/** Instantánea del precio de una reserva (COP): cambios posteriores de tarifas no la alteran. */
public record Precio(int base, int descuento, int finalAPagar) {

    public static Precio sinDescuento(long total) {
        return new Precio((int) total, 0, (int) total);
    }
}
