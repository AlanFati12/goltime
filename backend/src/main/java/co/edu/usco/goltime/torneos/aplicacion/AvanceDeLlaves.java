package co.edu.usco.goltime.torneos.aplicacion;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.torneos.aplicacion.CalculoTablas.TablaPosiciones;
import co.edu.usco.goltime.torneos.dominio.CalculadoraPosiciones.FilaPosicion;
import co.edu.usco.goltime.torneos.dominio.Lado;
import co.edu.usco.goltime.torneos.dominio.Partido;
import co.edu.usco.goltime.torneos.dominio.TipoFase;
import co.edu.usco.goltime.torneos.dominio.Torneo;

/**
 * Mueve a los equipos por el cuadro: el ganador pasa al partido siguiente, el perdedor de semifinal al tercer puesto,
 * y al terminar la fase de grupos los clasificados ocupan sus lugares en las llaves.
 */
@Component
public class AvanceDeLlaves {

    private final RepositorioPartidos partidos;
    private final RepositorioTorneos torneos;
    private final CalculoTablas tablas;

    public AvanceDeLlaves(RepositorioPartidos partidos, RepositorioTorneos torneos, CalculoTablas tablas) {
        this.partidos = partidos;
        this.torneos = torneos;
        this.tablas = tablas;
    }

    /** Coloca al ganador (y al perdedor) del partido en los partidos que le siguen. */
    public void propagar(Partido origen) {
        if (origen.siguienteId() != null && origen.ganadorId() != null) {
            colocar(origen.siguienteId(), origen.ladoSiguiente(), origen.ganadorId());
        }
        if (origen.siguientePerdedorId() != null && origen.perdedorId() != null) {
            colocar(origen.siguientePerdedorId(), origen.ladoPerdedor(), origen.perdedorId());
        }
    }

    /** Quita a los equipos que este partido había enviado a los siguientes (al corregirlo o reabrirlo). */
    public void retirar(Partido origen) {
        if (origen.siguienteId() != null) {
            colocar(origen.siguienteId(), origen.ladoSiguiente(), null);
        }
        if (origen.siguientePerdedorId() != null) {
            colocar(origen.siguientePerdedorId(), origen.ladoPerdedor(), null);
        }
    }

    /** Impide cambiar el resultado si el partido siguiente ya se jugó con el equipo que avanzó. */
    public void exigirSiguientesSinJugar(Partido origen) {
        for (Long id : new Long[] {origen.siguienteId(), origen.siguientePerdedorId()}) {
            if (id != null && partidos.buscar(id).map(Partido::jugado).orElse(false)) {
                throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO,
                        "El siguiente partido del cuadro ya se jugó. Corrige primero ese resultado.");
            }
        }
    }

    private void colocar(long partidoId, Lado lado, Long inscripcionId) {
        Partido destino = partidos.buscarBloqueando(partidoId).orElseThrow();
        destino.colocar(lado, inscripcionId);
        partidos.guardar(destino);
    }

    // ----- Fase de grupos -> eliminatoria -----

    /** ¿Se jugaron todos los partidos de la fase de grupos? */
    public static boolean gruposTerminados(List<Partido> todos) {
        List<Partido> grupos = todos.stream().filter(p -> p.tipoFase() == TipoFase.GRUPOS).toList();
        return !grupos.isEmpty() && grupos.stream().allMatch(Partido::jugado);
    }

    public static boolean eliminatoriaEmpezada(List<Partido> todos) {
        return todos.stream().anyMatch(p -> p.tipoFase() == TipoFase.ELIMINATORIA && p.jugado());
    }

    /**
     * Si la fase de grupos terminó, llena las llaves con los clasificados de cada grupo (y avanza los descansos).
     * Es idempotente: se puede volver a ejecutar tras corregir un resultado de grupos.
     */
    public void resolverClasificados(Torneo torneo) {
        List<Partido> todos = partidos.deTorneo(torneo.id());
        if (!gruposTerminados(todos)) {
            return;
        }
        if (eliminatoriaEmpezada(todos)) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "Ya hay partidos de eliminatoria jugados: no se puede cambiar la fase de grupos.");
        }
        Map<Long, Integer> disciplina = partidos.puntosDisciplina(torneo.id());
        List<TablaPosiciones> posiciones = tablas.calcular(torneo, partidos.fases(torneo.id()), partidos.grupos(torneo.id()), todos, disciplina, List.of());
        Map<String, Long> clasificados = new HashMap<>();
        for (TablaPosiciones t : posiciones) {
            for (FilaPosicion f : t.filas()) {
                clasificados.put("G:" + t.grupo() + ":" + f.posicion(), f.inscripcionId());
            }
        }
        // primero se limpian los avances anteriores y luego se vuelve a llenar, para no dejar equipos viejos
        limpiarClasificados(torneo.id());
        for (Partido p : partidos.deTorneo(torneo.id())) {
            if (p.tipoFase() != TipoFase.ELIMINATORIA) {
                continue;
            }
            boolean cambio = false;
            for (Lado lado : Lado.values()) {
                String ref = p.referenciaEn(lado);
                if (ref != null && clasificados.containsKey(ref)) {
                    p.colocar(lado, clasificados.get(ref));
                    cambio = true;
                }
            }
            if (cambio) {
                partidos.guardar(p);
            }
        }
        avanzarDescansos(torneo.id());
    }

    /** Vacía los lugares de las llaves que dependen de la fase de grupos. */
    public void limpiarClasificados(long torneoId) {
        for (Partido p : partidos.deTorneo(torneoId)) {
            if (p.tipoFase() != TipoFase.ELIMINATORIA) {
                continue;
            }
            boolean cambio = false;
            if (p.bye() && p.ganadorId() != null) {
                retirar(p);
                p.reabrirBye();
                cambio = true;
            }
            for (Lado lado : Lado.values()) {
                if (p.referenciaEn(lado) != null && p.equipoEn(lado) != null) {
                    p.colocar(lado, null);
                    cambio = true;
                }
            }
            if (cambio) {
                partidos.guardar(p);
            }
        }
    }

    /** Los partidos con descanso dan por ganador a su único equipo y lo pasan a la siguiente ronda. */
    public void avanzarDescansos(long torneoId) {
        for (Partido p : partidos.deTorneo(torneoId)) {
            if (p.bye() && p.localId() != null && p.ganadorId() == null) {
                p.marcarBye();
                partidos.guardar(p);
                propagar(p);
            }
        }
    }
}
