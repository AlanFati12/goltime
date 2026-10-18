package co.edu.usco.goltime.reservas.dominio;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.compartido.dominio.ZonaHoraria;

/**
 * Bloqueo de horario: impide reservar en una sede, en varias canchas o en una cancha.
 * Puede ser <b>puntual</b> (un intervalo continuo, o días completos) o <b>recurrente</b> (ciertos días de la
 * semana en un rango horario entre dos fechas). Se materializa como intervalos concretos que ocupan las canchas.
 */
public class Bloqueo {

    public static final int MAXIMO_INTERVALOS = 400;

    public enum Tipo {
        PUNTUAL, RECURRENTE
    }

    /** Intervalo concreto [inicio, fin) que ocupa el bloqueo. */
    public record Intervalo(Instant inicio, Instant fin) {
    }

    private Long id;
    private final long sedeId;
    private final Set<Long> canchaIds;
    private final Tipo tipo;
    private Instant inicio;
    private Instant fin;
    private final boolean todoElDia;
    private final Set<DayOfWeek> diasSemana;
    private final int minutoInicio;
    private final int minutoFin;
    private final LocalDate fechaDesde;
    private final LocalDate fechaHasta;
    private final MotivoBloqueo motivoTipo;
    private final String motivoDetalle;
    private boolean activo;

    private Bloqueo(Long id, long sedeId, Set<Long> canchaIds, Tipo tipo, Instant inicio, Instant fin, boolean todoElDia,
            Set<DayOfWeek> diasSemana, int minutoInicio, int minutoFin, LocalDate fechaDesde, LocalDate fechaHasta,
            MotivoBloqueo motivoTipo, String motivoDetalle, boolean activo) {
        this.id = id;
        this.sedeId = sedeId;
        this.canchaIds = canchaIds == null ? Set.of() : Set.copyOf(canchaIds);
        this.tipo = tipo;
        this.inicio = inicio;
        this.fin = fin;
        this.todoElDia = todoElDia;
        this.diasSemana = diasSemana == null ? Set.of() : Set.copyOf(diasSemana);
        this.minutoInicio = minutoInicio;
        this.minutoFin = minutoFin;
        this.fechaDesde = fechaDesde;
        this.fechaHasta = fechaHasta;
        this.motivoTipo = motivoTipo;
        this.motivoDetalle = motivoDetalle == null || motivoDetalle.isBlank() ? null : motivoDetalle.trim();
        this.activo = activo;
    }

    /** Puntual: [inicio, fin). Con {@code todoElDia} se extiende del inicio del día de {@code inicio} al final del día de {@code fin}. */
    public static Bloqueo puntual(Long id, long sedeId, Set<Long> canchaIds, Instant inicio, Instant fin, boolean todoElDia,
            MotivoBloqueo motivo, String detalle, boolean activo) {
        if (inicio == null || fin == null) {
            throw invalido("Indica cuándo empieza y cuándo termina el bloqueo.");
        }
        Instant desde = inicio;
        Instant hasta = fin;
        if (todoElDia) {
            desde = ZonaHoraria.inicioDelDia(ZonaHoraria.fechaLocal(inicio));
            hasta = ZonaHoraria.inicioDelDia(ZonaHoraria.fechaLocal(fin).plusDays(1));
        }
        if (!hasta.isAfter(desde)) {
            throw invalido("El bloqueo debe terminar después de empezar.");
        }
        validarMotivo(motivo);
        return new Bloqueo(id, sedeId, canchaIds, Tipo.PUNTUAL, desde, hasta, todoElDia, null, 0, 0, null, null, motivo, detalle, activo);
    }

