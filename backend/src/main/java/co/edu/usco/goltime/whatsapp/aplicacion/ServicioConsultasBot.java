package co.edu.usco.goltime.whatsapp.aplicacion;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import co.edu.usco.goltime.compartido.dominio.FormatoTexto;
import co.edu.usco.goltime.compartido.dominio.ZonaHoraria;
import co.edu.usco.goltime.reservas.aplicacion.ServicioDisponibilidad;
import co.edu.usco.goltime.reservas.aplicacion.Vistas.EstadoSlot;
import co.edu.usco.goltime.reservas.aplicacion.Vistas.Slot;
import co.edu.usco.goltime.sedes.aplicacion.ServicioCanchas;
import co.edu.usco.goltime.sedes.aplicacion.ServicioCotizacion;
import co.edu.usco.goltime.sedes.aplicacion.Vistas.VistaCancha;
import co.edu.usco.goltime.torneos.aplicacion.ConsultasTorneos.FiltroPartidos;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.EquipoPartido;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.VistaPartido;
import co.edu.usco.goltime.torneos.aplicacion.ServicioConsultaTorneos;
import co.edu.usco.goltime.torneos.dominio.EstadoPartido;
import co.edu.usco.goltime.torneos.dominio.EstadoTorneo;
import co.edu.usco.goltime.torneos.dominio.TipoEvento;
import co.edu.usco.goltime.whatsapp.aplicacion.ServicioBotWhatsapp.Franja;
import co.edu.usco.goltime.whatsapp.aplicacion.VistasBot.Cancha;
import co.edu.usco.goltime.whatsapp.aplicacion.VistasBot.Cotizacion;
import co.edu.usco.goltime.whatsapp.aplicacion.VistasBot.Disponibilidad;
import co.edu.usco.goltime.whatsapp.aplicacion.VistasBot.DisponibilidadCancha;
import co.edu.usco.goltime.whatsapp.aplicacion.VistasBot.Expulsion;
import co.edu.usco.goltime.whatsapp.aplicacion.VistasBot.FilaTabla;
import co.edu.usco.goltime.whatsapp.aplicacion.VistasBot.FranjaLibre;
import co.edu.usco.goltime.whatsapp.aplicacion.VistasBot.Gol;
import co.edu.usco.goltime.whatsapp.aplicacion.VistasBot.Partido;
import co.edu.usco.goltime.whatsapp.aplicacion.VistasBot.ResumenPartido;
import co.edu.usco.goltime.whatsapp.aplicacion.VistasBot.Tabla;
import co.edu.usco.goltime.whatsapp.aplicacion.VistasBot.Torneo;

/**
 * Consultas de solo lectura del bot. Solo lo público: torneos que no son borrador, marcadores, tabla y el
 * resumen de un partido; nunca goleadores del torneo, tarjetas acumuladas ni datos de jugadores (ADR 0018).
 */
@Service
public class ServicioConsultasBot {

    static final int MAX_PARTIDOS = 10;
    private static final Locale COLOMBIA = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", COLOMBIA);
    private static final Set<EstadoTorneo> ACTIVOS = Set.of(EstadoTorneo.INSCRIPCIONES, EstadoTorneo.EN_CURSO);

    private final ServicioCanchas canchas;
    private final ServicioDisponibilidad disponibilidad;
    private final ServicioCotizacion cotizacion;
    private final ServicioConsultaTorneos torneos;

    public ServicioConsultasBot(ServicioCanchas canchas, ServicioDisponibilidad disponibilidad, ServicioCotizacion cotizacion,
            ServicioConsultaTorneos torneos) {
        this.canchas = canchas;
        this.disponibilidad = disponibilidad;
        this.cotizacion = cotizacion;
        this.torneos = torneos;
    }

    public static String dia(LocalDate fecha) {
        return fecha.format(DIA);
    }

    // ---------- Canchas, horarios y precios ----------

    public List<Cancha> canchas(Long sedeId, String modalidad) {
        return canchas.listarPublicas(sedeId, modalidad).stream().map(ServicioConsultasBot::cancha).toList();
    }

