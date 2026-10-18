package co.edu.usco.goltime.compartido.dominio;

import java.util.List;
import java.util.function.Function;

/** Página de resultados, independiente de Spring Data. {@code pagina} empieza en 0. */
public record Pagina<T>(List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas) {

    public <R> Pagina<R> mapear(Function<T, R> funcion) {
        return new Pagina<>(contenido.stream().map(funcion).toList(), pagina, tamano, totalElementos,
                totalPaginas);
    }
}
