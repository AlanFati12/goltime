package co.edu.usco.goltime.whatsapp.aplicacion;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.usco.goltime.autenticacion.aplicacion.ServicioVerificacionTelefono;
import co.edu.usco.goltime.autenticacion.aplicacion.ServicioVerificacionTelefono.CodigoEnviado;
import co.edu.usco.goltime.autenticacion.aplicacion.ServicioVerificacionTelefono.Identificacion;
import co.edu.usco.goltime.autenticacion.aplicacion.ServicioVerificacionTelefono.Situacion;
import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.compartido.dominio.ZonaHoraria;
import co.edu.usco.goltime.reservas.aplicacion.RepositorioMostrador.ClienteMostrador;
import co.edu.usco.goltime.reservas.aplicacion.ServicioClientesSinCuenta;
import co.edu.usco.goltime.reservas.aplicacion.ServicioReservas;
import co.edu.usco.goltime.reservas.aplicacion.Titular;
import co.edu.usco.goltime.reservas.aplicacion.Vistas.PrevisualizacionReprogramacion;
import co.edu.usco.goltime.reservas.aplicacion.Vistas.ResultadoSolicitud;
import co.edu.usco.goltime.reservas.aplicacion.Vistas.VistaReserva;

/**
 * Lo que el bot hace a nombre de quien escribe. El número llega de n8n (tomado del mensaje de WhatsApp, nunca
 * del modelo de lenguaje) y decide quién es el titular: el cliente con ese número verificado o, si no hay cuenta,
 * un cliente sin cuenta. Toda regla de negocio la aplica el módulo de reservas, igual que en la web.
 */
@Service
public class ServicioBotWhatsapp {

    static final int MAX_RESERVAS_LISTADAS = 10;

    private final ServicioVerificacionTelefono verificacion;
    private final ServicioClientesSinCuenta sinCuenta;
    private final ServicioReservas reservas;
    private final ConfiguracionBot configuracion;

    public ServicioBotWhatsapp(ServicioVerificacionTelefono verificacion, ServicioClientesSinCuenta sinCuenta, ServicioReservas reservas,
            ConfiguracionBot configuracion) {
        this.verificacion = verificacion;
        this.sinCuenta = sinCuenta;
        this.reservas = reservas;
        this.configuracion = configuracion;
    }

    /**
     * Quién escribe. {@code nombre} es el del cliente registrado o el que dio la vez anterior quien no tiene cuenta.
     */
    public record Remitente(Situacion situacion, String telefono, String nombre, boolean conReservasSinCuenta) {
    }

    @Transactional(readOnly = true)
    public Remitente identificar(String numero) {
        Identificacion id = verificacion.identificar(numero);
        String nombre = switch (id.situacion()) {
            case CLIENTE -> id.nombres();
            case SIN_CUENTA -> sinCuenta.buscarPorTelefono(id.telefono()).map(ClienteMostrador::nombre).orElse(null);
            default -> null;
        };
        return new Remitente(id.situacion(), id.telefono(), nombre, id.situacion() == Situacion.SIN_CUENTA && configuracion.reservasSinCuenta());
    }

    // ---------- Vinculación del número con la cuenta ----------

    @Transactional
    public CodigoEnviado enviarCodigo(String numero) {
        verificarActivo();
        return verificacion.solicitarCodigo(numero);
    }

    @Transactional(noRollbackFor = ExcepcionNegocio.class)
    public String confirmarCodigo(String numero, String codigo) {
        verificarActivo();
        return verificacion.confirmarCodigo(numero, codigo).nombres();
    }

    // ---------- Reservas ----------

    /** Franja en hora de Bogotá. Si la hora de fin no es posterior a la de inicio, termina al día siguiente (23:00 a 00:00). */
    public record Franja(LocalDate fecha, LocalTime desde, LocalTime hasta) {

        public Instant inicio() {
            return ZonaHoraria.aInstante(fecha, desde);
        }

        public Instant fin() {
            return ZonaHoraria.aInstante(hasta.isAfter(desde) ? fecha : fecha.plusDays(1), hasta);
        }
    }

