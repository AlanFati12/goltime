package co.edu.usco.goltime.torneos.dominio;

import java.util.ArrayList;
import java.util.List;

/**
 * Llaves de eliminación directa. Se completa hasta la siguiente potencia de dos con descansos (byes) para las mejores
 * semillas, y cada partido apunta al siguiente. Los lados de la primera ronda siguen el cuadro clásico (1 contra el
 * último, 2 contra el penúltimo...), de modo que las mejores semillas no se cruzan antes de tiempo.
 */
final class Eliminatoria {

    private Eliminatoria() {
    }

    static List<PartidoPlan> generar(List<Participante> sembrados, boolean tercerPuesto, int claveInicial) {
        int n = sembrados.size();
        int tamano = Integer.highestOneBit(n) == n ? n : Integer.highestOneBit(n) << 1;
        int rondas = Integer.numberOfTrailingZeros(tamano);
        int[] orden = posiciones(tamano);

        List<List<PartidoPlan>> porRonda = new ArrayList<>();
        int clave = claveInicial;
        for (int r = 1; r <= rondas; r++) {
            int cantidad = tamano >> r;
            List<PartidoPlan> lista = new ArrayList<>();
            for (int j = 0; j < cantidad; j++) {
                Participante local = null;
                Participante visitante = null;
                boolean bye = false;
                if (r == 1) {
                    int semillaLocal = orden[2 * j];
                    int semillaVisitante = orden[2 * j + 1];
                    local = semillaLocal <= n ? sembrados.get(semillaLocal - 1) : null;
                    visitante = semillaVisitante <= n ? sembrados.get(semillaVisitante - 1) : null;
                    if (local == null || visitante == null) {
                        bye = true;
                        local = local != null ? local : visitante;
                        visitante = null;
                    }
                }
                lista.add(new PartidoPlan(clave++, null, r, j + 1, nombreRonda(r, rondas, tamano), local, visitante, bye, null, null, null, null));
            }
            porRonda.add(lista);
        }
        for (int r = 0; r < rondas - 1; r++) {
            List<PartidoPlan> actual = porRonda.get(r);
            List<PartidoPlan> siguiente = porRonda.get(r + 1);
            for (int j = 0; j < actual.size(); j++) {
                actual.set(j, actual.get(j).conSiguiente(siguiente.get(j / 2).clave(), j % 2 == 0 ? Lado.LOCAL : Lado.VISITANTE));
            }
        }
        List<PartidoPlan> resultado = new ArrayList<>();
        porRonda.forEach(resultado::addAll);

        if (tercerPuesto && hayCuatroSemifinalistas(n, tamano)) {
            int claveTercero = clave;
            List<PartidoPlan> semifinales = porRonda.get(rondas - 2);
            for (int j = 0; j < 2; j++) {
                PartidoPlan s = semifinales.get(j);
                int idx = resultado.indexOf(s);
                resultado.set(idx, s.conPerdedor(claveTercero, j == 0 ? Lado.LOCAL : Lado.VISITANTE));
            }
            resultado.add(new PartidoPlan(claveTercero, null, rondas, 2, "Tercer puesto", null, null, false, null, null, null, null));
        }
        return resultado;
    }

    /** Con 3 equipos (o 5 a 7 en cuartos...) hay semifinales con descanso: no hay cuatro perdedores/ganadores para el tercer puesto. */
    private static boolean hayCuatroSemifinalistas(int n, int tamano) {
        return tamano > 4 || (tamano == 4 && n == 4);
    }

    /** Orden de semillas en la primera ronda: para 8, [1,8,4,5,2,7,3,6]. */
    static int[] posiciones(int tamano) {
        int[] orden = {1};
        while (orden.length < tamano) {
            int[] siguiente = new int[orden.length * 2];
            for (int i = 0; i < orden.length; i++) {
                siguiente[2 * i] = orden[i];
                siguiente[2 * i + 1] = orden.length * 2 + 1 - orden[i];
            }
            orden = siguiente;
        }
        return orden;
    }

    static String nombreRonda(int ronda, int totalRondas, int tamano) {
        int faltan = totalRondas - ronda;
        return switch (faltan) {
            case 0 -> "Final";
            case 1 -> "Semifinal";
            case 2 -> "Cuartos de final";
            case 3 -> "Octavos de final";
            default -> "Ronda de " + (tamano >> (ronda - 1));
        };
    }
}
