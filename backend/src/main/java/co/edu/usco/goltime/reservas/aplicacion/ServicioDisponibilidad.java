package co.edu.usco.goltime.reservas.aplicacion;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.usco.goltime.compartido.dominio.ZonaHoraria;
import co.edu.usco.goltime.reservas.aplicacion.Vistas.DisponibilidadCancha;
import co.edu.usco.goltime.reservas.aplicacion.Vistas.DisponibilidadDia;
import co.edu.usco.goltime.reservas.aplicacion.Vistas.EstadoSlot;
import co.edu.usco.goltime.reservas.aplicacion.Vistas.ResumenDia;
import co.edu.usco.goltime.reservas.aplicacion.Vistas.Slot;
import co.edu.usco.goltime.reservas.dominio.PoliticaReservas;
import co.edu.usco.goltime.sedes.aplicacion.ServicioCanchas;
import co.edu.usco.goltime.sedes.aplicacion.ServicioFestivos;
import co.edu.usco.goltime.sedes.aplicacion.Vistas.VistaCancha;
import co.edu.usco.goltime.sedes.dominio.CalculadoraPrecio;
import co.edu.usco.goltime.sedes.dominio.HorarioDia;

/**
 * Disponibilidad pública: para cada cancha reservable y cada bloque de su horario de operación, indica
 * si está libre, ocupado por otra reserva o bloqueado, con el precio del bloque ya calculado por el servidor.
 * Una cancha compuesta (ej. fútbol 8) no está libre si alguna de sus componentes está ocupada.
 */
@Service
public class ServicioDisponibilidad {

    private final ServicioCanchas canchas;
    private final ServicioFestivos festivos;
    private final RepositorioOcupaciones ocupaciones;
    private final ConfiguracionReservas configuracion;
    private final Clock reloj;

    public ServicioDisponibilidad(ServicioCanchas canchas, ServicioFestivos festivos, RepositorioOcupaciones ocupaciones,
            ConfiguracionReservas configuracion, Clock reloj) {
        this.canchas = canchas;
        this.festivos = festivos;
        this.ocupaciones = ocupaciones;
        this.configuracion = configuracion;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public DisponibilidadDia delDia(Long sedeId, LocalDate fecha, String modalidad) {
        return calcular(canchas.listarPublicas(sedeId, modalidad), fecha, configuracion.politica(), reloj.instant());
    }

    /** Cuántos horarios libres hay en cada uno de los próximos 7 días, para elegir la fecha rápidamente. */
    @Transactional(readOnly = true)
    public List<ResumenDia> proximosSieteDias(Long sedeId, String modalidad) {
        List<VistaCancha> vistas = canchas.listarPublicas(sedeId, modalidad);
        PoliticaReservas politica = configuracion.politica();
        Instant ahora = reloj.instant();
        LocalDate hoy = ZonaHoraria.fechaLocal(ahora);
        List<ResumenDia> resumen = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate fecha = hoy.plusDays(i);
            DisponibilidadDia dia = calcular(vistas, fecha, politica, ahora);
            int libres = (int) dia.canchas().stream().flatMap(c -> c.slots().stream()).filter(s -> s.estado() == EstadoSlot.DISPONIBLE).count();
            resumen.add(new ResumenDia(fecha, libres));
        }
        return resumen;
    }

    private DisponibilidadDia calcular(List<VistaCancha> vistas, LocalDate fecha, PoliticaReservas politica, Instant ahora) {
        Instant inicioDelDia = ZonaHoraria.inicioDelDia(fecha);
        Instant finDelDia = ZonaHoraria.inicioDelDia(fecha.plusDays(1));

        Set<Long> idsConsulta = new HashSet<>();
        vistas.forEach(v -> {
            idsConsulta.add(v.cancha().id());
            idsConsulta.addAll(v.cancha().componentes());
        });
        List<Ocupacion> ocupadas = idsConsulta.isEmpty() ? List.of() : ocupaciones.entre(idsConsulta, inicioDelDia, finDelDia);
        Set<LocalDate> fechasFestivas = festivos.fechasEntre(fecha, fecha);

        List<DisponibilidadCancha> resultado = new ArrayList<>();
        for (VistaCancha v : vistas) {
            HorarioDia horario = v.horarioEfectivo().get(fecha.getDayOfWeek().getValue() - 1);
            int bloque = v.cancha().duracionBloqueMin();
            List<Slot> slots = new ArrayList<>();
            if (!horario.cerrado()) {
                for (int minuto = horario.minutoApertura(); minuto + bloque <= horario.minutoCierre(); minuto += bloque) {
                    Instant inicio = inicioDelDia.plus(Duration.ofMinutes(minuto));
                    Instant fin = inicio.plus(Duration.ofMinutes(bloque));
                    long precio = CalculadoraPrecio.calcular(v.cancha().precioBaseHora(), v.tarifas(), fechasFestivas, inicio, fin).total();
                    slots.add(new Slot(inicio, fin, estadoDelSlot(v, inicio, fin, ocupadas, politica, ahora), precio));
                }
            }
            resultado.add(new DisponibilidadCancha(v.cancha().id(), v.cancha().nombre(), v.cancha().modalidad(),
                    v.cancha().sedeId(), v.sede().nombre(), bloque, v.cancha().duracionMinMin(), v.cancha().duracionMaxMin(),
                    horario.cerrado(), slots));
        }
        return new DisponibilidadDia(fecha, resultado);
    }

    private static EstadoSlot estadoDelSlot(VistaCancha v, Instant inicio, Instant fin, List<Ocupacion> ocupadas,
            PoliticaReservas politica, Instant ahora) {
        // Una cancha compuesta también está ocupada si lo está cualquiera de sus componentes.
        Set<Long> afectadas = new HashSet<>(v.cancha().componentes());
        afectadas.add(v.cancha().id());
        boolean bloqueado = false;
        boolean ocupado = false;
        for (Ocupacion o : ocupadas) {
            if (afectadas.contains(o.canchaId()) && o.seSolapaCon(inicio, fin)) {
                if ("BLOQUEO".equals(o.tipo())) {
                    bloqueado = true;
                } else {
                    ocupado = true;
                }
            }
        }
        if (bloqueado) {
            return EstadoSlot.BLOQUEADO;
        }
        if (ocupado) {
            return EstadoSlot.OCUPADO;
        }
        boolean fueraDeVentana = inicio.isBefore(ahora.plus(Duration.ofHours(politica.antelacionMinimaHoras())))
                || inicio.isAfter(ahora.plus(Duration.ofDays(politica.antelacionMaximaDias())));
        return fueraDeVentana ? EstadoSlot.NO_RESERVABLE : EstadoSlot.DISPONIBLE;
    }
}