    /** Recurrente: ciertos días en [minutoInicio, minutoFin) entre dos fechas (máximo un año). */
    public static Bloqueo recurrente(Long id, long sedeId, Set<Long> canchaIds, Set<DayOfWeek> dias, int minutoInicio, int minutoFin,
            LocalDate desde, LocalDate hasta, MotivoBloqueo motivo, String detalle, boolean activo) {
        if (dias == null || dias.isEmpty()) {
            throw invalido("Elige al menos un día de la semana.");
        }
        if (minutoInicio < 0 || minutoFin > 1440 || minutoFin <= minutoInicio) {
            throw invalido("La hora final debe ser posterior a la inicial.");
        }
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw invalido("Indica el rango de fechas del bloqueo recurrente.");
        }
        if (Duration.between(desde.atStartOfDay(), hasta.atStartOfDay()).toDays() > 366) {
            throw invalido("Un bloqueo recurrente puede durar como máximo un año.");
        }
        validarMotivo(motivo);
        return new Bloqueo(id, sedeId, canchaIds, Tipo.RECURRENTE, null, null, false, dias, minutoInicio, minutoFin, desde, hasta, motivo,
                detalle, activo);
    }

    /** Reconstruye un bloqueo guardado, con sus intervalos tal como se almacenaron (sin volver a extenderlos). */
    public static Bloqueo reconstruir(Long id, long sedeId, Set<Long> canchaIds, Tipo tipo, Instant inicio, Instant fin, boolean todoElDia,
            Set<DayOfWeek> dias, int minutoInicio, int minutoFin, LocalDate desde, LocalDate hasta, MotivoBloqueo motivo, String detalle,
            boolean activo) {
        return new Bloqueo(id, sedeId, canchaIds, tipo, inicio, fin, todoElDia, dias, minutoInicio, minutoFin, desde, hasta, motivo, detalle, activo);
    }

    /**
     * Intervalos concretos que ocupa, descartando los que ya terminaron (no tiene sentido bloquear el pasado).
     * Falla si son demasiados.
     */
    public List<Intervalo> expandir(Instant ahora) {
        List<Intervalo> intervalos = new ArrayList<>();
        if (tipo == Tipo.PUNTUAL) {
            if (fin.isAfter(ahora)) {
                intervalos.add(new Intervalo(inicio, fin));
            }
        } else {
            for (LocalDate dia = fechaDesde; !dia.isAfter(fechaHasta); dia = dia.plusDays(1)) {
                if (!diasSemana.contains(dia.getDayOfWeek())) {
                    continue;
                }
                Instant desde = ZonaHoraria.inicioDelDia(dia).plus(Duration.ofMinutes(minutoInicio));
                Instant hasta = ZonaHoraria.inicioDelDia(dia).plus(Duration.ofMinutes(minutoFin));
                if (hasta.isAfter(ahora)) {
                    intervalos.add(new Intervalo(desde, hasta));
                }
            }
        }
        if (intervalos.isEmpty()) {
            throw invalido("El bloqueo no tiene ningún horario futuro: todo el rango ya pasó.");
        }
        if (intervalos.size() > MAXIMO_INTERVALOS) {
            throw invalido("El bloqueo genera demasiados horarios (máximo " + MAXIMO_INTERVALOS + "). Acorta el rango.");
        }
        return intervalos;
    }

    private static void validarMotivo(MotivoBloqueo motivo) {
        if (motivo == null) {
            throw invalido("El motivo del bloqueo es obligatorio.");
        }
    }

    private static ExcepcionNegocio invalido(String mensaje) {
        return new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, mensaje);
    }

    public void asignarId(long id) { this.id = id; }

    public void desactivar() { this.activo = false; }

    /** Texto del motivo que se muestra a los clientes: "Mantenimiento: cambio de grama". */
    public String motivoTexto() {
        return motivoDetalle == null ? motivoTipo.etiqueta() : motivoTipo.etiqueta() + ": " + motivoDetalle;
    }

    public Long id() { return id; }
    public long sedeId() { return sedeId; }
    public Set<Long> canchaIds() { return canchaIds; }
    public Tipo tipo() { return tipo; }
    public Instant inicio() { return inicio; }
    public Instant fin() { return fin; }
    public boolean todoElDia() { return todoElDia; }
    public Set<DayOfWeek> diasSemana() { return diasSemana; }
    public int minutoInicio() { return minutoInicio; }
    public int minutoFin() { return minutoFin; }
    public LocalDate fechaDesde() { return fechaDesde; }
    public LocalDate fechaHasta() { return fechaHasta; }
    public MotivoBloqueo motivoTipo() { return motivoTipo; }
    public String motivoDetalle() { return motivoDetalle; }
    public boolean activo() { return activo; }
}
