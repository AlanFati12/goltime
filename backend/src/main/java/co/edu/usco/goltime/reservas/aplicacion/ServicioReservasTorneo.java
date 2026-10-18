package co.edu.usco.goltime.reservas.aplicacion;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.usco.goltime.compartido.dominio.Actor;
import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.compartido.dominio.Rol;
import co.edu.usco.goltime.compartido.dominio.ZonaHoraria;
import co.edu.usco.goltime.reservas.aplicacion.ServicioBloqueos.ConflictoReserva;
import co.edu.usco.goltime.reservas.dominio.Bloqueo.Intervalo;
import co.edu.usco.goltime.reservas.dominio.OrigenReserva;
import co.edu.usco.goltime.reservas.dominio.Reserva;
import co.edu.usco.goltime.sedes.aplicacion.ServicioCanchas;
import co.edu.usco.goltime.sedes.aplicacion.Vistas.VistaCancha;

/**
 * Reserva de canchas para los partidos de torneo. Usa la misma tabla de ocupaciones que las reservas de clientes,
 * así que un partido bloquea la cancha (y las canchas que la componen) y no puede solaparse con nada.
 * <p>
 * Si el horario choca con reservas de clientes sigue el flujo de los bloqueos: primero se previsualizan, luego
 * el administrador confirma enviando la huella de esa lista y solo entonces se cancelan. Chocar con otro partido
 * o con un bloqueo nunca se cancela: es un error.
 */
@Service
public class ServicioReservasTorneo {

    private final RepositorioReservas reservas;
    private final RepositorioOcupaciones ocupaciones;
    private final RepositorioHistorial historial;
    private final ServicioReservas servicioReservas;
    private final ServicioCanchas canchas;
    private final CalculosReserva calculos;
    private final ConfiguracionReservas configuracion;
    private final Clock reloj;

    public ServicioReservasTorneo(RepositorioReservas reservas, RepositorioOcupaciones ocupaciones, RepositorioHistorial historial,
            ServicioReservas servicioReservas, ServicioCanchas canchas, CalculosReserva calculos, ConfiguracionReservas configuracion, Clock reloj) {
        this.reservas = reservas;
        this.ocupaciones = ocupaciones;
        this.historial = historial;
        this.servicioReservas = servicioReservas;
        this.canchas = canchas;
        this.calculos = calculos;
        this.configuracion = configuracion;
        this.reloj = reloj;
    }

    public record ConflictosHorario(List<ConflictoReserva> conflictos, String huella) {
        public boolean requiereConfirmacion() {
            return !conflictos.isEmpty();
        }
    }

    /**
     * Reservas de clientes que se cancelarían al ocupar la cancha en ese horario. {@code ignorarReservaId} es la reserva
     * que el propio partido ya tiene (al reprogramarlo). Lanza error si el horario no es válido o está ocupado por otro
     * partido o por un bloqueo.
     */
    @Transactional(readOnly = true)
    public ConflictosHorario previsualizar(long canchaId, Instant inicio, Instant fin, Long ignorarReservaId) {
        VistaCancha cancha = canchas.detallePublico(canchaId);
        calculos.validarFranja(cancha, inicio, fin, reloj.instant(), configuracion.politica().paraAdministrador());
        List<Long> afectadas = new ArrayList<>(CalculosReserva.idsQueOcupa(cancha));
        List<Long> ids = ocupaciones.reservasActivasEnIntervalos(afectadas, List.of(new Intervalo(inicio, fin))).stream()
                .filter(id -> ignorarReservaId == null || id != ignorarReservaId).toList();

        Map<Long, Relacionados> relacionados = reservas.relacionados(ids);
        List<ConflictoReserva> conflictos = new ArrayList<>();
        for (long id : ids) {
            Reserva r = reservas.buscarPorId(id).orElseThrow();
            if (r.origen() == OrigenReserva.TORNEO) {
                throw new ExcepcionNegocio(CodigoError.RESERVA_SOLAPADA, "La cancha ya está ocupada por otro partido de torneo en ese horario.");
            }
            Relacionados rel = relacionados.get(id);
            conflictos.add(new ConflictoReserva(id, r.codigo(), rel.clienteNombre(), rel.canchaNombre(), r.inicio(), r.fin(),
                    r.precio().finalAPagar(), r.montoPagado() > 0, r.estadoPago().name(), r.estado().name()));
        }
        conflictos.sort(Comparator.comparing(ConflictoReserva::inicio).thenComparing(ConflictoReserva::reservaId));
        if (conflictos.isEmpty() && ocupaciones.hayOcupacion(afectadas, inicio, fin, ignorarReservaId)) {
            throw new ExcepcionNegocio(CodigoError.RESERVA_SOLAPADA, "La cancha está bloqueada en ese horario.");
        }
        return new ConflictosHorario(conflictos, ServicioBloqueos.huella(conflictos));
    }

    /**
     * Reserva la cancha para el partido y devuelve el id de la reserva. Con reservas de clientes en conflicto exige la
     * huella confirmada y las cancela primero, en la misma transacción.
     */
    @Transactional
    public long reservar(Actor admin, long canchaId, Instant inicio, Instant fin, String descripcion, String huellaConfirmada, Long ignorarReservaId) {
        ConflictosHorario previa = previsualizar(canchaId, inicio, fin, ignorarReservaId);
        if (previa.requiereConfirmacion()) {
            if (huellaConfirmada == null || huellaConfirmada.isBlank()) {
                throw new ExcepcionNegocio(CodigoError.CONFLICTOS_SIN_CONFIRMAR,
                        "El partido choca con " + previa.conflictos().size() + " reservas. Revisa la lista y confirma para cancelarlas.");
            }
            if (!previa.huella().equals(huellaConfirmada)) {
                throw new ExcepcionNegocio(CodigoError.CONFLICTOS_SIN_CONFIRMAR,
                        "Las reservas afectadas cambiaron desde que las revisaste (ahora son " + previa.conflictos().size() + "). Revisa la lista de nuevo.");
            }
            for (ConflictoReserva c : previa.conflictos()) {
                servicioReservas.cancelarPorAdmin(c.reservaId(), admin, "Partido de torneo: " + descripcion);
            }
        }
        Instant ahora = reloj.instant();
        VistaCancha cancha = canchas.detallePublico(canchaId);
        Reserva nueva = Reserva.crearParaTorneo(reservas.siguienteCodigo(ZonaHoraria.fechaLocal(ahora).getYear()), canchaId, inicio, fin, descripcion,
                admin.id(), ahora);
        Reserva guardada = reservas.guardar(nueva);
        ocupaciones.ocupar(guardada.id(), CalculosReserva.idsQueOcupa(cancha), inicio, fin);
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("canchaId", canchaId);
        datos.put("inicio", inicio.toString());
        datos.put("fin", fin.toString());
        datos.put("origen", OrigenReserva.TORNEO.name());
        historial.registrar(new EntradaHistorial(null, guardada.id(), "CREADA", admin.id(), null, Rol.ADMIN, descripcion, null, datos, ahora));
        return guardada.id();
    }

    /** Cancela la reserva del partido y libera la cancha. Si el horario ya empezó no hay nada que liberar. */
    @Transactional
    public void liberar(Actor admin, long reservaId, String motivo) {
        Reserva reserva = reservas.buscarPorId(reservaId).orElse(null);
        if (reserva == null || !reserva.estado().ocupaHorario() || !reserva.inicio().isAfter(reloj.instant())) {
            return;
        }
        servicioReservas.cancelarPorAdmin(reservaId, admin, motivo);
    }
}