    /** Solo las franjas libres de cada cancha, agrupando los bloques seguidos (no cada bloque con su estado). */
    public Disponibilidad disponibilidad(LocalDate fecha, Long sedeId, String modalidad) {
        var dia = disponibilidad.delDia(sedeId, fecha, modalidad);
        List<DisponibilidadCancha> resultado = new ArrayList<>();
        for (var c : dia.canchas()) {
            List<FranjaLibre> franjas = franjasLibres(c.slots(), c.duracionBloqueMin());
            if (!c.cerrado() && !franjas.isEmpty()) {
                resultado.add(new DisponibilidadCancha(c.canchaId(), c.nombre(), c.modalidad(), c.sedeNombre(), c.duracionMinMin(),
                        c.duracionMaxMin(), franjas));
            }
        }
        String nota = null;
        if (resultado.isEmpty()) {
            nota = dia.canchas().stream().allMatch(c -> c.cerrado()) ? "Ese día las canchas no abren." : "Ese día ya no quedan horarios libres.";
        }
        return new Disponibilidad(fecha.toString(), dia(fecha), resultado, nota);
    }

    static List<FranjaLibre> franjasLibres(List<Slot> slots, int bloqueMin) {
        List<FranjaLibre> franjas = new ArrayList<>();
        Instant desde = null;
        Instant hasta = null;
        int minimo = Integer.MAX_VALUE;
        int maximo = 0;
        for (Slot s : slots) {
            boolean libre = s.estado() == EstadoSlot.DISPONIBLE;
            boolean seguido = hasta != null && hasta.equals(s.inicio());
            if (desde != null && (!libre || !seguido)) {
                franjas.add(new FranjaLibre(FormatoTexto.hora(desde), FormatoTexto.hora(hasta), minimo, maximo));
                desde = null;
            }
            if (libre) {
                if (desde == null) {
                    desde = s.inicio();
                    minimo = Integer.MAX_VALUE;
                    maximo = 0;
                }
                hasta = s.fin();
                int porHora = (int) Math.round(s.precio() * 60.0 / bloqueMin);
                minimo = Math.min(minimo, porHora);
                maximo = Math.max(maximo, porHora);
            }
        }
        if (desde != null) {
            franjas.add(new FranjaLibre(FormatoTexto.hora(desde), FormatoTexto.hora(hasta), minimo, maximo));
        }
        return franjas;
    }

    public Cotizacion cotizar(long canchaId, Franja franja) {
        VistaCancha cancha = canchas.detallePublico(canchaId);
        long total = cotizacion.cotizar(canchaId, franja.inicio(), franja.fin()).total();
        return new Cotizacion(canchaId, cancha.cancha().nombre(), cancha.sede().nombre(), franja.fecha().toString(),
                FormatoTexto.hora(franja.inicio()), FormatoTexto.hora(franja.fin()), total, FormatoTexto.cop(total));
    }

    private static Cancha cancha(VistaCancha v) {
        var c = v.cancha();
        return new Cancha(c.id(), c.nombre(), c.modalidad(), c.sedeId(), v.sede().nombre(), c.duracionMinMin(), c.duracionMaxMin(),
                c.precioBaseHora());
    }

    // ---------- Torneos ----------

    /** Torneos con inscripciones abiertas o en curso. */
    public List<Torneo> torneosActivos() {
        return torneos.listar(null, true).stream().filter(r -> ACTIVOS.contains(r.torneo().estado()))
                .map(r -> new Torneo(r.torneo().id(), r.torneo().nombre(), textoEstado(r.torneo().estado()), r.sedeNombre(),
                        "Fútbol " + r.torneo().jugadoresPorEquipo(), fecha(r.torneo().fechaInicio()), fecha(r.torneo().fechaFin()),
                        r.equiposInscritos(), r.torneo().cupoEquipos(), r.torneo().valorInscripcion()))
                .toList();
    }

    public List<Tabla> tabla(long torneoId) {
        Map<Long, EquipoPartido> equipos = torneos.equipos(torneoId, true);
        return torneos.posiciones(torneoId, true).stream()
                .map(t -> new Tabla(t.faseNombre(), t.grupo(), t.filas().stream()
                        .map(f -> new FilaTabla(f.posicion(), nombre(equipos.get(f.inscripcionId())), f.jugados(), f.ganados(), f.empatados(),
                                f.perdidos(), f.golesFavor(), f.golesContra(), f.diferencia(), f.puntos()))
                        .toList()))
                .toList();
    }

    /** {@code jugados}: los últimos resultados primero; si no, los próximos por fecha. */
    public List<Partido> partidos(long torneoId, boolean jugados) {
        Comparator<VistaPartido> porFecha = Comparator.comparing(VistaPartido::inicio, Comparator.nullsLast(Comparator.naturalOrder()));
        return torneos.partidos(torneoId, new FiltroPartidos(null, null, null), true).stream()
                .filter(p -> jugados ? p.estado().jugado() : p.estado() == EstadoPartido.PROGRAMADO || p.estado() == EstadoPartido.APLAZADO)
                .sorted(jugados ? porFecha.reversed() : porFecha)
                .limit(MAX_PARTIDOS)
                .map(ServicioConsultasBot::partido)
                .toList();
    }

