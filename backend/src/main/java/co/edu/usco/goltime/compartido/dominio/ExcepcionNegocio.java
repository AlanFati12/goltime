package co.edu.usco.goltime.compartido.dominio;

/**
 * Error de regla de negocio. Lleva un código estable ({@link CodigoError}) que el frontend
 * traduce a un mensaje amigable, y el estado HTTP con el que debe responder la API.
 */
public class ExcepcionNegocio extends RuntimeException {

    private final CodigoError codigo;

    public ExcepcionNegocio(CodigoError codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public CodigoError codigo() {
        return codigo;
    }
}
