package co.edu.usco.goltime.reservas.aplicacion;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.usco.goltime.auditoria.aplicacion.RegistroAuditoria;
import co.edu.usco.goltime.compartido.dominio.Actor;
import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.compartido.dominio.FormatoTexto;
import co.edu.usco.goltime.compartido.dominio.Pagina;
import co.edu.usco.goltime.compartido.dominio.Rol;
import co.edu.usco.goltime.compartido.dominio.SolicitudPagina;
import co.edu.usco.goltime.compartido.dominio.ZonaHoraria;
import co.edu.usco.goltime.notificaciones.aplicacion.Notificador;
import co.edu.usco.goltime.reservas.aplicacion.RepositorioPagos.PagoRegistrado;
import co.edu.usco.goltime.reservas.aplicacion.RepositorioReservas.FiltroReservasAdmin;
import co.edu.usco.goltime.reservas.aplicacion.Vistas.VistaReserva;
import co.edu.usco.goltime.reservas.dominio.CompletadaRevertidaEvento;
import co.edu.usco.goltime.reservas.dominio.EstadoPago;
import co.edu.usco.goltime.reservas.dominio.EstadoReserva;
import co.edu.usco.goltime.reservas.dominio.MetodoPago;
import co.edu.usco.goltime.reservas.dominio.Pago;
import co.edu.usco.goltime.reservas.dominio.PoliticaReservas;
import co.edu.usco.goltime.reservas.dominio.Precio;
import co.edu.usco.goltime.reservas.dominio.Reserva;
import co.edu.usco.goltime.reservas.dominio.ReservaCompletadaEvento;
import co.edu.usco.goltime.reservas.dominio.TipoPago;
import co.edu.usco.goltime.sedes.aplicacion.ServicioCanchas;
import co.edu.usco.goltime.sedes.aplicacion.Vistas.VistaCancha;
import co.edu.usco.goltime.usuarios.aplicacion.RepositorioUsuarios;
import co.edu.usco.goltime.usuarios.dominio.Usuario;

/**
 * Casos de uso del administrador sobre reservas: crearlas a nombre de un cliente (registrado o de mostrador),
 * marcarlas como jugadas o no asistidas, registrar pagos, abonos y reembolsos, y exportar el listado.
 * Cada acción queda en el historial de la reserva y en la bitácora de auditoría.
 */
@Service
public class ServicioReservasAdmin {

    private static final String ENTIDAD = "reservas";

    private final RepositorioReservas reservas;
    private final RepositorioOcupaciones ocupaciones;
    private final RepositorioHistorial historial;
    private final RepositorioPagos pagos;
    private final RepositorioMostrador mostrador;
    private final RepositorioUsuarios usuarios;
    private final ServicioCanchas canchas;
    private final ServicioReservas servicio;
    private final CalculosReserva calculos;
    private final ConfiguracionReservas configuracion;
    private final Notificador notificador;
    private final RegistroAuditoria auditoria;
    private final ApplicationEventPublisher eventos;
    private final Clock reloj;

    public ServicioReservasAdmin(RepositorioReservas reservas, RepositorioOcupaciones ocupaciones, RepositorioHistorial historial,
            RepositorioPagos pagos, RepositorioMostrador mostrador, RepositorioUsuarios usuarios, ServicioCanchas canchas,
            ServicioReservas servicio, CalculosReserva calculos, ConfiguracionReservas configuracion, Notificador notificador,
            RegistroAuditoria auditoria, ApplicationEventPublisher eventos, Clock reloj) {
        this.reservas = reservas;
        this.ocupaciones = ocupaciones;
        this.historial = historial;
        this.pagos = pagos;
        this.mostrador = mostrador;
        this.usuarios = usuarios;
        this.canchas = canchas;
        this.servicio = servicio;
        this.calculos = calculos;
        this.configuracion = configuracion;
        this.notificador = notificador;
        this.auditoria = auditoria;
        this.eventos = eventos;
        this.reloj = reloj;
    }

    /** Datos para crear una reserva: un cliente registrado ({@code clienteId}) o uno de mostrador (nombre y teléfono). */
    public record DatosReservaAdmin(long canchaId, Instant inicio, Instant fin, Long clienteId, String mostradorNombre,
            String mostradorTelefono, String nombreEquipo, String notas) {
    }

    // ===================================================================================
    // Crear
    // ===================================================================================

