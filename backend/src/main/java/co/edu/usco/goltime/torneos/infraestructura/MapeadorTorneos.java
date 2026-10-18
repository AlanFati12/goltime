package co.edu.usco.goltime.torneos.infraestructura;

import java.util.List;
import java.util.Map;

import co.edu.usco.goltime.torneos.aplicacion.CalculoTablas.TablaPosiciones;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.Equipo;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.EquipoPartido;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.InscripcionVista;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.JugadorConDorsal;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.PerfilEquipo;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.PerfilJugador;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.ResumenTorneo;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.VistaEvento;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.VistaPartido;
import co.edu.usco.goltime.torneos.aplicacion.ServicioConsultaTorneos.DetallePartido;
import co.edu.usco.goltime.torneos.aplicacion.ServicioConsultaTorneos.DetalleTorneo;
import co.edu.usco.goltime.torneos.dominio.Torneo;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.RespuestaDetallePartido;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.RespuestaDetalleTorneo;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.RespuestaEquipo;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.RespuestaEquipoPublico;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.RespuestaEstadisticaJugador;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.RespuestaEvento;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.RespuestaFase;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.RespuestaFilaPosicion;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.RespuestaInscripcion;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.RespuestaJugador;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.RespuestaJugadorPublico;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.RespuestaLado;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.RespuestaPartido;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.RespuestaPerfilEquipo;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.RespuestaPerfilJugador;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.RespuestaTabla;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.RespuestaTorneo;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.RespuestaTorneoDeEquipo;

final class MapeadorTorneos {

    private MapeadorTorneos() {
    }

    static RespuestaEquipo equipo(Equipo e) {
        return new RespuestaEquipo(e.id(), e.nombre(), e.color(), e.delegadoNombre(), e.delegadoTelefono(), e.usuarioResponsableId(), e.activo());
    }

    static RespuestaJugador jugador(JugadorConDorsal j) {
        return new RespuestaJugador(j.jugador().id(), j.jugador().nombres(), j.jugador().apellidos(), j.jugador().nombreCompleto(), j.jugador().documento(),
                j.jugador().posicion(), j.dorsal());
    }

    static RespuestaTorneo torneo(Torneo t, String sedeNombre, int equipos, String campeon, String subcampeon, String tercero) {
        return new RespuestaTorneo(t.id(), t.nombre(), t.descripcion(), t.sedeId(), sedeNombre, t.jugadoresPorEquipo(), t.formato(), t.estado(),
                t.fechaInicio(), t.fechaFin(), t.cupoEquipos(), t.valorInscripcion(), t.premiosTexto(), t.reglamento(), t.modoEmparejamiento(),
                t.idaYVuelta(), t.tercerPuesto(), t.numGrupos(), t.clasificadosPorGrupo(), t.reglas().puntosVictoria(), t.reglas().puntosEmpate(),
                t.reglas().puntosDerrota(), t.reglas().criterios(), t.reglas().marcadorWoGoles(), t.campeonId(), t.subcampeonId(), t.terceroId(), campeon,
                subcampeon, tercero, equipos);
    }

    static RespuestaTorneo resumen(ResumenTorneo r) {
        return torneo(r.torneo(), r.sedeNombre(), r.equiposInscritos(), r.campeonNombre(), null, null);
    }

    static RespuestaInscripcion inscripcion(InscripcionVista i) {
        return new RespuestaInscripcion(i.inscripcion().id(), i.inscripcion().equipoId(), i.equipoNombre(), i.equipoColor(), i.inscripcion().semilla(),
                i.jugadores());
    }

    static RespuestaDetalleTorneo detalle(DetalleTorneo d) {
        return new RespuestaDetalleTorneo(torneo(d.torneo(), d.sedeNombre(), d.inscripciones().size(), d.campeon(), d.subcampeon(), d.tercero()),
                d.canchaIds(), d.inscripciones().stream().map(MapeadorTorneos::inscripcion).toList(), d.solicitudes().stream().map(MapeadorTorneos::inscripcion).toList(),
                d.fases().stream().map(f -> new RespuestaFase(f.id(), f.tipo(), f.orden(), f.nombre())).toList(), d.partidosTotales(), d.partidosJugados());
    }

    static RespuestaLado lado(EquipoPartido e) {
        return e == null ? null : new RespuestaLado(e.inscripcionId(), e.equipoId(), e.nombre(), e.color(), e.etiqueta());
    }

    static RespuestaPartido partido(VistaPartido p) {
        return new RespuestaPartido(p.id(), p.torneoId(), p.torneoNombre(), p.faseId(), p.tipoFase(), p.grupo(), p.ronda(), p.numeroLlave(), p.nombreRonda(),
                lado(p.local()), lado(p.visitante()), p.canchaId(), p.canchaNombre(), p.inicio(), p.duracionMin(), p.arbitro(), p.estado(), p.golesLocal(),
                p.golesVisitante(), p.penalesLocal(), p.penalesVisitante(), p.ganadorInscripcionId(), p.bye(), p.wo(), p.figuraJugadorId(), p.figuraNombre(),
                p.siguientePartidoId(), p.ladoSiguiente());
    }

    static RespuestaEvento evento(VistaEvento e) {
        return new RespuestaEvento(e.id(), e.partidoId(), e.inscripcionId(), e.equipoNombre(), e.jugadorId(), e.jugadorNombre(), e.dorsal(), e.tipo(),
                e.minuto(), e.eventoRelacionadoId(), e.observacion());
    }

    static RespuestaDetallePartido detallePartido(DetallePartido d) {
        return new RespuestaDetallePartido(partido(d.partido()), d.eventos().stream().map(MapeadorTorneos::evento).toList());
    }

    static List<RespuestaTabla> tablas(List<TablaPosiciones> tablas, Map<Long, EquipoPartido> equipos) {
        return tablas.stream().map(t -> new RespuestaTabla(t.faseId(), t.faseNombre(), t.grupoId(), t.grupo(), t.filas().stream().map(f -> {
            EquipoPartido e = equipos.get(f.inscripcionId());
            return new RespuestaFilaPosicion(f.inscripcionId(), e == null ? null : e.equipoId(), e == null ? null : e.nombre(), e == null ? null : e.color(),
                    f.posicion(), f.jugados(), f.ganados(), f.empatados(), f.perdidos(), f.golesFavor(), f.golesContra(), f.diferencia(), f.puntos(),
                    f.puntosDisciplina(), f.forma());
        }).toList())).toList();
    }

    static RespuestaPerfilEquipo perfilEquipo(PerfilEquipo p) {
        return new RespuestaPerfilEquipo(new RespuestaEquipoPublico(p.equipo().id(), p.equipo().nombre(), p.equipo().color()),
                p.plantilla().stream().map(j -> new RespuestaJugadorPublico(j.jugador().id(), j.jugador().nombreCompleto(), j.jugador().posicion(), j.dorsal()))
                        .toList(),
                p.torneos().stream().map(t -> new RespuestaTorneoDeEquipo(t.torneoId(), t.torneoNombre(), t.estado())).toList(),
                p.partidos().stream().map(MapeadorTorneos::partido).toList());
    }

    static RespuestaPerfilJugador perfilJugador(PerfilJugador p) {
        return new RespuestaPerfilJugador(p.id(), p.nombreCompleto(), p.posicion(), p.torneos().stream()
                .map(t -> new RespuestaEstadisticaJugador(t.torneoId(), t.torneoNombre(), t.equipoNombre(), t.goles(), t.asistencias(), t.amarillas(), t.rojas()))
                .toList());
    }
}
