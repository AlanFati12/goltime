package co.edu.usco.goltime.torneos.dominio;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/** Decide el orden de siembra de las inscripciones antes de generar el fixture. */
public final class OrdenadorParticipantes {

    private OrdenadorParticipantes() {
    }

    public record Entrada(long inscripcionId, Integer semilla) {
    }

    /**
     * MANUAL respeta el orden recibido; ALEATORIO sortea; SEMBRADO ordena por cabeza de serie
     * (las que no tienen semilla van al final, en el orden recibido).
     */
    public static List<Long> ordenar(ModoEmparejamiento modo, List<Entrada> entradas, Random azar) {
        List<Entrada> copia = new ArrayList<>(entradas);
        switch (modo) {
            case MANUAL -> {
                // sin cambios
            }
            case ALEATORIO -> java.util.Collections.shuffle(copia, azar);
            case SEMBRADO -> copia.sort(Comparator.comparing(Entrada::semilla, Comparator.nullsLast(Comparator.naturalOrder())));
        }
        return copia.stream().map(Entrada::inscripcionId).toList();
    }
}
