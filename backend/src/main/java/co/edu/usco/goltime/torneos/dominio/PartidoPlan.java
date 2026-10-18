package co.edu.usco.goltime.torneos.dominio;

/**
 * Partido generado por un fixture, aún sin guardar. {@code clave} identifica al partido dentro del plan para enlazar
 * al ganador (y al perdedor, para el tercer puesto) con el partido siguiente. Un partido {@code bye} tiene solo
 * el lado local: ese equipo avanza sin jugar.
 */
public record PartidoPlan(int clave, String grupo, int ronda, int numeroLlave, String nombreRonda, Participante local,
        Participante visitante, boolean bye, Integer siguienteClave, Lado ladoEnSiguiente, Integer siguientePerdedorClave,
        Lado ladoPerdedor) {

    PartidoPlan conSiguiente(Integer clave, Lado lado) {
        return new PartidoPlan(this.clave, grupo, ronda, numeroLlave, nombreRonda, local, visitante, bye, clave, lado,
                siguientePerdedorClave, ladoPerdedor);
    }

    PartidoPlan conPerdedor(Integer clave, Lado lado) {
        return new PartidoPlan(this.clave, grupo, ronda, numeroLlave, nombreRonda, local, visitante, bye, siguienteClave,
                ladoEnSiguiente, clave, lado);
    }
}
