package co.edu.usco.goltime.compartido.infraestructura.persistencia;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import co.edu.usco.goltime.compartido.aplicacion.AlmacenArchivos;

/**
 * Guarda los archivos en un directorio local (un volumen de Docker en producción) con nombres
 * aleatorios. Los nombres se validan al leer o borrar para impedir salir del directorio.
 */
@Component
class AlmacenArchivosLocal implements AlmacenArchivos {

    private static final Pattern NOMBRE_VALIDO = Pattern.compile("[0-9a-f\\-]{36}\\.(jpg|png|webp)");

    private final Path directorio;

    AlmacenArchivosLocal(@Value("${goltime.almacenamiento.directorio}") String directorio) {
        this.directorio = Path.of(directorio).toAbsolutePath().normalize();
    }

    @Override
    public String guardar(byte[] contenido, String extension) {
        String nombre = UUID.randomUUID() + "." + extension;
        try {
            Files.createDirectories(directorio);
            Files.write(directorio.resolve(nombre), contenido);
            return nombre;
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo guardar el archivo", ex);
        }
    }

    @Override
    public Optional<byte[]> leer(String nombre) {
        if (!NOMBRE_VALIDO.matcher(nombre).matches()) {
            return Optional.empty();
        }
        Path ruta = directorio.resolve(nombre).normalize();
        try {
            return ruta.startsWith(directorio) && Files.isRegularFile(ruta) ? Optional.of(Files.readAllBytes(ruta)) : Optional.empty();
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo leer el archivo", ex);
        }
    }

    @Override
    public void borrar(String nombre) {
        if (!NOMBRE_VALIDO.matcher(nombre).matches()) {
            return;
        }
        try {
            Files.deleteIfExists(directorio.resolve(nombre).normalize());
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo borrar el archivo", ex);
        }
    }
}
