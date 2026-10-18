package co.edu.usco.goltime.torneos.dominio;

/**
 * Quién ocupa un lado de un partido: una inscripción ya conocida, o una referencia por resolver
 * (por ejemplo "1° Grupo A", que se llena cuando termina la fase de grupos).
 */
public record Participante(Long inscripcionId, String etiqueta, String referencia) {

    public static Participante de(long inscripcionId) {
        return new Participante(inscripcionId, null, null);
    }

    public static Participante porDefinir(String etiqueta, String referencia) {
        return new Participante(null, etiqueta, referencia);
    }

    public boolean conocido() {
        return inscripcionId != null;
    }
}
