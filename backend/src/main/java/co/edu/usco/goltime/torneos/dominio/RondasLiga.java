package co.edu.usco.goltime.torneos.dominio;

import java.util.ArrayList;
import java.util.List;

/**
 * Todos contra todos por el método del círculo (Berger). Con un número impar de equipos uno descansa cada jornada.
 */
final class RondasLiga {

    private RondasLiga() {
    }

    /** Genera los partidos de un grupo o liga; {@code grupo} puede ser nulo. Las claves empiezan en {@code claveInicial}. */
    static List<PartidoPlan> generar(List<Long> equipos, boolean idaYVuelta, String grupo, int claveInicial) {
        List<Long> t = new ArrayList<>(equipos);
        if (t.size() % 2 == 1) {
            t.add(null); // descanso
        }
        int n = t.size();
        int jornadas = n - 1;
        List<PartidoPlan> partidos = new ArrayList<>();
        int clave = claveInicial;
        for (int r = 0; r < jornadas; r++) {
            int llave = 1;
            for (int i = 0; i < n / 2; i++) {
                Long a = t.get(i);
                Long b = t.get(n - 1 - i);
                if (a == null || b == null) {
                    continue;
                }
                // alterna la localía para que nadie juegue siempre de local
                boolean invertir = (r + i) % 2 == 1;
                Long local = invertir ? b : a;
                Long visitante = invertir ? a : b;
                partidos.add(partido(clave++, grupo, r + 1, llave++, local, visitante));
            }
            t.add(1, t.remove(n - 1)); // rota todos menos el primero
        }
        if (idaYVuelta) {
            List<PartidoPlan> ida = List.copyOf(partidos);
            for (PartidoPlan p : ida) {
                partidos.add(partido(clave++, grupo, p.ronda() + jornadas, p.numeroLlave(), p.visitante().inscripcionId(), p.local().inscripcionId()));
            }
        }
        return partidos;
    }

    private static PartidoPlan partido(int clave, String grupo, int ronda, int llave, long local, long visitante) {
        return new PartidoPlan(clave, grupo, ronda, llave, "Fecha " + ronda, Participante.de(local), Participante.de(visitante), false, null, null,
                null, null);
    }
}
