package co.edu.usco.goltime.compartido.dominio;

import java.util.Optional;

/**
 * Tipos de imagen aceptados. Se identifican por los primeros bytes del archivo (firma), nunca por la
 * extensión ni el Content-Type que declara el cliente, que se pueden falsificar.
 */
public enum TipoImagen {
    JPEG("jpg", "image/jpeg"),
    PNG("png", "image/png"),
    WEBP("webp", "image/webp");

    private final String extension;
    private final String tipoMime;

    TipoImagen(String extension, String tipoMime) {
        this.extension = extension;
        this.tipoMime = tipoMime;
    }

    public String extension() {
        return extension;
    }

    public String tipoMime() {
        return tipoMime;
    }

    public static Optional<TipoImagen> detectar(byte[] b) {
        if (b == null || b.length < 12) {
            return Optional.empty();
        }
        if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return Optional.of(JPEG);
        }
        if ((b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return Optional.of(PNG);
        }
        if (b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return Optional.of(WEBP);
        }
        return Optional.empty();
    }

    public static Optional<TipoImagen> dePorExtension(String nombre) {
        for (TipoImagen t : values()) {
            if (nombre.endsWith("." + t.extension)) {
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }
}
