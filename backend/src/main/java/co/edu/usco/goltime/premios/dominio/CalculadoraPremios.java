package co.edu.usco.goltime.premios.dominio;

/**
 * Cuántos premios le corresponden a un cliente. Se calcula siempre desde el total de reservas completadas (no con contadores que se
 * actualizan a mano), así corregir una reserva a "no asistió" se refleja solo: sobran premios y se revocan los que no se hayan usado.
 */
public final class CalculadoraPremios {

    private CalculadoraPremios() {
    }

    /** Premios que le corresponden en total con {@code completadas} reservas. */
    public static int ganados(int completadas, int requeridas) {
        return requeridas <= 0 ? 0 : Math.max(completadas, 0) / requeridas;
    }

    /** Positivo: faltan premios por otorgar. Negativo: hay de más (una reserva completada se corrigió). */
    public static int diferencia(int completadas, int requeridas, int yaOtorgados) {
        return ganados(completadas, requeridas) - yaOtorgados;
    }

    /** Reservas completadas dentro del ciclo actual (0 .. requeridas - 1). */
    public static int enCiclo(int completadas, int requeridas) {
        return requeridas <= 0 ? 0 : Math.max(completadas, 0) % requeridas;
    }

    public static int faltan(int completadas, int requeridas) {
        return requeridas - enCiclo(completadas, requeridas);
    }
}
