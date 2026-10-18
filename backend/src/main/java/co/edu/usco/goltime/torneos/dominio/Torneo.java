package co.edu.usco.goltime.torneos.dominio;

import java.time.LocalDate;

import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;

/** Torneo con su configuración. Las reglas de forma se validan al construirlo; las de flujo, en los servicios. */
public record Torneo(Long id, String nombre, String descripcion, long sedeId, int jugadoresPorEquipo, FormatoTorneo formato,
        EstadoTorneo estado, LocalDate fechaInicio, LocalDate fechaFin, Integer cupoEquipos, int valorInscripcion, String premiosTexto,
        String reglamento, ModoEmparejamiento modoEmparejamiento, boolean idaYVuelta, boolean tercerPuesto, Integer numGrupos,
        Integer clasificadosPorGrupo, ReglasPuntuacion reglas, Long campeonId, Long subcampeonId, Long terceroId) {

    public Torneo {
        nombre = limpiar(nombre);
        if (nombre == null || nombre.length() > 150) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "El nombre del torneo es obligatorio (máximo 150 caracteres).");
        }
        if (jugadoresPorEquipo < 2 || jugadoresPorEquipo > 11) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Los jugadores por equipo deben estar entre 2 y 11.");
        }
        if (fechaInicio != null && fechaFin != null && fechaFin.isBefore(fechaInicio)) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "La fecha de fin no puede ser anterior a la de inicio.");
        }
        if (cupoEquipos != null && cupoEquipos < 2) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "El cupo mínimo es de 2 equipos.");
        }
        if (valorInscripcion < 0) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "El valor de la inscripción no puede ser negativo.");
        }
        if (formato == FormatoTorneo.GRUPOS_Y_ELIMINACION) {
            if (numGrupos == null || numGrupos < 2 || clasificadosPorGrupo == null || clasificadosPorGrupo < 1) {
                throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Indica al menos 2 grupos y cuántos equipos clasifican por grupo.");
            }
            if (cupoEquipos != null && cupoEquipos < numGrupos * 2) {
                throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "El cupo debe permitir al menos 2 equipos por grupo.");
            }
        } else {
            numGrupos = null;
            clasificadosPorGrupo = null;
        }
        if (formato == FormatoTorneo.ELIMINACION_DIRECTA) {
            idaYVuelta = false; // las llaves de ida y vuelta no se soportan: un solo partido por cruce
        }
        if (formato == FormatoTorneo.LIGA) {
            tercerPuesto = false;
        }
        descripcion = limpiar(descripcion);
        premiosTexto = limpiar(premiosTexto);
        reglamento = limpiar(reglamento);
    }

    public Torneo conEstado(EstadoTorneo nuevo) {
        return new Torneo(id, nombre, descripcion, sedeId, jugadoresPorEquipo, formato, nuevo, fechaInicio, fechaFin, cupoEquipos, valorInscripcion,
                premiosTexto, reglamento, modoEmparejamiento, idaYVuelta, tercerPuesto, numGrupos, clasificadosPorGrupo, reglas, campeonId,
                subcampeonId, terceroId);
    }

    public Torneo conPodio(Long campeon, Long subcampeon, Long tercero) {
        return new Torneo(id, nombre, descripcion, sedeId, jugadoresPorEquipo, formato, estado, fechaInicio, fechaFin, cupoEquipos, valorInscripcion,
                premiosTexto, reglamento, modoEmparejamiento, idaYVuelta, tercerPuesto, numGrupos, clasificadosPorGrupo, reglas, campeon, subcampeon,
                tercero);
    }

    public void exigirTransicion(EstadoTorneo destino) {
        if (!estado.siguientes().contains(destino)) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "No se puede pasar el torneo de " + estado + " a " + destino + ".");
        }
    }

    static String limpiar(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim();
    }
}
