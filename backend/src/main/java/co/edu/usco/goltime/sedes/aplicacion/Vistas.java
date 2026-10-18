package co.edu.usco.goltime.sedes.aplicacion;

import java.util.List;

import co.edu.usco.goltime.sedes.dominio.Cancha;
import co.edu.usco.goltime.sedes.dominio.Foto;
import co.edu.usco.goltime.sedes.dominio.HorarioDia;
import co.edu.usco.goltime.sedes.dominio.Sede;
import co.edu.usco.goltime.sedes.dominio.Tarifa;

/** Vistas de lectura que combinan una entidad con sus datos relacionados. */
public final class Vistas {

    private Vistas() {
    }

    public record VistaSede(Sede sede, List<Foto> fotos, List<VistaCancha> canchas) {
    }

    /**
     * Cancha con su sede, fotos, tarifas y horario efectivo por día (el propio si lo tiene, si no el
     * de la sede). {@code componentes} son las canchas de las que depende, si es compuesta.
     */
    public record VistaCancha(Cancha cancha, Sede sede, List<Foto> fotos, List<Tarifa> tarifas,
            List<HorarioDia> horarioEfectivo, List<Cancha> componentes) {
    }
}
