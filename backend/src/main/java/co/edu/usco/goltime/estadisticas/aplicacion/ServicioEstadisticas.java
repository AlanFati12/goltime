package co.edu.usco.goltime.estadisticas.aplicacion;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.usco.goltime.estadisticas.aplicacion.Estadisticas.CeldaPico;
import co.edu.usco.goltime.estadisticas.aplicacion.Estadisticas.ClienteFrecuente;
import co.edu.usco.goltime.estadisticas.aplicacion.Estadisticas.Consultas;
import co.edu.usco.goltime.estadisticas.aplicacion.Estadisticas.FilaCancha;
import co.edu.usco.goltime.estadisticas.aplicacion.Estadisticas.Filtro;
import co.edu.usco.goltime.estadisticas.aplicacion.Estadisticas.HorasPico;
import co.edu.usco.goltime.estadisticas.aplicacion.Estadisticas.PuntoSerie;
import co.edu.usco.goltime.estadisticas.aplicacion.Estadisticas.Resumen;
import co.edu.usco.goltime.estadisticas.aplicacion.Estadisticas.ResumenConComparacion;
import co.edu.usco.goltime.estadisticas.aplicacion.Estadisticas.Serie;
import co.edu.usco.goltime.estadisticas.aplicacion.Estadisticas.TotalesReservas;
import co.edu.usco.goltime.estadisticas.dominio.Agrupacion;
import co.edu.usco.goltime.estadisticas.dominio.PeriodoEstadistico;

/**
 * Estadísticas del negocio, siempre calculadas en el backend. Los ingresos reales cuentan solo reservas COMPLETADA y no incluyen los
 * partidos de torneo (que ocupan la cancha pero no cobran); la ocupación sí los incluye, porque la cancha estuvo en uso.
 */
@Service
public class ServicioEstadisticas {

    private final Consultas consultas;

    public ServicioEstadisticas(Consultas consultas) {
        this.consultas = consultas;
    }

    @Transactional(readOnly = true)
    public ResumenConComparacion resumen(Filtro filtro) {
        PeriodoEstadistico anterior = filtro.periodo().anterior();
        Resumen actual = resumenDe(filtro);
        Resumen previo = resumenDe(filtro.conPeriodo(anterior));
        Map<String, Double> variaciones = new LinkedHashMap<>();
        poner(variaciones, "ingresosReales", actual.ingresosReales(), previo.ingresosReales());
        poner(variaciones, "recaudado", actual.recaudado(), previo.recaudado());
        poner(variaciones, "reservas", actual.reservas(), previo.reservas());
        poner(variaciones, "completadas", actual.completadas(), previo.completadas());
        poner(variaciones, "ocupacionPct", actual.ocupacionPct(), previo.ocupacionPct());
        poner(variaciones, "ticketPromedio", actual.ticketPromedio(), previo.ticketPromedio());
        return new ResumenConComparacion(filtro.periodo(), actual, anterior, previo, variaciones);
    }

    private static void poner(Map<String, Double> destino, String clave, double actual, double previo) {
        Double v = PeriodoEstadistico.variacion(actual, previo);
        if (v != null) {
            destino.put(clave, v);
        }
    }

    Resumen resumenDe(Filtro filtro) {
        TotalesReservas t = consultas.totales(filtro);
        Map<Long, Double> ocupados = consultas.minutosOcupados(filtro);
        Map<Long, Double> disponibles = consultas.minutosDisponibles(filtro);
        double tasaCancelacion = t.total() == 0 ? 0 : redondear(100.0 * t.canceladas() / t.total());
        int jugadas = t.completadas() + t.noAsistio();
        double tasaNoAsistencia = jugadas == 0 ? 0 : redondear(100.0 * t.noAsistio() / jugadas);
        long ticket = t.completadas() == 0 ? 0 : Math.round((double) t.ingresosReales() / t.completadas());
        return new Resumen(t.ingresosReales(), consultas.recaudado(filtro), t.porCobrar(), t.total(), consultas.reservasPorEstado(filtro), t.completadas(),
                t.canceladas(), t.noAsistio(), tasaCancelacion, tasaNoAsistencia, redondear(t.horasJugadas()), ticket, t.clientesUnicos(),
                porcentaje(suma(ocupados), suma(disponibles)), consultas.partidosDeTorneo(filtro));
    }

    /** Serie con todas las cubetas del periodo, también las que no tuvieron movimiento. */
    @Transactional(readOnly = true)
    public Serie serie(Filtro filtro, Agrupacion agrupacion) {
        Map<LocalDate, PuntoSerie> reales = new HashMap<>();
        for (PuntoSerie p : consultas.serie(filtro, agrupacion)) {
            reales.put(p.fecha(), p);
        }
        List<PuntoSerie> puntos = new ArrayList<>();
        for (LocalDate cubeta : agrupacion.cubetas(filtro.periodo())) {
            puntos.add(reales.getOrDefault(cubeta, new PuntoSerie(cubeta, 0, 0, 0)));
        }
        return new Serie(agrupacion, puntos);
    }

    @Transactional(readOnly = true)
    public List<FilaCancha> porCancha(Filtro filtro) {
        Map<Long, Double> ocupados = consultas.minutosOcupados(filtro);
        Map<Long, Double> disponibles = consultas.minutosDisponibles(filtro);
        return consultas.porCancha(filtro).stream()
                .map(f -> new FilaCancha(f.canchaId(), f.cancha(), f.sede(), f.completadas(), redondear(f.horas()), f.ingresos(),
                        porcentaje(ocupados.getOrDefault(f.canchaId(), 0.0), disponibles.getOrDefault(f.canchaId(), 0.0))))
                .toList();
    }

    @Transactional(readOnly = true)
    public HorasPico horasPico(Filtro filtro) {
        List<CeldaPico> celdas = consultas.horasPico(filtro);
        return new HorasPico(celdas, celdas.stream().mapToInt(CeldaPico::reservas).max().orElse(0));
    }

    @Transactional(readOnly = true)
    public List<ClienteFrecuente> clientesFrecuentes(Filtro filtro) {
        return consultas.clientesFrecuentes(filtro, 10);
    }

    static double porcentaje(double parte, double total) {
        return total <= 0 ? 0 : redondear(Math.min(100.0, 100.0 * parte / total));
    }

    private static double redondear(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }

    private static double suma(Map<Long, Double> minutos) {
        return minutos.values().stream().mapToDouble(Double::doubleValue).sum();
    }
}
