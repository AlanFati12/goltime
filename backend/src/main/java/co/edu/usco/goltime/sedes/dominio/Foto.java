package co.edu.usco.goltime.sedes.dominio;

/** Foto de una sede o cancha: nombre del archivo guardado (aleatorio) y posición. */
public record Foto(Long id, String archivo, int orden) {
}
