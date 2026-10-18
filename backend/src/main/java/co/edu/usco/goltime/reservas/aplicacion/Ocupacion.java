package co.edu.usco.goltime.reservas.aplicacion;

import java.time.Instant;

/** Rango de tiempo en que una cancha está ocupada, por una reserva o (Fase 4) por un bloqueo. */
public record Ocupacion(long canchaId, Instant inicio, Instant fin, String tipo) {

    public boolean seSolapaCon(Instant desde, Instant hasta) {
        return inicio.isBefore(hasta) && desde.isBefore(fin);
    }
}
