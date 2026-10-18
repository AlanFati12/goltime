package co.edu.usco.goltime.torneos.infraestructura;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.usco.goltime.torneos.aplicacion.ConsultasTorneos.FiltroPartidos;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.EstadisticasTorneo;
import co.edu.usco.goltime.torneos.aplicacion.ServicioConsultaTorneos;
import co.edu.usco.goltime.torneos.dominio.EstadoPartido;
import co.edu.usco.goltime.torneos.dominio.EstadoTorneo;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.RespuestaDetallePartido;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.RespuestaDetalleTorneo;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.RespuestaLado;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.RespuestaPartido;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.RespuestaPerfilEquipo;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.RespuestaPerfilJugador;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.RespuestaTabla;
import co.edu.usco.goltime.torneos.infraestructura.DtosTorneos.RespuestaTorneo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Consulta pública (sin sesión) de torneos: los borradores no existen para el público. */
@RestController
@RequestMapping("/api/v1/publico")
@Tag(name = "Público: torneos", description = "Torneos, tablas, llaves, calendario y estadísticas visibles sin iniciar sesión")
public class ControladorTorneosPublico {

    private final ServicioConsultaTorneos consultas;

    public ControladorTorneosPublico(ServicioConsultaTorneos consultas) {
        this.consultas = consultas;
    }

    @GetMapping("/torneos")
    @Operation(summary = "Torneos no borrador: en curso e inscripciones primero, luego finalizados")
    public List<RespuestaTorneo> torneos(@RequestParam(required = false) EstadoTorneo estado) {
        return consultas.listar(estado, true).stream().map(MapeadorTorneos::resumen).toList();
    }

    @GetMapping("/torneos/{id}")
    public RespuestaDetalleTorneo torneo(@PathVariable long id) {
        return MapeadorTorneos.detalle(consultas.detalle(id, true));
    }

    @GetMapping("/torneos/{id}/posiciones")
    @Operation(summary = "Tabla de posiciones (una por grupo o de la liga), calculada con los criterios del torneo")
    public List<RespuestaTabla> posiciones(@PathVariable long id) {
        return MapeadorTorneos.tablas(consultas.posiciones(id, true), consultas.equipos(id, true));
    }

    @GetMapping("/torneos/{id}/partidos")
    @Operation(summary = "Partidos del torneo (calendario y llaves)")
    public List<RespuestaPartido> partidos(@PathVariable long id, @RequestParam(required = false) Long faseId,
            @RequestParam(required = false) Long inscripcionId, @RequestParam(required = false) EstadoPartido estado) {
        return consultas.partidos(id, new FiltroPartidos(faseId, inscripcionId, estado), true).stream().map(MapeadorTorneos::partido).toList();
    }

    @GetMapping("/torneos/{id}/equipos")
    public List<RespuestaLado> equipos(@PathVariable long id) {
        return consultas.equipos(id, true).values().stream().map(MapeadorTorneos::lado).toList();
    }

    @GetMapping("/torneos/{id}/estadisticas")
    @Operation(summary = "Goleadores, asistidores y tarjetas")
    public EstadisticasTorneo estadisticas(@PathVariable long id) {
        return consultas.estadisticas(id, true);
    }

    @GetMapping("/partidos/proximos")
    @Operation(summary = "Próximos partidos programados de torneos públicos")
    public List<RespuestaPartido> proximos(@RequestParam(defaultValue = "6") int limite) {
        return consultas.proximosPartidos(limite).stream().map(MapeadorTorneos::partido).toList();
    }

    @GetMapping("/partidos/{id}")
    public RespuestaDetallePartido partido(@PathVariable long id) {
        return MapeadorTorneos.detallePartido(consultas.partido(id, true));
    }

    @GetMapping("/equipos/{id}")
    @Operation(summary = "Perfil público de un equipo: plantilla, torneos y partidos recientes (sin datos de contacto)")
    public RespuestaPerfilEquipo equipo(@PathVariable long id) {
        return MapeadorTorneos.perfilEquipo(consultas.equipo(id));
    }

    @GetMapping("/jugadores/{id}")
    @Operation(summary = "Perfil público de un jugador: estadísticas por torneo (sin documento)")
    public RespuestaPerfilJugador jugador(@PathVariable long id) {
        return MapeadorTorneos.perfilJugador(consultas.jugador(id));
    }
}
