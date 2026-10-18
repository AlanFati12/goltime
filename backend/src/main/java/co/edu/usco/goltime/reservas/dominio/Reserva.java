package co.edu.usco.goltime.reservas.dominio;

import java.time.Duration;
import java.time.Instant;

import co.edu.usco.goltime.compartido.dominio.Actor;
import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.compartido.dominio.Rol;

/**
 * Reserva de una cancha. Clase de dominio pura: concentra la máquina de estados y las políticas de
 * cancelación y reprogramación.
 *
 * <pre>
 * PENDIENTE  -> CONFIRMADA (aprueba el admin) | RECHAZADA | EXPIRADA | CANCELADA
 * CONFIRMADA -> CANCELADA | COMPLETADA | NO_ASISTIO
 * COMPLETADA -> NO_ASISTIO (corrección del admin)
 * </pre>
 */
public class Reserva {

    private Long id;
    private String codigo;
    private Long clienteId;
    private Long clienteMostradorId;
    private long canchaId;
    private Instant inicio;
    private Instant fin;
    private EstadoReserva estado;
    private OrigenReserva origen;
    private Precio precio;
    private EstadoPago estadoPago;
    private int montoPagado;
    private String nombreEquipo;
    private String notas;
    private Long canceladaPor;
    private Rol rolCancelador;
    private String motivoCancelacion;
    private Instant fechaCancelacion;
    private boolean cancelacionTardia;
    private int vecesReprogramada;
    private Long aprobadaPor;
    private Instant fechaAprobacion;
    private String motivoRechazo;
    private Instant expiraEn;
    private String claveIdempotencia;
    private Instant recordatorioEnviadoEn;
    private Instant creadoEn;

    private Reserva() {
    }

    /** Solicitud de un cliente por la web: nace PENDIENTE y retiene el horario hasta que el admin responda. */
    public static Reserva solicitar(String codigo, long clienteId, long canchaId, Instant inicio, Instant fin,
            Precio precio, String nombreEquipo, String notas, Instant expiraEn, String claveIdempotencia) {
        Reserva r = new Reserva();
        r.codigo = codigo;
        r.clienteId = clienteId;
        r.canchaId = canchaId;
        r.inicio = inicio;
        r.fin = fin;
        r.precio = precio;
        r.nombreEquipo = limpiar(nombreEquipo);
        r.notas = limpiar(notas);
        r.estado = EstadoReserva.PENDIENTE;
        r.origen = OrigenReserva.WEB;
        r.estadoPago = EstadoPago.PENDIENTE;
        r.expiraEn = expiraEn;
        r.claveIdempotencia = claveIdempotencia;
        return r;
    }

    /** Reconstruye una reserva existente desde el almacenamiento. */
    public static Reserva reconstruir(Long id, String codigo, Long clienteId, Long clienteMostradorId, long canchaId,
            Instant inicio, Instant fin, EstadoReserva estado, OrigenReserva origen, Precio precio, EstadoPago estadoPago,
            int montoPagado, String nombreEquipo, String notas, Long canceladaPor, Rol rolCancelador,
            String motivoCancelacion, Instant fechaCancelacion, boolean cancelacionTardia, int vecesReprogramada,
            Long aprobadaPor, Instant fechaAprobacion, String motivoRechazo, Instant expiraEn,
            String claveIdempotencia, Instant recordatorioEnviadoEn, Instant creadoEn) {
        Reserva r = new Reserva();
        r.id = id;
        r.codigo = codigo;
        r.clienteId = clienteId;
        r.clienteMostradorId = clienteMostradorId;
        r.canchaId = canchaId;
        r.inicio = inicio;
        r.fin = fin;
        r.estado = estado;
        r.origen = origen;
        r.precio = precio;
        r.estadoPago = estadoPago;
        r.montoPagado = montoPagado;
        r.nombreEquipo = nombreEquipo;
        r.notas = notas;
        r.canceladaPor = canceladaPor;
        r.rolCancelador = rolCancelador;
        r.motivoCancelacion = motivoCancelacion;
        r.fechaCancelacion = fechaCancelacion;
        r.cancelacionTardia = cancelacionTardia;
        r.vecesReprogramada = vecesReprogramada;
        r.aprobadaPor = aprobadaPor;
        r.fechaAprobacion = fechaAprobacion;
        r.motivoRechazo = motivoRechazo;
        r.expiraEn = expiraEn;
        r.claveIdempotencia = claveIdempotencia;
        r.recordatorioEnviadoEn = recordatorioEnviadoEn;
        r.creadoEn = creadoEn;
        return r;
    }

