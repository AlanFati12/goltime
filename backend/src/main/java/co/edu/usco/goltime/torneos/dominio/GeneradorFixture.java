package co.edu.usco.goltime.torneos.dominio;

import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;

/** Estrategia de generación de partidos según el formato del torneo. */
public interface GeneradorFixture {

    PlanFixture generar(ConfiguracionFixture configuracion);

    static GeneradorFixture para(FormatoTorneo formato) {
        return switch (formato) {
            case LIGA -> new FixtureLiga();
            case ELIMINACION_DIRECTA -> new FixtureEliminacionDirecta();
            case GRUPOS_Y_ELIMINACION -> new FixtureGruposYEliminacion();
        };
    }

    static void exigirEquipos(int cantidad, int minimo) {
        if (cantidad < minimo) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Se necesitan al menos " + minimo + " equipos inscritos para generar el fixture.");
        }
    }
}
