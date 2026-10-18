package co.edu.usco.goltime.compartido.dominio;

/** Paginación solicitada. {@code pagina} empieza en 0; el tamaño se acota a 1..100. */
public record SolicitudPagina(int pagina, int tamano) {

    public static final int TAMANO_MAXIMO = 100;

    public SolicitudPagina {
        pagina = Math.max(pagina, 0);
        tamano = Math.min(Math.max(tamano, 1), TAMANO_MAXIMO);
    }
}
