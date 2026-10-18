package co.edu.usco.goltime.reservas.aplicacion;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.compartido.dominio.FormatoTexto;
import co.edu.usco.goltime.notificaciones.aplicacion.Notificador;
import co.edu.usco.goltime.reservas.aplicacion.RepositorioListaEspera.Espera;
import co.edu.usco.goltime.reservas.aplicacion.RepositorioListaEspera.Estado;
import co.edu.usco.goltime.reservas.dominio.Reserva;
import co.edu.usco.goltime.reservas.dominio.ReservaCreadaEvento;
import co.edu.usco.goltime.reservas.dominio.ReservaLiberadaEvento;
import co.edu.usco.goltime.sedes.aplicacion.ServicioCanchas;
import co.edu.usco.goltime.sedes.aplicacion.Vistas.VistaCancha;

/**
 * Lista de espera de horarios ocupados. El cliente pide que le avisen; cuando una reserva que ocupaba ese horario se cancela, se rechaza o
 * expira y el horario realmente queda libre, todos los que esperan reciben el aviso (el primero en reservar se lo queda: la reserva sigue
 * necesitando aprobación y el anti-solapamiento de siempre).
 */
@Service
public class ServicioListaEspera {

    public static final int MAXIMO_ACTIVAS_POR_CLIENTE = 5;

    private final RepositorioListaEspera espera;
    private final RepositorioReservas reservas;
    private final RepositorioOcupaciones ocupaciones;
    private final ServicioCanchas canchas;
    private final CalculosReserva calculos;
    private final ConfiguracionReservas configuracion;
    private final Notificador notificador;
    private final Clock reloj;

    public ServicioListaEspera(RepositorioListaEspera espera, RepositorioReservas reservas, RepositorioOcupaciones ocupaciones, ServicioCanchas canchas,
            CalculosReserva calculos, ConfiguracionReservas configuracion, Notificador notificador, Clock reloj) {
        this.espera = espera;
        this.reservas = reservas;
        this.ocupaciones = ocupaciones;
        this.canchas = canchas;
        this.calculos = calculos;
        this.configuracion = configuracion;
        this.notificador = notificador;
        this.reloj = reloj;
    }

    @Transactional
    public Espera unirse(long clienteId, long canchaId, Instant inicio, Instant fin) {
        Instant ahora = reloj.instant();
        VistaCancha cancha = canchas.detallePublico(canchaId);
        calculos.validarFranja(cancha, inicio, fin, ahora, configuracion.politica());
        if (!ocupaciones.hayOcupacion(CalculosReserva.idsQueOcupa(cancha), inicio, fin, null)) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "Ese horario está libre: puedes reservarlo ahora.");
        }
        if (espera.existeActiva(clienteId, canchaId, inicio, fin)) {
            throw new ExcepcionNegocio(CodigoError.YA_EXISTE, "Ya estás en la lista de espera de ese horario.");
        }
        if (espera.contarActivas(clienteId, ahora) >= MAXIMO_ACTIVAS_POR_CLIENTE) {
            throw new ExcepcionNegocio(CodigoError.LIMITE_DE_RESERVAS, "Puedes esperar como máximo " + MAXIMO_ACTIVAS_POR_CLIENTE + " horarios a la vez.");
        }
        return espera.crear(clienteId, canchaId, inicio, fin);
    }

    @Transactional(readOnly = true)
    public List<Espera> deCliente(long clienteId) {
        return espera.activasDeCliente(clienteId, reloj.instant());
    }

    /** Un cliente solo cancela las suyas; las ajenas responden como si no existieran. */
    @Transactional
    public void salir(long clienteId, long id) {
        Espera e = espera.buscar(id).filter(x -> x.clienteId() == clienteId).orElseThrow(
                () -> new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "La espera no existe."));
        if (e.estado() == Estado.CUMPLIDA || e.estado() == Estado.CANCELADA) {
            return;
        }
        espera.cambiarEstado(id, Estado.CANCELADA, null);
    }

    /** Avisa a quienes esperaban un horario que acaba de quedar libre. */
    @EventListener
    public void alLiberarseUnaReserva(ReservaLiberadaEvento evento) {
        Reserva r = reservas.buscarPorId(evento.reservaId()).orElse(null);
        if (r == null) {
            return;
        }
        Instant ahora = reloj.instant();
        VistaCancha cancha = canchas.detalleAdmin(r.canchaId());
        Set<Long> liberadas = CalculosReserva.idsQueOcupa(cancha);
        for (Espera e : espera.esperandoEn(liberadas, r.inicio(), r.fin(), ahora)) {
            if (e.clienteId() == (evento.clienteId() == null ? -1 : evento.clienteId())) {
                continue; // quien canceló no necesita que le avisen de su propio horario
            }
            VistaCancha suya = canchas.detalleAdmin(e.canchaId());
            if (ocupaciones.hayOcupacion(CalculosReserva.idsQueOcupa(suya), e.inicio(), e.fin(), null)) {
                continue; // sigue ocupado por otra reserva o bloqueo
            }
            espera.cambiarEstado(e.id(), Estado.NOTIFICADA, ahora);
            notificador.notificar(e.clienteId(), "ESPERA_LIBERADA", "¡Se liberó un horario!",
                    "%s en %s (%s) quedó libre. Solicítalo ya: otros también esperaban.".formatted(FormatoTexto.fechaHora(e.inicio()), e.canchaNombre(), e.sedeNombre()),
                    "cancha", e.canchaId());
        }
    }

    /** Si el cliente reserva lo que esperaba (o algo que se le solapa), su espera se da por cumplida. */
    @EventListener
    public void alSolicitarseUnaReserva(ReservaCreadaEvento evento) {
        VistaCancha cancha = canchas.detalleAdmin(evento.canchaId());
        espera.cumplir(evento.clienteId(), CalculosReserva.idsQueOcupa(cancha), evento.inicio(), evento.fin());
    }
}
