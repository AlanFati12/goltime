package co.edu.usco.goltime.torneos.dominio;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Tabla de posiciones de una liga o de un grupo. Los empates se resuelven con los criterios configurados, en orden:
 * cada criterio parte a los equipos igualados en subgrupos y solo los que siguen igualados pasan al criterio siguiente.
 * El enfrentamiento directo se calcula solo con los partidos jugados entre los equipos igualados.
 */
public final class CalculadoraPosiciones {

    private CalculadoraPosiciones() {
    }

    /** Partido con resultado definitivo (los W.O. entran con su marcador). */
    public record PartidoJugado(long localId, long visitanteId, int golesLocal, int golesVisitante) {
    }

    public record FilaPosicion(long inscripcionId, int posicion, int jugados, int ganados, int empatados, int perdidos, int golesFavor,
            int golesContra, int diferencia, int puntos, int puntosDisciplina, List<String> forma) {
    }

    /**
     * @param equipos       todas las inscripciones de la tabla, aunque aún no hayan jugado
     * @param partidos      partidos jugados, de más antiguo a más reciente (para la forma reciente)
     * @param disciplina    puntos disciplinarios por inscripción (amarilla 1, roja 3); ausente = 0
     */
    public static List<FilaPosicion> calcular(List<Long> equipos, List<PartidoJugado> partidos, Map<Long, Integer> disciplina, ReglasPuntuacion reglas) {
        Map<Long, Acumulado> acumulados = new HashMap<>();
        for (long id : equipos) {
            acumulados.put(id, new Acumulado(id));
        }
        for (PartidoJugado p : partidos) {
            Acumulado l = acumulados.get(p.localId());
            Acumulado v = acumulados.get(p.visitanteId());
            if (l == null || v == null) {
                continue;
            }
            l.registrar(p.golesLocal(), p.golesVisitante(), reglas);
            v.registrar(p.golesVisitante(), p.golesLocal(), reglas);
        }
        acumulados.values().forEach(a -> a.disciplina = disciplina.getOrDefault(a.id, 0));

        List<Acumulado> ordenados = ordenar(new ArrayList<>(acumulados.values()), reglas.criterios(), 0, partidos, reglas);
        List<FilaPosicion> filas = new ArrayList<>();
        for (int i = 0; i < ordenados.size(); i++) {
            Acumulado a = ordenados.get(i);
            filas.add(new FilaPosicion(a.id, i + 1, a.jugados, a.ganados, a.empatados, a.perdidos, a.golesFavor, a.golesContra,
                    a.golesFavor - a.golesContra, a.puntos, a.disciplina, ultimos(a.forma, 5)));
        }
        return filas;
    }

    private static List<Acumulado> ordenar(List<Acumulado> grupo, List<CriterioDesempate> criterios, int indice, List<PartidoJugado> partidos,
            ReglasPuntuacion reglas) {
        if (grupo.size() <= 1) {
            return grupo;
        }
        if (indice >= criterios.size()) {
            // empate total: orden estable y reproducible
            grupo.sort(Comparator.comparingLong((Acumulado a) -> sorteo(a.id)).thenComparingLong(a -> a.id));
            return grupo;
        }
        CriterioDesempate criterio = criterios.get(indice);
        Map<List<Long>, List<Acumulado>> subgrupos = new TreeMap<>(CalculadoraPosiciones::compararDescendente);
        Map<Long, List<Long>> claves = clavesDe(grupo, criterio, partidos, reglas);
        for (Acumulado a : grupo) {
            subgrupos.computeIfAbsent(claves.get(a.id), k -> new ArrayList<>()).add(a);
        }
        List<Acumulado> resultado = new ArrayList<>();
        for (List<Acumulado> sub : subgrupos.values()) {
            resultado.addAll(ordenar(sub, criterios, indice + 1, partidos, reglas));
        }
        return resultado;
    }

    /** Valor de cada equipo para el criterio; mayor es mejor. */
    private static Map<Long, List<Long>> clavesDe(List<Acumulado> grupo, CriterioDesempate criterio, List<PartidoJugado> partidos, ReglasPuntuacion reglas) {
        Map<Long, List<Long>> claves = new HashMap<>();
        switch (criterio) {
            case PUNTOS -> grupo.forEach(a -> claves.put(a.id, List.of((long) a.puntos)));
            case DIFERENCIA_GOL -> grupo.forEach(a -> claves.put(a.id, List.of((long) (a.golesFavor - a.golesContra))));
            case GOLES_A_FAVOR -> grupo.forEach(a -> claves.put(a.id, List.of((long) a.golesFavor)));
            case JUEGO_LIMPIO -> grupo.forEach(a -> claves.put(a.id, List.of((long) -a.disciplina)));
            case SORTEO -> grupo.forEach(a -> claves.put(a.id, List.of(-sorteo(a.id))));
            case ENFRENTAMIENTO_DIRECTO -> {
                Map<Long, Acumulado> mini = new HashMap<>();
                grupo.forEach(a -> mini.put(a.id, new Acumulado(a.id)));
                for (PartidoJugado p : partidos) {
                    Acumulado l = mini.get(p.localId());
                    Acumulado v = mini.get(p.visitanteId());
                    if (l != null && v != null) {
                        l.registrar(p.golesLocal(), p.golesVisitante(), reglas);
                        v.registrar(p.golesVisitante(), p.golesLocal(), reglas);
                    }
                }
                mini.values().forEach(m -> claves.put(m.id, List.of((long) m.puntos, (long) (m.golesFavor - m.golesContra), (long) m.golesFavor)));
            }
        }
        return claves;
    }

    private static int compararDescendente(List<Long> a, List<Long> b) {
        for (int i = 0; i < Math.min(a.size(), b.size()); i++) {
            int c = Long.compare(b.get(i), a.get(i));
            if (c != 0) {
                return c;
            }
        }
        return 0;
    }

    /** Número pseudoaleatorio fijo por equipo: el "sorteo" no cambia entre consultas. */
    private static long sorteo(long id) {
        long x = id * 0x9E3779B97F4A7C15L;
        x ^= (x >>> 31);
        return Math.abs(x % 1_000_003L);
    }

    private static List<String> ultimos(List<String> forma, int n) {
        return List.copyOf(forma.subList(Math.max(0, forma.size() - n), forma.size()));
    }

    private static final class Acumulado {
        final long id;
        int jugados;
        int ganados;
        int empatados;
        int perdidos;
        int golesFavor;
        int golesContra;
        int puntos;
        int disciplina;
        final List<String> forma = new ArrayList<>();

        Acumulado(long id) {
            this.id = id;
        }

        void registrar(int favor, int contra, ReglasPuntuacion reglas) {
            jugados++;
            golesFavor += favor;
            golesContra += contra;
            if (favor > contra) {
                ganados++;
                puntos += reglas.puntosVictoria();
                forma.add("G");
            } else if (favor == contra) {
                empatados++;
                puntos += reglas.puntosEmpate();
                forma.add("E");
            } else {
                perdidos++;
                puntos += reglas.puntosDerrota();
                forma.add("P");
            }
        }
    }
}
