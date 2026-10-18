package co.edu.usco.goltime.reservas.aplicacion;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.usco.goltime.auditoria.aplicacion.RegistroAuditoria;
import co.edu.usco.goltime.compartido.dominio.Actor;
import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.compartido.dominio.FormatoTexto;
import co.edu.usco.goltime.compartido.dominio.ZonaHoraria;
import co.edu.usco.goltime.notificaciones.aplicacion.Notificador;
import co.edu.usco.goltime.reservas.aplicacion.ServicioReservasAdmin.DatosReservaAdmin;
import co.edu.usco.goltime.reservas.aplicacion.Vistas.VistaReserva;
import co.edu.usco.goltime.sedes.aplicacion.ServicioCanchas;
import co.edu.usco.goltime.sedes.aplicacion.Vistas.VistaCancha;

/**
 * Reservas recurrentes: el mismo día y hora durante varias semanas, creadas por el administrador para un cliente. Primero se
 * previsualiza qué fechas se pueden reservar (horario, tarifas y ocupación de cada una); crear exige que todas estén libres o que el
 * administrador acepte omitir las que no. Cada fecha es una reserva normal (confirmada, con su precio y sus pagos).
 */
@Service
public class ServicioSeries {

    public static final int SEMANAS_MIN = 2;
    public static final int SEMANAS_MAX = 26;

    public enum EstadoOcurrencia {
        DISPONIBLE, OCUPADA, FUERA_DE_HORARIO
    }

    public record Ocurrencia(int numero, Instant inicio, Instant fin, EstadoOcurrencia estado, String motivo, int precio) {
    }

    /** Un cliente registrado ({@code clienteId}) o de mostrador (nombre y teléfono). */
    public record DatosSerie(long canchaId, Instant inicio, Instant fin, int semanas, Long clienteId, String mostradorNombre, String mostradorTelefono,
            String nombreEquipo, String notas) {
    }

    public record ResultadoSerie(long serieId, List<VistaReserva> reservas, List<Ocurrencia> omitidas) {
    }

    private final RepositorioSeries series;
    private final ServicioReservasAdmin reservasAdmin;
    private final ServicioReservas servicioReservas;
    private final RepositorioOcupaciones ocupaciones;
    private final ServicioCanchas canchas;
    private final CalculosReserva calculos;
    private final ConfiguracionReservas configuracion;
    private final Notificador notificador;
    private final RegistroAuditoria auditoria;
    private final Clock reloj;

    public ServicioSeries(RepositorioSeries series, ServicioReservasAdmin reservasAdmin, ServicioReservas servicioReservas, RepositorioOcupaciones ocupaciones,
            ServicioCanchas canchas, CalculosReserva calculos, ConfiguracionReservas configuracion, Notificador notificador, RegistroAuditoria auditoria,
            Clock reloj) {
        this.series = series;
        this.reservasAdmin = reservasAdmin;
        this.servicioReservas = servicioReservas;
        this.ocupaciones = ocupaciones;
        this.canchas = canchas;
        this.calculos = calculos;
        this.configuracion = configuracion;
        this.notificador = notificador;
        this.auditoria = auditoria;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public List<Ocurrencia> previsualizar(DatosSerie d) {
        validar(d);
        return ocurrencias(d);
    }

    @Transactional
    public ResultadoSerie crear(Actor admin, DatosSerie d, boolean omitirOcupadas) {
        validar(d);
        List<Ocurrencia> todas = ocurrencias(d);
        List<Ocurrencia> libres = todas.stream().filter(o -> o.estado() == EstadoOcurrencia.DISPONIBLE).toList();
        List<Ocurrencia> omitidas = todas.stream().filter(o -> o.estado() != EstadoOcurrencia.DISPONIBLE).toList();
        if (!omitidas.isEmpty() && !omitirOcupadas) {
            throw new ExcepcionNegocio(CodigoError.CONFLICTOS_SIN_CONFIRMAR,
                    omitidas.size() + " de " + todas.size() + " fechas no se pueden reservar. Revísalas y confirma para crear solo las disponibles.");
        }
        if (libres.isEmpty()) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "Ninguna de las fechas está disponible.");
        }
        LocalDateTime local = ZonaHoraria.aHoraLocal(d.inicio());
        long serieId = series.crear(d.canchaId(), local.getDayOfWeek().getValue(), local.getHour() * 60 + local.getMinute(),
                (int) Duration.between(d.inicio(), d.fin()).toMinutes(), d.semanas(), d.nombreEquipo(), admin.id());

