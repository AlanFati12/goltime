package co.edu.usco.goltime.sedes.dominio;

import java.math.BigDecimal;
import java.util.Set;

import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;

/**
 * Cancha de una sede. Puede depender de otras (cancha compuesta): si alguna de sus componentes no
 * está disponible, ella tampoco, y al revés. Las reglas de duración garantizan que las reservas
 * siempre se alineen al bloque de la cancha.
 */
public class Cancha {

    private Long id;
    private final long sedeId;
    private String nombre;
    private String modalidad;
    private int jugadoresPorEquipo;
    private int capacidadTotal;
    private String superficie;
    private BigDecimal largoM;
    private BigDecimal anchoM;
    private boolean techada;
    private boolean iluminacion;
    private int precioBaseHora;
    private int duracionBloqueMin;
    private int duracionMinMin;
    private int duracionMaxMin;
    private EstadoCancha estado;
    private String descripcion;
    private int orden;
    private HorarioSemanal horarioPropio;
    private Set<Long> componentes;

    private Cancha(long sedeId) {
        this.sedeId = sedeId;
    }

    public static Cancha nueva(long sedeId, DatosCancha datos, Set<Long> componentes) {
        Cancha cancha = new Cancha(sedeId);
        cancha.estado = EstadoCancha.ACTIVA;
        cancha.actualizar(datos);
        cancha.definirComponentes(componentes);
        return cancha;
    }

    public static Cancha reconstruir(Long id, long sedeId, DatosCancha datos, EstadoCancha estado,
            HorarioSemanal horarioPropio, Set<Long> componentes) {
        Cancha cancha = nueva(sedeId, datos, componentes);
        cancha.id = id;
        cancha.estado = estado;
        cancha.horarioPropio = horarioPropio;
        return cancha;
    }

    public void actualizar(DatosCancha d) {
        this.nombre = Sede.obligatorio(d.nombre(), "El nombre de la cancha es obligatorio.");
        this.modalidad = Sede.obligatorio(d.modalidad(), "La modalidad es obligatoria.");
        if (d.jugadoresPorEquipo() < 2 || d.jugadoresPorEquipo() > 11) {
            throw invalida("Los jugadores por equipo deben estar entre 2 y 11.");
        }
        if (d.capacidadTotal() < d.jugadoresPorEquipo() * 2) {
            throw invalida("La capacidad total no puede ser menor a dos equipos completos.");
        }
        if (d.precioBaseHora() < 0) {
            throw invalida("El precio base no puede ser negativo.");
        }
        if (d.duracionBloqueMin() < 15 || d.duracionBloqueMin() > 240) {
            throw invalida("La duración del bloque debe estar entre 15 y 240 minutos.");
        }
        if (d.duracionMinMin() % d.duracionBloqueMin() != 0 || d.duracionMaxMin() % d.duracionBloqueMin() != 0) {
            throw invalida("Las duraciones mínima y máxima deben ser múltiplos del bloque (" + d.duracionBloqueMin() + " min).");
        }
        if (d.duracionMinMin() < d.duracionBloqueMin() || d.duracionMaxMin() < d.duracionMinMin()) {
            throw invalida("La duración máxima debe ser mayor o igual a la mínima, y esta al menos un bloque.");
        }
        if ((d.largoM() != null && d.largoM().signum() <= 0) || (d.anchoM() != null && d.anchoM().signum() <= 0)) {
            throw invalida("Las dimensiones deben ser positivas.");
        }
        this.jugadoresPorEquipo = d.jugadoresPorEquipo();
        this.capacidadTotal = d.capacidadTotal();
        this.superficie = d.superficie() == null || d.superficie().isBlank() ? "Sintética" : d.superficie().trim();
        this.largoM = d.largoM();
        this.anchoM = d.anchoM();
        this.techada = d.techada();
        this.iluminacion = d.iluminacion();
        this.precioBaseHora = d.precioBaseHora();
        this.duracionBloqueMin = d.duracionBloqueMin();
        this.duracionMinMin = d.duracionMinMin();
        this.duracionMaxMin = d.duracionMaxMin();
        this.descripcion = Sede.vacioANulo(d.descripcion());
        this.orden = d.orden();
    }

    /** Define de qué canchas depende (sus componentes). Una cancha no puede depender de sí misma. */
    public void definirComponentes(Set<Long> nuevas) {
        Set<Long> ids = nuevas == null ? Set.of() : Set.copyOf(nuevas);
        if (id != null && ids.contains(id)) {
            throw invalida("Una cancha no puede depender de sí misma.");
        }
        this.componentes = ids;
    }

    public void cambiarEstado(EstadoCancha nuevo) {
        this.estado = nuevo;
    }

    public void definirHorarioPropio(HorarioSemanal horario) {
        this.horarioPropio = horario;
    }

    public boolean esReservable() {
        return estado == EstadoCancha.ACTIVA;
    }

    public boolean esCompuesta() {
        return !componentes.isEmpty();
    }

    private static ExcepcionNegocio invalida(String mensaje) {
        return new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, mensaje);
    }

    public Long id() { return id; }
    public long sedeId() { return sedeId; }
    public String nombre() { return nombre; }
    public String modalidad() { return modalidad; }
    public int jugadoresPorEquipo() { return jugadoresPorEquipo; }
    public int capacidadTotal() { return capacidadTotal; }
    public String superficie() { return superficie; }
    public BigDecimal largoM() { return largoM; }
    public BigDecimal anchoM() { return anchoM; }
    public boolean techada() { return techada; }
    public boolean iluminacion() { return iluminacion; }
    public int precioBaseHora() { return precioBaseHora; }
    public int duracionBloqueMin() { return duracionBloqueMin; }
    public int duracionMinMin() { return duracionMinMin; }
    public int duracionMaxMin() { return duracionMaxMin; }
    public EstadoCancha estado() { return estado; }
    public String descripcion() { return descripcion; }
    public int orden() { return orden; }
    public HorarioSemanal horarioPropio() { return horarioPropio; }
    public Set<Long> componentes() { return componentes; }

    /** Datos editables de una cancha. */
    public record DatosCancha(String nombre, String modalidad, int jugadoresPorEquipo, int capacidadTotal,
            String superficie, BigDecimal largoM, BigDecimal anchoM, boolean techada, boolean iluminacion,
            int precioBaseHora, int duracionBloqueMin, int duracionMinMin, int duracionMaxMin,
            String descripcion, int orden) {
    }
}
