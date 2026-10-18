package co.edu.usco.goltime.compartido.aplicacion;

import java.util.Optional;

/** Puerto de almacenamiento de archivos (hoy un volumen local; luego puede ser S3 u otro). */
public interface AlmacenArchivos {

    /** Guarda el contenido con un nombre aleatorio y devuelve ese nombre. */
    String guardar(byte[] contenido, String extension);

    Optional<byte[]> leer(String nombre);

    void borrar(String nombre);
}