    /**
     * Solicita la reserva (nace PENDIENTE y la aprueba el administrador). {@code claveIdempotencia} es el id del
     * mensaje de WhatsApp: si n8n reintenta, no se crea dos veces.
     */
    @Transactional
    public ResultadoSolicitud solicitar(String numero, String nombre, long canchaId, Franja franja, String nombreEquipo,
            String claveIdempotencia) {
        verificarActivo();
        Titular titular = titularParaReservar(numero, nombre);
        return reservas.solicitarPorWhatsapp(titular, canchaId, franja.inicio(), franja.fin(), nombreEquipo, null, claveIdempotencia);
    }

    @Transactional(readOnly = true)
    public List<VistaReserva> misReservas(String numero) {
        verificarActivo();
        Titular titular = titularExistente(numero);
        return titular == null ? List.of() : reservas.proximasDe(titular, MAX_RESERVAS_LISTADAS);
    }

    @Transactional
    public VistaReserva cancelar(String numero, String codigo, String motivo) {
        verificarActivo();
        Titular titular = titularObligatorio(numero);
        return reservas.cancelar(reservas.idDeCodigo(codigo, titular), titular, motivo);
    }

    @Transactional(readOnly = true)
    public PrevisualizacionReprogramacion previsualizarReprogramacion(String numero, String codigo, Long canchaId, Franja franja) {
        verificarActivo();
        Titular titular = titularObligatorio(numero);
        return reservas.previsualizarReprogramacion(reservas.idDeCodigo(codigo, titular), titular, canchaId, franja.inicio(), franja.fin());
    }

    @Transactional
    public VistaReserva reprogramar(String numero, String codigo, Long canchaId, Franja franja, String motivo) {
        verificarActivo();
        Titular titular = titularObligatorio(numero);
        return reservas.reprogramar(reservas.idDeCodigo(codigo, titular), titular, canchaId, franja.inicio(), franja.fin(), motivo);
    }

    public void verificarActivo() {
        if (!configuracion.activo()) {
            throw new ExcepcionNegocio(CodigoError.SERVICIO_NO_DISPONIBLE,
                    "El bot de WhatsApp está en pausa. Puedes reservar en la web o comunicarte con el administrador.");
        }
    }

    /** El titular para una reserva nueva; quien no tiene cuenta queda registrado con su nombre la primera vez. */
    private Titular titularParaReservar(String numero, String nombre) {
        Identificacion id = verificacion.identificar(numero);
        return switch (id.situacion()) {
            case CLIENTE -> Titular.cliente(id.usuarioId());
            case SIN_CUENTA -> {
                if (!configuracion.reservasSinCuenta()) {
                    throw new ExcepcionNegocio(CodigoError.ACCESO_DENEGADO,
                            "Para reservar por WhatsApp necesitas una cuenta en Goltime con este número. Regístrate en la web.");
                }
                ClienteMostrador cliente = sinCuenta.buscarPorTelefono(id.telefono())
                        .orElseGet(() -> sinCuenta.registrar(nombre, id.telefono()));
                yield Titular.sinCuenta(cliente.id());
            }
            default -> throw sinTitular(id.situacion());
        };
    }

    /** El titular de las reservas de este número, o {@code null} si nunca ha reservado sin cuenta. */
    private Titular titularExistente(String numero) {
        Identificacion id = verificacion.identificar(numero);
        return switch (id.situacion()) {
            case CLIENTE -> Titular.cliente(id.usuarioId());
            case SIN_CUENTA -> sinCuenta.buscarPorTelefono(id.telefono()).map(c -> Titular.sinCuenta(c.id())).orElse(null);
            default -> throw sinTitular(id.situacion());
        };
    }

    private Titular titularObligatorio(String numero) {
        Titular titular = titularExistente(numero);
        if (titular == null) {
            throw new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "No encontramos esa reserva entre las tuyas.");
        }
        return titular;
    }

    private static ExcepcionNegocio sinTitular(Situacion situacion) {
        return situacion == Situacion.DESACTIVADO
                ? new ExcepcionNegocio(CodigoError.USUARIO_DESACTIVADO, "Tu cuenta está desactivada. Comunícate con el administrador.")
                : new ExcepcionNegocio(CodigoError.WHATSAPP_SIN_VINCULAR,
                        "Este número es de una cuenta Goltime que aún no está vinculada a WhatsApp. Vincúlala primero con el código que enviamos a tu correo.");
    }
}
