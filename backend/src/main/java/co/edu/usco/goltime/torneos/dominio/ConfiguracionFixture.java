package co.edu.usco.goltime.torneos.dominio;

import java.util.List;

/**
 * Entradas de un generador de fixture. {@code inscripciones} ya viene en el orden de siembra
 * (la primera es la cabeza de serie); lo decide {@link OrdenadorParticipantes}.
 */
public record ConfiguracionFixture(List<Long> inscripciones, boolean idaYVuelta, boolean tercerPuesto, int numGrupos,
        int clasificadosPorGrupo) {
}
