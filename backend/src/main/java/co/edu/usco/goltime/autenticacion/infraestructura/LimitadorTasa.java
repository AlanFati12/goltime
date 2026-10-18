package co.edu.usco.goltime.autenticacion.infraestructura;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

/**
 * Limita cuántas peticiones por clave (IP + operación) se aceptan en una ventana, con Bucket4j
 * (token bucket). Es en memoria: suficiente para una sola instancia de la API.
 */
class LimitadorTasa {

    private static final int LIMITE_ENTRADAS = 50_000;

    private final ConcurrentHashMap<String, Bucket> cubos = new ConcurrentHashMap<>();
    private final long capacidad;
    private final Duration ventana;

    LimitadorTasa(long capacidad, Duration ventana) {
        this.capacidad = capacidad;
        this.ventana = ventana;
    }

    /** Devuelve true si la petición cabe dentro del límite. */
    boolean permitir(String clave) {
        if (cubos.size() > LIMITE_ENTRADAS) {
            cubos.clear();
        }
        return cubos.computeIfAbsent(clave, k -> Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(capacidad).refillIntervally(capacidad, ventana).build())
                .build()).tryConsume(1);
    }
}