    // ---------- Transiciones ----------

    /** El administrador aprueba una solicitud pendiente que aún no expiró. */
    public void aprobar(long adminId, Instant ahora) {
        exigirEstado(EstadoReserva.PENDIENTE, "Solo se pueden aprobar solicitudes pendientes.");
        if (expiraEn != null && !expiraEn.isAfter(ahora)) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "La solicitud ya expiró.");
        }
        if (!inicio.isAfter(ahora)) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "La reserva ya comenzó.");
        }
        this.estado = EstadoReserva.CONFIRMADA;
        this.aprobadaPor = adminId;
        this.fechaAprobacion = ahora;
        this.expiraEn = null;
    }

    public void rechazar(long adminId, String motivo, Instant ahora) {
        exigirEstado(EstadoReserva.PENDIENTE, "Solo se pueden rechazar solicitudes pendientes.");
        String motivoValido = motivoObligatorio(motivo, "El motivo del rechazo es obligatorio.");
        this.estado = EstadoReserva.RECHAZADA;
        this.motivoRechazo = motivoValido;
        this.aprobadaPor = adminId;
        this.fechaAprobacion = ahora;
        this.expiraEn = null;
    }

    /** Pasa a EXPIRADA una solicitud que nadie respondió a tiempo. */
    public void expirar(Instant ahora) {
        exigirEstado(EstadoReserva.PENDIENTE, "Solo expiran las solicitudes pendientes.");
        if (expiraEn == null || expiraEn.isAfter(ahora)) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "La solicitud todavía no expira.");
        }
        this.estado = EstadoReserva.EXPIRADA;
    }

    /**
     * Cancela la reserva. Un cliente cancela sin motivo obligatorio; si la reserva estaba CONFIRMADA y
     * faltan menos de las horas mínimas queda marcada como cancelación tardía (el admin decide qué hacer
     * con un abono). El administrador cancela siempre con motivo y nunca es tardía.
     */
    public void cancelar(Actor actor, String motivo, Instant ahora, PoliticaReservas politica) {
        if (!estado.ocupaHorario()) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "Esta reserva ya no se puede cancelar.");
        }
        if (!inicio.isAfter(ahora)) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "La reserva ya comenzó y no se puede cancelar.");
        }
        boolean porAdmin = actor.rol() != Rol.CLIENTE;
        this.motivoCancelacion = porAdmin ? motivoObligatorio(motivo, "El motivo de la cancelación es obligatorio.") : limpiar(motivo);
        this.cancelacionTardia = !porAdmin && estado == EstadoReserva.CONFIRMADA && !politica.conAntelacionSuficiente(ahora, inicio);
        this.estado = EstadoReserva.CANCELADA;
        this.canceladaPor = actor.id();
        this.rolCancelador = actor.rol();
        this.fechaCancelacion = ahora;
        this.expiraEn = null;
    }

    /**
     * Mueve la reserva a otra cancha, fecha u hora. El cliente tiene un máximo de reprogramaciones y
     * debe pedirlo con la antelación mínima; el administrador no tiene esos límites. El nuevo precio
     * reemplaza al anterior. Una solicitud pendiente renueva su plazo de expiración.
     */
    public void reprogramar(Actor actor, long nuevaCanchaId, Instant nuevoInicio, Instant nuevoFin, Precio nuevoPrecio,
            Instant ahora, PoliticaReservas politica, Instant nuevaExpiracion) {
        if (!estado.ocupaHorario()) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "Esta reserva ya no se puede reprogramar.");
        }
        if (!inicio.isAfter(ahora)) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "La reserva ya comenzó y no se puede reprogramar.");
        }
        if (actor.rol() == Rol.CLIENTE) {
            if (vecesReprogramada >= politica.maxReprogramaciones()) {
                throw new ExcepcionNegocio(CodigoError.REPROGRAMACION_NO_PERMITIDA,
                        "Alcanzaste el máximo de " + politica.maxReprogramaciones() + " reprogramaciones para esta reserva.");
            }
            if (!politica.conAntelacionSuficiente(ahora, inicio)) {
                throw new ExcepcionNegocio(CodigoError.REPROGRAMACION_NO_PERMITIDA,
                        "Solo puedes reprogramar con al menos " + politica.horasMinCancelacion() + " horas de antelación.");
            }
        }
        this.canchaId = nuevaCanchaId;
        this.inicio = nuevoInicio;
        this.fin = nuevoFin;
        this.precio = nuevoPrecio;
        this.vecesReprogramada++;
        if (estado == EstadoReserva.PENDIENTE) {
            this.expiraEn = nuevaExpiracion;
        }
    }

    /** Una reserva confirmada cuya hora de fin ya pasó se da por jugada (job automático o el admin). */
    public void completar(Instant ahora) {
        exigirEstado(EstadoReserva.CONFIRMADA, "Solo se pueden completar reservas confirmadas.");
        if (fin.isAfter(ahora)) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "La reserva todavía no termina.");
        }
        this.estado = EstadoReserva.COMPLETADA;
    }

    /** El administrador marca que el cliente no se presentó (también corrige una completada por error). */
    public void marcarNoAsistio(Instant ahora) {
        if (estado != EstadoReserva.CONFIRMADA && estado != EstadoReserva.COMPLETADA) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "Solo se puede marcar no asistió en reservas confirmadas o completadas.");
        }
        if (inicio.isAfter(ahora)) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "La reserva todavía no comienza.");
        }
        this.estado = EstadoReserva.NO_ASISTIO;
    }

    public void registrarRecordatorio(Instant ahora) {
        this.recordatorioEnviadoEn = ahora;
    }

    // ---------- Consultas de política (para la interfaz) ----------

    public boolean puedeCancelar(Instant ahora) {
        return estado.ocupaHorario() && inicio.isAfter(ahora);
    }

    public boolean puedeReprogramar(Instant ahora, PoliticaReservas politica) {
        return estado.ocupaHorario() && inicio.isAfter(ahora) && vecesReprogramada < politica.maxReprogramaciones()
                && politica.conAntelacionSuficiente(ahora, inicio);
    }

    /** Si el cliente cancelara ahora, ¿sería una cancelación tardía? */
    public boolean seriaCancelacionTardia(Instant ahora, PoliticaReservas politica) {
        return estado == EstadoReserva.CONFIRMADA && !politica.conAntelacionSuficiente(ahora, inicio);
    }

    public int duracionMin() {
        return (int) Duration.between(inicio, fin).toMinutes();
    }

    public boolean esDe(long usuarioId) {
        return clienteId != null && clienteId == usuarioId;
    }

    // ---------- Utilidades ----------

    private void exigirEstado(EstadoReserva esperado, String mensaje) {
        if (estado != esperado) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, mensaje);
        }
    }

    private static String motivoObligatorio(String motivo, String mensaje) {
        if (motivo == null || motivo.isBlank()) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, mensaje);
        }
        return motivo.trim();
    }

    private static String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    public void asignarId(long id) { this.id = id; }

    public Long id() { return id; }
    public String codigo() { return codigo; }
    public Long clienteId() { return clienteId; }
    public Long clienteMostradorId() { return clienteMostradorId; }
    public long canchaId() { return canchaId; }
    public Instant inicio() { return inicio; }
    public Instant fin() { return fin; }
    public EstadoReserva estado() { return estado; }
    public OrigenReserva origen() { return origen; }
    public Precio precio() { return precio; }
    public EstadoPago estadoPago() { return estadoPago; }
    public int montoPagado() { return montoPagado; }
    public String nombreEquipo() { return nombreEquipo; }
    public String notas() { return notas; }
    public Long canceladaPor() { return canceladaPor; }
    public Rol rolCancelador() { return rolCancelador; }
    public String motivoCancelacion() { return motivoCancelacion; }
    public Instant fechaCancelacion() { return fechaCancelacion; }
    public boolean cancelacionTardia() { return cancelacionTardia; }
    public int vecesReprogramada() { return vecesReprogramada; }
    public Long aprobadaPor() { return aprobadaPor; }
    public Instant fechaAprobacion() { return fechaAprobacion; }
    public String motivoRechazo() { return motivoRechazo; }
    public Instant expiraEn() { return expiraEn; }
    public String claveIdempotencia() { return claveIdempotencia; }
    public Instant recordatorioEnviadoEn() { return recordatorioEnviadoEn; }
    public Instant creadoEn() { return creadoEn; }
}