    /** Crea una reserva ya CONFIRMADA (no necesita aprobación) con las reglas del administrador. */
    @Transactional
    public VistaReserva crear(Actor admin, DatosReservaAdmin d) {
        Instant ahora = reloj.instant();
        PoliticaReservas politica = configuracion.politica().paraAdministrador();
        VistaCancha cancha = canchas.detallePublico(d.canchaId());
        calculos.validarFranja(cancha, d.inicio(), d.fin(), ahora, politica);

        boolean traeMostrador = (d.mostradorNombre() != null && !d.mostradorNombre().isBlank())
                || (d.mostradorTelefono() != null && !d.mostradorTelefono().isBlank());
        if (d.clienteId() != null && traeMostrador) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Indica un cliente registrado o uno de mostrador, no ambos.");
        }
        Long clienteId = null;
        Long mostradorId = null;
        if (d.clienteId() != null) {
            Usuario cliente = usuarios.buscarPorId(d.clienteId()).filter(u -> u.rol() == Rol.CLIENTE).orElseThrow(
                    () -> new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "El cliente no existe."));
            if (!cliente.activo()) {
                throw new ExcepcionNegocio(CodigoError.USUARIO_DESACTIVADO, "El cliente está desactivado.");
            }
            clienteId = cliente.id();
        } else {
            if (d.mostradorNombre() == null || d.mostradorNombre().isBlank() || d.mostradorTelefono() == null || d.mostradorTelefono().isBlank()) {
                throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Indica el cliente registrado o el nombre y teléfono del cliente de mostrador.");
            }
            mostradorId = mostrador.buscarOCrear(d.mostradorNombre().trim(), d.mostradorTelefono().trim());
        }

        Precio precio = calculos.calcularPrecio(cancha, d.inicio(), d.fin());
        Reserva nueva = Reserva.crearPorAdmin(reservas.siguienteCodigo(ZonaHoraria.fechaLocal(ahora).getYear()), clienteId, mostradorId,
                d.canchaId(), d.inicio(), d.fin(), precio, d.nombreEquipo(), d.notas(), admin.id(), ahora);
        Reserva guardada = reservas.guardar(nueva);
        ocupaciones.ocupar(guardada.id(), CalculosReserva.idsQueOcupa(cancha), d.inicio(), d.fin());

        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("canchaId", guardada.canchaId());
        datos.put("inicio", guardada.inicio().toString());
        datos.put("fin", guardada.fin().toString());
        datos.put("precioFinal", guardada.precio().finalAPagar());
        datos.put("estado", guardada.estado().name());
        datos.put("origen", guardada.origen().name());
        historial.registrar(new EntradaHistorial(null, guardada.id(), "CREADA", admin.id(), null, Rol.ADMIN, null, null, datos, ahora));
        auditoria.registrar("CREAR_RESERVA_ADMIN", ENTIDAD, guardada.id(),
                Map.of("codigo", guardada.codigo(), "canchaId", guardada.canchaId(), "cliente", clienteId != null ? "usuario " + clienteId : "mostrador " + mostradorId), null);
        if (clienteId != null) {
            notificador.notificar(clienteId, "RESERVA_CREADA_ADMIN", "Te creamos una reserva",
                    "El administrador registró la reserva %s para el %s en %s.".formatted(guardada.codigo(), FormatoTexto.fechaHora(guardada.inicio()),
                            cancha.cancha().nombre()), "reserva", guardada.id());
        }
        return servicio.detalle(guardada.id(), admin);
    }

    // ===================================================================================
    // Cambios de estado
    // ===================================================================================

    /** Marca la reserva como COMPLETADA (ya terminó) o NO_ASISTIO (ya empezó; también corrige una completada por error). */
    @Transactional
    public VistaReserva cambiarEstado(long id, Actor admin, EstadoReserva destino) {
        Reserva reserva = cargar(id);
        EstadoReserva antes = reserva.estado();
        Instant ahora = reloj.instant();
        switch (destino) {
            case COMPLETADA -> reserva.completar(ahora);
            case NO_ASISTIO -> reserva.marcarNoAsistio(ahora);
            default -> throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Solo se puede marcar como COMPLETADA o NO_ASISTIO.");
        }
        Reserva guardada = reservas.guardar(reserva);
        historial.registrar(new EntradaHistorial(null, id, destino.name(), admin.id(), null, Rol.ADMIN, null, Map.of("estado", antes.name()),
                Map.of("estado", destino.name()), ahora));
        auditoria.registrar("MARCAR_" + destino.name() + "_RESERVA", ENTIDAD, id, Map.of("estado", Map.of("antes", antes.name(), "despues", destino.name())), null);
        if (antes == EstadoReserva.COMPLETADA && destino != EstadoReserva.COMPLETADA) {
            eventos.publishEvent(new CompletadaRevertidaEvento(id, guardada.clienteId()));
        }
        if (destino == EstadoReserva.COMPLETADA) {
            eventos.publishEvent(new ReservaCompletadaEvento(id, guardada.clienteId(), guardada.canchaId(), guardada.duracionMin()));
        }
        return servicio.detalle(id, admin);
    }

    // ===================================================================================
    // Pagos
    // ===================================================================================

    /** Registra un pago o abono (solo reservas confirmadas o ya jugadas, sin pasarse del valor). */
    @Transactional
    public VistaReserva registrarPago(long id, Actor admin, int monto, MetodoPago metodo, String referencia) {
        Reserva reserva = cargar(id);
        EstadoPago antes = reserva.estadoPago();
        int pagadoAntes = reserva.montoPagado();
        TipoPago tipo = reserva.aplicarPago(monto);
        reservas.guardar(reserva);
        return registrarMovimiento(reserva, admin, monto, metodo, referencia, tipo, antes, pagadoAntes, "REGISTRAR_PAGO");
    }

    /** Registra una devolución de dinero. Con {@code cerrar} se da por saldado el reembolso aunque quede dinero retenido. */
    @Transactional
    public VistaReserva registrarReembolso(long id, Actor admin, int monto, MetodoPago metodo, String referencia, boolean cerrar) {
        Reserva reserva = cargar(id);
        EstadoPago antes = reserva.estadoPago();
        int pagadoAntes = reserva.montoPagado();
        reserva.aplicarReembolso(monto, cerrar);
        reservas.guardar(reserva);
        return registrarMovimiento(reserva, admin, monto, metodo, referencia, TipoPago.REEMBOLSO, antes, pagadoAntes, "REGISTRAR_REEMBOLSO");
    }

    private VistaReserva registrarMovimiento(Reserva reserva, Actor admin, int monto, MetodoPago metodo, String referencia, TipoPago tipo,
            EstadoPago estadoAntes, int pagadoAntes, String accionAuditoria) {
        Instant ahora = reloj.instant();
        pagos.registrar(new Pago(null, reserva.id(), monto, metodo, referencia == null || referencia.isBlank() ? null : referencia.trim(), tipo, admin.id(), ahora));
        Map<String, Object> antes = Map.of("montoPagado", pagadoAntes, "estadoPago", estadoAntes.name());
        Map<String, Object> despues = Map.of("montoPagado", reserva.montoPagado(), "estadoPago", reserva.estadoPago().name());
        String detalle = "%s de %s por %s%s".formatted(tipo.name(), FormatoTexto.cop(monto), metodo.name(), referencia == null || referencia.isBlank() ? "" : " (ref. " + referencia.trim() + ")");
        historial.registrar(new EntradaHistorial(null, reserva.id(), "PAGO_REGISTRADO", admin.id(), null, Rol.ADMIN, detalle, antes, despues, ahora));
        Map<String, Object> cambios = new LinkedHashMap<>();
        cambios.put("tipo", tipo.name());
        cambios.put("monto", monto);
        cambios.put("metodo", metodo.name());
        cambios.put("estadoPago", Map.of("antes", estadoAntes.name(), "despues", reserva.estadoPago().name()));
        auditoria.registrar(accionAuditoria, ENTIDAD, reserva.id(), cambios, null);
        return servicio.detalle(reserva.id(), admin);
    }

    @Transactional(readOnly = true)
    public List<PagoRegistrado> pagosDe(long reservaId) {
        cargar(reservaId);
        return pagos.deReserva(reservaId);
    }

    // ===================================================================================
    // Consultas
    // ===================================================================================

    @Transactional(readOnly = true)
    public List<RepositorioMostrador.ClienteMostrador> buscarMostrador(String texto) {
        return mostrador.buscar(texto, 10);
    }

    /** Todas las reservas que cumplen el filtro (hasta 5.000), para exportar a CSV. */
    @Transactional(readOnly = true)
    public List<VistaReserva> paraExportar(FiltroReservasAdmin filtro) {
        List<VistaReserva> todas = new ArrayList<>();
        for (int pagina = 0; pagina < 50; pagina++) {
            Pagina<VistaReserva> bloque = servicio.listarAdmin(filtro, new SolicitudPagina(pagina, SolicitudPagina.TAMANO_MAXIMO));
            todas.addAll(bloque.contenido());
            if (pagina + 1 >= bloque.totalPaginas()) {
                break;
            }
        }
        return todas;
    }

    private Reserva cargar(long id) {
        return reservas.buscarPorId(id).orElseThrow(() -> new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "La reserva no existe."));
    }
}
