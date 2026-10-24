package co.edu.usco.goltime.whatsapp.aplicacion;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

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
 * Lo que el bot hace a nombre de quien escribe. El remitente llega de n8n (tomado del mensaje de WhatsApp, nunca
 * del modelo de lenguaje) y decide quién es el titular de las reservas:
 * <ul>
 * <li>con teléfono: el cliente que lo verificó o, si no hay cuenta, un cliente sin cuenta con ese teléfono;</li>
 * <li>con @lid (WhatsApp no compartió el número): el cliente que lo vinculó con el código del correo o, si no,
 * un cliente sin cuenta atado a ese @lid.</li>
 * </ul>
 * Toda regla de negocio la aplica el módulo de reservas, igual que en la web.
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
     * Quién escribe. {@code nombre} es el del cliente registrado o el que dio la vez anterior quien no tiene cuenta;
     * {@code porLid} indica que WhatsApp no compartió su número.
     */
    public record Remitente(Situacion situacion, String telefono, String nombre, boolean conReservasSinCuenta, boolean porLid) {
    }

    @Transactional(readOnly = true)
    public Remitente identificar(RemitenteWhatsapp quien) {
        Identificacion id = identificacion(quien);
        String nombre = switch (id.situacion()) {
            case CLIENTE -> id.nombres();
            case SIN_CUENTA -> sinCuentaExistente(quien).map(ClienteMostrador::nombre).orElse(null);
            default -> null;
        };
        return new Remitente(id.situacion(), quien.telefono(), nombre, id.situacion() == Situacion.SIN_CUENTA && configuracion.reservasSinCuenta(),
                quien.porLid());
    }

    // ---------- Vinculación del WhatsApp con la cuenta ----------

    /** Con teléfono, el código va a la cuenta que escribió ese número; con @lid, a la cuenta del correo que dé. */
    @Transactional
    public CodigoEnviado enviarCodigo(RemitenteWhatsapp quien, String correo) {
        verificarActivo();
        return quien.porLid() ? verificacion.solicitarCodigoDesdeLid(quien.lid(), correo) : verificacion.solicitarCodigo(quien.telefono());
    }

    @Transactional(noRollbackFor = ExcepcionNegocio.class)
    public String confirmarCodigo(RemitenteWhatsapp quien, String codigo) {
        verificarActivo();
        return (quien.porLid() ? verificacion.confirmarCodigoDesdeLid(quien.lid(), codigo) : verificacion.confirmarCodigo(quien.telefono(), codigo))
                .nombres();
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
     * mensaje de WhatsApp: si n8n reintenta, no se crea dos veces. {@code nombre} y {@code telefonoContacto} solo hacen
     * falta la primera vez que reserva alguien sin cuenta (el teléfono, si escribe desde un @lid).
     */
    @Transactional
    public ResultadoSolicitud solicitar(RemitenteWhatsapp quien, String nombre, String telefonoContacto, long canchaId, Franja franja,
            String nombreEquipo, String claveIdempotencia) {
        verificarActivo();
        Titular titular = titularParaReservar(quien, nombre, telefonoContacto);
        return reservas.solicitarPorWhatsapp(titular, canchaId, franja.inicio(), franja.fin(), nombreEquipo, null, claveIdempotencia);
    }

    @Transactional(readOnly = true)
    public List<VistaReserva> misReservas(RemitenteWhatsapp quien) {
        verificarActivo();
        Titular titular = titularExistente(quien);
        return titular == null ? List.of() : reservas.proximasDe(titular, MAX_RESERVAS_LISTADAS);
    }

    @Transactional
    public VistaReserva cancelar(RemitenteWhatsapp quien, String codigo, String motivo) {
        verificarActivo();
        Titular titular = titularObligatorio(quien);
        return reservas.cancelar(reservas.idDeCodigo(codigo, titular), titular, motivo);
    }

    @Transactional(readOnly = true)
    public PrevisualizacionReprogramacion previsualizarReprogramacion(RemitenteWhatsapp quien, String codigo, Long canchaId, Franja franja) {
        verificarActivo();
        Titular titular = titularObligatorio(quien);
        return reservas.previsualizarReprogramacion(reservas.idDeCodigo(codigo, titular), titular, canchaId, franja.inicio(), franja.fin());
    }

    @Transactional
    public VistaReserva reprogramar(RemitenteWhatsapp quien, String codigo, Long canchaId, Franja franja, String motivo) {
        verificarActivo();
        Titular titular = titularObligatorio(quien);
        return reservas.reprogramar(reservas.idDeCodigo(codigo, titular), titular, canchaId, franja.inicio(), franja.fin(), motivo);
    }

    public void verificarActivo() {
        if (!configuracion.activo()) {
            throw new ExcepcionNegocio(CodigoError.SERVICIO_NO_DISPONIBLE,
                    "El bot de WhatsApp está en pausa. Puedes reservar en la web o comunicarte con el administrador.");
        }
    }

    private Identificacion identificacion(RemitenteWhatsapp quien) {
        return quien.porLid() ? verificacion.identificarPorLid(quien.lid()) : verificacion.identificar(quien.telefono());
    }

    private Optional<ClienteMostrador> sinCuentaExistente(RemitenteWhatsapp quien) {
        return quien.porLid() ? sinCuenta.buscarPorLid(quien.lid()) : sinCuenta.buscarPorTelefono(quien.telefono());
    }

    /** El titular para una reserva nueva; quien no tiene cuenta queda registrado la primera vez. */
    private Titular titularParaReservar(RemitenteWhatsapp quien, String nombre, String telefonoContacto) {
        Identificacion id = identificacion(quien);
        return switch (id.situacion()) {
            case CLIENTE -> Titular.cliente(id.usuarioId());
            case SIN_CUENTA -> {
                if (!configuracion.reservasSinCuenta()) {
                    throw new ExcepcionNegocio(CodigoError.ACCESO_DENEGADO,
                            "Para reservar por WhatsApp necesitas una cuenta en Goltime. Regístrate en la web y vincula este WhatsApp.");
                }
                ClienteMostrador cliente = sinCuentaExistente(quien).orElseGet(() -> quien.porLid()
                        ? sinCuenta.registrarConLid(nombre, telefonoContacto, quien.lid())
                        : sinCuenta.registrar(nombre, quien.telefono()));
                yield Titular.sinCuenta(cliente.id());
            }
            default -> throw sinTitular(id.situacion());
        };
    }

    /** El titular de las reservas de quien escribe, o {@code null} si nunca ha reservado sin cuenta. */
    private Titular titularExistente(RemitenteWhatsapp quien) {
        Identificacion id = identificacion(quien);
        return switch (id.situacion()) {
            case CLIENTE -> Titular.cliente(id.usuarioId());
            case SIN_CUENTA -> sinCuentaExistente(quien).map(c -> Titular.sinCuenta(c.id())).orElse(null);
            default -> throw sinTitular(id.situacion());
        };
    }

    private Titular titularObligatorio(RemitenteWhatsapp quien) {
        Titular titular = titularExistente(quien);
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