    /** Próximos partidos de todos los torneos públicos. */
    public List<Partido> proximosPartidos() {
        return torneos.proximosPartidos(MAX_PARTIDOS).stream().map(ServicioConsultasBot::partido).toList();
    }

    public ResumenPartido resumen(long partidoId) {
        var detalle = torneos.partido(partidoId, true);
        VistaPartido p = detalle.partido();
        List<Gol> goles = detalle.eventos().stream().filter(e -> e.tipo().esGol())
                .sorted(Comparator.comparing(e -> e.minuto() == null ? Integer.MAX_VALUE : e.minuto()))
                .map(e -> new Gol(e.minuto(), e.jugadorNombre(), e.equipoNombre(),
                        switch (e.tipo()) {
                            case GOL_PENAL -> "penal";
                            case AUTOGOL -> "autogol";
                            default -> "gol";
                        }))
                .toList();
        List<Expulsion> expulsiones = detalle.eventos().stream().filter(e -> e.tipo() == TipoEvento.ROJA)
                .map(e -> new Expulsion(e.minuto(), e.jugadorNombre(), e.equipoNombre())).toList();
        Partido partido = partido(p);
        return new ResumenPartido(partido, goles, expulsiones, p.figuraNombre(), redactar(partido, p, goles));
    }

    private static String redactar(Partido partido, VistaPartido p, List<Gol> goles) {
        StringBuilder texto = new StringBuilder();
        if (p.estado().jugado()) {
            texto.append(partido.local()).append(' ').append(partido.marcador()).append(' ').append(partido.visitante());
            texto.append(p.wo() ? " (ganado por no presentación)." : ".");
            if (!goles.isEmpty()) {
                Map<String, List<String>> porEquipo = goles.stream().collect(Collectors.groupingBy(
                        g -> "autogol".equals(g.tipo()) ? "Autogoles" : g.equipo(), java.util.LinkedHashMap::new,
                        Collectors.mapping(g -> g.jugador() + (g.minuto() == null ? "" : " " + g.minuto() + "'")
                                + ("penal".equals(g.tipo()) ? " (penal)" : ""), Collectors.toList())));
                porEquipo.forEach((equipo, autores) -> texto.append(' ').append(equipo).append(": ").append(String.join(", ", autores)).append('.'));
            }
        } else {
            texto.append(partido.local()).append(" vs. ").append(partido.visitante()).append(": ").append(partido.estado().toLowerCase(COLOMBIA));
            if (partido.fecha() != null) {
                texto.append(", ").append(partido.fecha());
            }
            texto.append('.');
        }
        return texto.toString();
    }

    private static Partido partido(VistaPartido p) {
        String marcador = null;
        if (p.estado().jugado() && p.golesLocal() != null && p.golesVisitante() != null) {
            marcador = p.golesLocal() + "-" + p.golesVisitante()
                    + (p.penalesLocal() != null && p.penalesVisitante() != null ? " (penales " + p.penalesLocal() + "-" + p.penalesVisitante() + ")" : "");
        }
        String fase = p.grupo() != null ? "Grupo " + p.grupo() : p.nombreRonda();
        return new Partido(p.id(), p.torneoNombre(), fase, p.inicio() == null ? null : FormatoTexto.fechaHora(p.inicio()), nombre(p.local()),
                nombre(p.visitante()), marcador, textoEstado(p.estado()), p.canchaNombre());
    }

    private static String nombre(EquipoPartido e) {
        if (e == null) {
            return "Por definir";
        }
        return e.nombre() != null ? e.nombre() : e.etiqueta() != null ? e.etiqueta() : "Por definir";
    }

    private static String fecha(LocalDate fecha) {
        return fecha == null ? null : fecha.toString();
    }

    private static String textoEstado(EstadoTorneo estado) {
        return estado == EstadoTorneo.INSCRIPCIONES ? "Inscripciones abiertas" : "En curso";
    }

    private static String textoEstado(EstadoPartido estado) {
        return switch (estado) {
            case PENDIENTE -> "Por programar";
            case PROGRAMADO -> "Programado";
            case EN_JUEGO -> "En juego";
            case FINALIZADO -> "Finalizado";
            case APLAZADO -> "Aplazado";
            case WO -> "Ganado por W.O.";
            case BYE -> "Pasa sin jugar";
        };
    }

    /** Fecha de hoy en Bogotá (para el contexto del bot). */
    public static LocalDate hoy(Instant ahora) {
        return ZonaHoraria.fechaLocal(ahora);
    }
}