        List<VistaReserva> creadas = new ArrayList<>();
        for (Ocurrencia o : libres) {
            VistaReserva v = reservasAdmin.crear(admin, new DatosReservaAdmin(d.canchaId(), o.inicio(), o.fin(), d.clienteId(), d.mostradorNombre(),
                    d.mostradorTelefono(), d.nombreEquipo(), d.notas()), false);
            series.asignarReserva(v.reserva().id(), serieId);
            creadas.add(v);
        }
        Long clienteId = creadas.get(0).reserva().clienteId();
        series.asignarCliente(serieId, clienteId, clienteId == null ? series.mostradorDeReserva(creadas.get(0).reserva().id()) : null);
        auditoria.registrar("CREAR_SERIE_RESERVAS", "series_reservas", serieId,
                Map.of("canchaId", d.canchaId(), "creadas", creadas.size(), "omitidas", omitidas.size(), "semanas", d.semanas()), null);
        if (clienteId != null) {
            notificador.notificar(clienteId, "RESERVA_CREADA_ADMIN", "Te creamos una reserva recurrente",
                    "El administrador registró %d reservas semanales desde el %s en %s.".formatted(creadas.size(), FormatoTexto.fechaHora(libres.get(0).inicio()),
                            creadas.get(0).relacionados().canchaNombre()), "reserva", creadas.get(0).reserva().id());
        }
        return new ResultadoSerie(serieId, creadas, omitidas);
    }

    /** Cancela todas las reservas futuras que siguen activas de la serie (las ya jugadas o canceladas no se tocan). */
    @Transactional
    public int cancelar(Actor admin, long serieId, String motivo) {
        series.buscar(serieId).orElseThrow(() -> new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "La serie no existe."));
        if (motivo == null || motivo.isBlank()) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Indica el motivo de la cancelación.");
        }
        List<Long> ids = series.idsFuturasActivas(serieId, reloj.instant());
        for (long id : ids) {
            servicioReservas.cancelarPorAdmin(id, admin, "Serie cancelada: " + motivo.trim());
        }
        auditoria.registrar("CANCELAR_SERIE_RESERVAS", "series_reservas", serieId, Map.of("canceladas", ids.size()), motivo.trim());
        return ids.size();
    }

    @Transactional(readOnly = true)
    public List<RepositorioSeries.ResumenSerie> listar() {
        return series.listar(reloj.instant());
    }

    // ----- Interno -----

    private void validar(DatosSerie d) {
        if (d.inicio() == null || d.fin() == null || !d.fin().isAfter(d.inicio())) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Indica la hora de inicio y de fin de la primera reserva.");
        }
        if (d.semanas() < SEMANAS_MIN || d.semanas() > SEMANAS_MAX) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "La serie debe durar entre " + SEMANAS_MIN + " y " + SEMANAS_MAX + " semanas.");
        }
        if (!ZonaHoraria.fechaLocal(d.inicio()).equals(ZonaHoraria.fechaLocal(d.fin().minusSeconds(1)))) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Cada reserva de la serie debe empezar y terminar el mismo día.");
        }
    }

    /** Una por semana, a la misma hora de pared en Bogotá. */
    private List<Ocurrencia> ocurrencias(DatosSerie d) {
        VistaCancha cancha = canchas.detallePublico(d.canchaId());
        var politica = configuracion.politica().paraAdministrador();
        Instant ahora = reloj.instant();
        LocalDate fecha = ZonaHoraria.fechaLocal(d.inicio());
        LocalTime desde = ZonaHoraria.aHoraLocal(d.inicio()).toLocalTime();
        Duration duracion = Duration.between(d.inicio(), d.fin());
        List<Ocurrencia> lista = new ArrayList<>();
        for (int i = 0; i < d.semanas(); i++) {
            Instant inicio = ZonaHoraria.aInstante(fecha.plusWeeks(i), desde);
            Instant fin = inicio.plus(duracion);
            try {
                calculos.validarFranja(cancha, inicio, fin, ahora, politica);
            } catch (ExcepcionNegocio ex) {
                lista.add(new Ocurrencia(i + 1, inicio, fin, EstadoOcurrencia.FUERA_DE_HORARIO, ex.getMessage(), 0));
                continue;
            }
            if (ocupaciones.hayOcupacion(CalculosReserva.idsQueOcupa(cancha), inicio, fin, null)) {
                lista.add(new Ocurrencia(i + 1, inicio, fin, EstadoOcurrencia.OCUPADA, "La cancha ya está ocupada en ese horario.", 0));
                continue;
            }
            lista.add(new Ocurrencia(i + 1, inicio, fin, EstadoOcurrencia.DISPONIBLE, null, calculos.calcularPrecio(cancha, inicio, fin).finalAPagar()));
        }
        return lista;
    }
}
