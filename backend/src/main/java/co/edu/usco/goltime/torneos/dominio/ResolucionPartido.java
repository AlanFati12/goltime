package co.edu.usco.goltime.torneos.dominio;

import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;

/** Reglas para decidir el ganador de un partido a partir del marcador. */
public final class ResolucionPartido {

    private ResolucionPartido() {
    }

    /**
     * Ganador del partido, o {@code null} si es empate (solo válido en liga o grupos). En eliminatoria un empate exige
     * definición por penales, que no pueden terminar igualados.
     */
    public static Lado ganador(int golesLocal, int golesVisitante, Integer penalesLocal, Integer penalesVisitante, boolean eliminatoria) {
        if (golesLocal < 0 || golesVisitante < 0) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Los goles no pueden ser negativos.");
        }
        if (golesLocal != golesVisitante) {
            if (penalesLocal != null || penalesVisitante != null) {
                throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Los penales solo se registran cuando el partido termina empatado.");
            }
            return golesLocal > golesVisitante ? Lado.LOCAL : Lado.VISITANTE;
        }
        boolean hayPenales = penalesLocal != null || penalesVisitante != null;
        if (!eliminatoria) {
            if (hayPenales) {
                throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "En esta fase el empate se mantiene: no hay penales.");
            }
            return null;
        }
        if (penalesLocal == null || penalesVisitante == null) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "En eliminación directa un empate se define por penales.");
        }
        if (penalesLocal < 0 || penalesVisitante < 0 || penalesLocal.equals(penalesVisitante)) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Los penales deben tener un ganador.");
        }
        return penalesLocal > penalesVisitante ? Lado.LOCAL : Lado.VISITANTE;
    }
}
