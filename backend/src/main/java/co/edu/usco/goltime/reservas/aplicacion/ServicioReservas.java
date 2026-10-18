package co.edu.usco.goltime.reservas.aplicacion;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.usco.goltime.compartido.dominio.Actor;
import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.compartido.dominio.FormatoTexto;
import co.edu.usco.goltime.compartido.dominio.Pagina;
import co.edu.usco.goltime.compartido.dominio.Rol;
import co.edu.usco.goltime.compartido.dominio.SolicitudPagina;
import co.edu.usco.goltime.compartido.dominio.ZonaHoraria;
import co.edu.usco.goltime.notificaciones.aplicacion.Notificador;
import co.edu.usco.goltime.reservas.aplicacion.RepositorioReservas.FiltroReservasAdmin;
import co.edu.usco.goltime.reservas.aplicacion.RepositorioReservas.PestanaReservas;
import co.edu.usco.goltime.reservas.aplicacion.Vistas.PrevisualizacionReprogramacion;
import co.edu.usco.goltime.reservas.aplicacion.Vistas.ResultadoSolicitud;
import co.edu.usco.goltime.reservas.aplicacion.Vistas.VistaReserva;
import co.edu.usco.goltime.reservas.dominio.EstadoReserva;
import co.edu.usco.goltime.reservas.dominio.PoliticaReservas;
import co.edu.usco.goltime.reservas.dominio.Precio;
import co.edu.usco.goltime.reservas.dominio.Reserva;
import co.edu.usco.goltime.reservas.dominio.ReservaCompletadaEvento;
import co.edu.usco.goltime.reservas.dominio.ValidadorFranja;
import co.edu.usco.goltime.reservas.dominio.ValidadorFranja.ReglasCancha;
import co.edu.usco.goltime.sedes.aplicacion.ServicioCanchas;
import co.edu.usco.goltime.sedes.aplicacion.ServicioFestivos;
import co.edu.usco.goltime.sedes.aplicacion.Vistas.VistaCancha;
import co.edu.usco.goltime.sedes.dominio.CalculadoraPrecio;
import co.edu.usco.goltime.sedes.dominio.HorarioDia;

/**
 * Casos de uso de reservas: solicitar, consultar, cancelar, reprogramar, aprobar/rechazar y los pasos
 * automáticos (expirar, completar, recordar). Cada acción deja constancia en el historial de la reserva
 * (quién, cuándo, por qué, antes y después) y avisa a quien corresponda.
 *
 * <p>La garantía de que dos reservas no se solapen no está aquí sino en la base de datos
 * ({@link RepositorioOcupaciones}); este servicio solo traduce el choque a un error de negocio.
 */
@Service
public class ServicioReservas {

    private final RepositorioReservas reservas;
    private final RepositorioOcupaciones ocupaciones;
    private final RepositorioHistorial historial;
    private final ServicioCanchas canchas;
    private final ServicioFestivos festivos;
    private final ConfiguracionReservas configuracion;
    private final Notificador notificador;
    private final ApplicationEventPublisher eventos;
    private final Clock reloj;

    public ServicioReservas(RepositorioReservas reservas, RepositorioOcupaciones ocupaciones, RepositorioHistorial historial,
            ServicioCanchas canchas, ServicioFestivos festivos, ConfiguracionReservas configuracion, Notificador notificador,
            ApplicationEventPublisher eventos, Clock reloj) {
        this.reservas = reservas;
        this.ocupaciones = ocupaciones;
        this.historial = historial;
        this.canchas = canchas;
        this.festivos = festivos;
        this.configuracion = configuracion;
        this.notificador = notificador;
        this.eventos = eventos;
        this.reloj = reloj;
    }

    // ===================================================================================
    // Cliente
    // ===================================================================================

    /** Solicitud de reserva de un cliente. Con la misma clave de idempotencia devuelve la ya creada. */
    @Transactional
    public ResultadoSolicitud solicitar(long clienteId, long canchaId, Instant inicio, Instant fin, String nombreEquipo,
            String notas, String claveIdempotencia) {
        String clave = claveIdempotencia == null || claveIdempotencia.isBlank() ? null : claveIdempotencia.trim();
        if (clave != null) {
            reservas.bloquearPorClave(clienteId + ":" + clave);
            var existente = reservas.buscarPorClaveIdempotencia(clienteId, clave);
            if (existente.isPresent()) {
                return new ResultadoSolicitud(vista(existente.get(), true), false);
            }
        }
        Instant ahora = reloj.instant();
        PoliticaReservas politica = configuracion.politica();
        VistaCancha cancha = canchas.detallePublico(canchaId);
        validarFranja(cancha, inicio, fin, ahora, politica);
        if (reservas.contarActivasFuturas(clienteId, ahora) >= politica.maxReservasActivasCliente()) {
            throw new ExcepcionNegocio(CodigoError.LIMITE_DE_RESERVAS,
                    "Ya tienes " + politica.maxReservasActivasCliente() + " reservas activas. Cancela alguna o espera a que se juegue.");
        }
        Precio precio = calcularPrecio(cancha, inicio, fin);
        int anio = ZonaHoraria.fechaLocal(ahora).getYear();
        Reserva nueva = Reserva.solicitar(reservas.siguienteCodigo(anio), clienteId, canchaId, inicio, fin, precio, nombreEquipo,
                notas, politica.calcularExpiracion(ahora, inicio), clave);
        Reserva guardada = reservas.guardar(nueva);
        ocupaciones.ocupar(guardada.id(), idsQueOcupa(cancha), inicio, fin);

        Actor actor = new Actor(clienteId, Rol.CLIENTE);
        registrar(guardada.id(), "CREADA", actor, null, null, datosDe(guardada));
        notificador.notificarAdministradores("RESERVA_SOLICITADA", "Nueva solicitud de reserva",
                "%s pidió %s el %s. Responde antes de que expire.".formatted(guardada.codigo(), cancha.cancha().nombre(),
                        FormatoTexto.fechaHora(inicio)), "reserva", guardada.id());
        return new ResultadoSolicitud(vista(guardada, true), true);
    }

    @Transactional(readOnly = true)
    public Pagina<VistaReserva> deCliente(long clienteId, PestanaReservas pestana, SolicitudPagina pagina) {
        Instant ahora = reloj.instant();
        Pagina<Reserva> filas = reservas.deCliente(clienteId, pestana, ahora, pagina);
        return conVistas(filas, ahora);
    }

    /** Detalle con historial. Un cliente solo ve las suyas: las ajenas responden como si no existieran (evita IDOR). */
    @Transactional(readOnly = true)
    public VistaReserva detalle(long id, Actor actor) {
        return vista(cargarAutorizada(id, actor), true);
    }

    @Transactional
    public VistaReserva cancelar(long id, Actor actor, String motivo) {
        Reserva reserva = cargarAutorizada(id, actor);
        Instant ahora = reloj.instant();
        EstadoReserva antes = reserva.estado();
        reserva.cancelar(actor, motivo, ahora, configuracion.politica());
        Reserva guardada = reservas.guardar(reserva);
        ocupaciones.liberar(guardada.id());

        Map<String, Object> despues = new LinkedHashMap<>();
        despues.put("estado", guardada.estado().name());
        despues.put("cancelacionTardia", guardada.cancelacionTardia());
        registrar(guardada.id(), "CANCELADA", actor, guardada.motivoCancelacion(), Map.of("estado", antes.name()), despues);

        if (actor.rol() == Rol.CLIENTE) {
            if (antes == EstadoReserva.CONFIRMADA) {
                notificador.notificarAdministradores("RESERVA_CANCELADA", "Reserva cancelada por el cliente",
                        "%s del %s fue cancelada%s.".formatted(guardada.codigo(), FormatoTexto.fechaHora(guardada.inicio()),
                                guardada.cancelacionTardia() ? " (cancelación tardía)" : ""), "reserva", guardada.id());
            }
        } else {
            notificarCliente(guardada, "RESERVA_CANCELADA_ADMIN", "Tu reserva fue cancelada",
                    "El administrador canceló la reserva %s del %s. Motivo: %s".formatted(guardada.codigo(),
                            FormatoTexto.fechaHora(guardada.inicio()), guardada.motivoCancelacion()));
        }
        return vista(guardada, true);
    }

    /** Muestra el efecto de reprogramar (precio nuevo, diferencia y si el horario está libre) sin cambiar nada. */
    @Transactional(readOnly = true)
    public PrevisualizacionReprogramacion previsualizarReprogramacion(long id, Actor actor, Long nuevaCanchaId, Instant inicio, Instant fin) {
        Reserva reserva = cargarAutorizada(id, actor);
        Instant ahora = reloj.instant();
        PoliticaReservas politica = configuracion.politica();
        verificarPuedeReprogramar(reserva, actor, ahora, politica);
        VistaCancha destino = canchas.detallePublico(nuevaCanchaId != null ? nuevaCanchaId : reserva.canchaId());
        validarFranja(destino, inicio, fin, ahora, politica);
        Precio nuevo = calcularPrecio(destino, inicio, fin);
        boolean libre = !ocupaciones.hayOcupacion(idsQueOcupa(destino), inicio, fin, reserva.id());
        return new PrevisualizacionReprogramacion(libre, reserva.precio().finalAPagar(), nuevo.finalAPagar(),
                nuevo.finalAPagar() - reserva.precio().finalAPagar());
    }

    @Transactional
    public VistaReserva reprogramar(long id, Actor actor, Long nuevaCanchaId, Instant inicio, Instant fin) {
        Reserva reserva = cargarAutorizada(id, actor);
        Instant ahora = reloj.instant();
        PoliticaReservas politica = configuracion.politica();
        VistaCancha destino = canchas.detallePublico(nuevaCanchaId != null ? nuevaCanchaId : reserva.canchaId());
        validarFranja(destino, inicio, fin, ahora, politica);
        Precio nuevo = calcularPrecio(destino, inicio, fin);

        Map<String, Object> antes = datosDe(reserva);
        reserva.reprogramar(actor, destino.cancha().id(), inicio, fin, nuevo, ahora, politica, politica.calcularExpiracion(ahora, inicio));
        Reserva guardada = reservas.guardar(reserva);
        ocupaciones.liberar(guardada.id());
        ocupaciones.ocupar(guardada.id(), idsQueOcupa(destino), inicio, fin);
        registrar(guardada.id(), "REPROGRAMADA", actor, null, antes, datosDe(guardada));

        if (actor.rol() == Rol.CLIENTE) {
            notificador.notificarAdministradores("RESERVA_REPROGRAMADA", "Reserva reprogramada por el cliente",
                    "%s pasó al %s en %s.".formatted(guardada.codigo(), FormatoTexto.fechaHora(inicio), destino.cancha().nombre()),
                    "reserva", guardada.id());
        } else {
            notificarCliente(guardada, "RESERVA_REPROGRAMADA_ADMIN", "Tu reserva fue reprogramada",
                    "El administrador movió la reserva %s al %s en %s.".formatted(guardada.codigo(), FormatoTexto.fechaHora(inicio),
                            destino.cancha().nombre()));
        }
        return vista(guardada, true);
    }

    // ===================================================================================
    // Administración
    // ===================================================================================

    @Transactional(readOnly = true)
    public Pagina<VistaReserva> listarAdmin(FiltroReservasAdmin filtro, SolicitudPagina pagina) {
        Instant ahora = reloj.instant();
        return conVistas(reservas.deAdmin(filtro, pagina), ahora);
    }

    @Transactional
    public VistaReserva aprobar(long id, long adminId) {
        Reserva reserva = cargar(id);
        EstadoReserva antes = reserva.estado();
        reserva.aprobar(adminId, reloj.instant());
        Reserva guardada = reservas.guardar(reserva);
        registrar(id, "APROBADA", new Actor(adminId, Rol.ADMIN), null, Map.of("estado", antes.name()), Map.of("estado", guardada.estado().name()));
        notificarCliente(guardada, "RESERVA_CONFIRMADA", "¡Reserva confirmada!",
                "Tu reserva %s para el %s fue aprobada.".formatted(guardada.codigo(), FormatoTexto.fechaHora(guardada.inicio())));
        return vista(guardada, true);
    }

    @Transactional
    public VistaReserva rechazar(long id, long adminId, String motivo) {
        Reserva reserva = cargar(id);
        EstadoReserva antes = reserva.estado();
        reserva.rechazar(adminId, motivo, reloj.instant());
        Reserva guardada = reservas.guardar(reserva);
        ocupaciones.liberar(id);
        registrar(id, "RECHAZADA", new Actor(adminId, Rol.ADMIN), guardada.motivoRechazo(), Map.of("estado", antes.name()),
                Map.of("estado", guardada.estado().name()));
        notificarCliente(guardada, "RESERVA_RECHAZADA", "Tu solicitud no fue aprobada",
                "La reserva %s del %s no fue aprobada. Motivo: %s".formatted(guardada.codigo(), FormatoTexto.fechaHora(guardada.inicio()),
                        guardada.motivoRechazo()));
        return vista(guardada, true);
    }

    /** Cancelación hecha por el administrador (también al desactivar una cancha, sede o cliente). */
    @Transactional
    public VistaReserva cancelarPorAdmin(long id, Actor admin, String motivo) {
        return cancelar(id, admin, motivo);
    }

    /** Cuántas reservas futuras activas ocupan alguna de las canchas. */
    @Transactional(readOnly = true)
    public List<Long> idsFuturasEnCanchas(Collection<Long> canchaIds) {
        return ocupaciones.reservasFuturasActivasEnCanchas(canchaIds, reloj.instant());
    }

    @Transactional(readOnly = true)
    public List<Long> idsFuturasEnSede(long sedeId) {
        return ocupaciones.reservasFuturasActivasEnSede(sedeId, reloj.instant());
    }

    @Transactional(readOnly = true)
    public List<Long> idsFuturasDeCliente(long clienteId) {
        return reservas.idsFuturasActivasDeCliente(clienteId, reloj.instant());
    }

    // ===================================================================================
    // Pasos automáticos (los invoca TareasReservas, cada uno en su propia transacción)
    // ===================================================================================

    @Transactional
    public void expirar(long id) {
        Reserva reserva = cargar(id);
        reserva.expirar(reloj.instant());
        Reserva guardada = reservas.guardar(reserva);
        ocupaciones.liberar(id);
        registrar(id, "EXPIRADA", Actor.sistema(), "El administrador no respondió a tiempo", Map.of("estado", "PENDIENTE"), Map.of("estado", "EXPIRADA"));
        notificarCliente(guardada, "RESERVA_EXPIRADA", "Tu solicitud expiró",
                "La reserva %s del %s no fue respondida a tiempo y se liberó el horario. Puedes volver a solicitarla.".formatted(
                        guardada.codigo(), FormatoTexto.fechaHora(guardada.inicio())));
    }

    @Transactional
    public void completar(long id) {
        Reserva reserva = cargar(id);
        reserva.completar(reloj.instant());
        Reserva guardada = reservas.guardar(reserva);
        registrar(id, "COMPLETADA", Actor.sistema(), null, Map.of("estado", "CONFIRMADA"), Map.of("estado", "COMPLETADA"));
        eventos.publishEvent(new ReservaCompletadaEvento(id, guardada.clienteId(), guardada.canchaId(), guardada.duracionMin()));
    }

    @Transactional
    public void recordar(long id) {
        Reserva reserva = cargar(id);
        Instant ahora = reloj.instant();
        reserva.registrarRecordatorio(ahora);
        Reserva guardada = reservas.guardar(reserva);
        notificarCliente(guardada, "RESERVA_RECORDATORIO", "Tu partido se acerca",
                "Recuerda tu reserva %s: %s.".formatted(guardada.codigo(), FormatoTexto.fechaHora(guardada.inicio())));
    }

    // ===================================================================================
    // Apoyo
    // ===================================================================================

    private Reserva cargar(long id) {
        return reservas.buscarPorId(id).orElseThrow(() -> new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "La reserva no existe."));
    }

    private Reserva cargarAutorizada(long id, Actor actor) {
        Reserva reserva = cargar(id);
        if (actor.rol() == Rol.CLIENTE && !reserva.esDe(actor.id())) {
            throw new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "La reserva no existe.");
        }
        return reserva;
    }

    private void verificarPuedeReprogramar(Reserva reserva, Actor actor, Instant ahora, PoliticaReservas politica) {
        if (!reserva.estado().ocupaHorario() || !reserva.inicio().isAfter(ahora)) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "Esta reserva ya no se puede reprogramar.");
        }
        if (actor.rol() == Rol.CLIENTE) {
            if (reserva.vecesReprogramada() >= politica.maxReprogramaciones()) {
                throw new ExcepcionNegocio(CodigoError.REPROGRAMACION_NO_PERMITIDA,
                        "Alcanzaste el máximo de " + politica.maxReprogramaciones() + " reprogramaciones para esta reserva.");
            }
            if (!politica.conAntelacionSuficiente(ahora, reserva.inicio())) {
                throw new ExcepcionNegocio(CodigoError.REPROGRAMACION_NO_PERMITIDA,
                        "Solo puedes reprogramar con al menos " + politica.horasMinCancelacion() + " horas de antelación.");
            }
        }
    }

    private void validarFranja(VistaCancha cancha, Instant inicio, Instant fin, Instant ahora, PoliticaReservas politica) {
        if (inicio == null || fin == null) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Indica la hora de inicio y de fin.");
        }
        HorarioDia horario = cancha.horarioEfectivo().get(ZonaHoraria.aHoraLocal(inicio).getDayOfWeek().getValue() - 1);
        ValidadorFranja.validar(horario, new ReglasCancha(cancha.cancha().duracionBloqueMin(), cancha.cancha().duracionMinMin(),
                cancha.cancha().duracionMaxMin()), inicio, fin, ahora, politica);
    }

    private Precio calcularPrecio(VistaCancha cancha, Instant inicio, Instant fin) {
        LocalDate desde = ZonaHoraria.fechaLocal(inicio);
        Set<LocalDate> festivosDelRango = festivos.fechasEntre(desde, ZonaHoraria.fechaLocal(fin));
        return Precio.sinDescuento(CalculadoraPrecio.calcular(cancha.cancha().precioBaseHora(), cancha.tarifas(), festivosDelRango, inicio, fin).total());
    }

    /** La cancha reservada y, si es compuesta, todas sus componentes. */
    private static Set<Long> idsQueOcupa(VistaCancha cancha) {
        Set<Long> ids = new LinkedHashSet<>();
        ids.add(cancha.cancha().id());
        ids.addAll(cancha.cancha().componentes());
        return ids;
    }

    private static Map<String, Object> datosDe(Reserva r) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("canchaId", r.canchaId());
        datos.put("inicio", r.inicio().toString());
        datos.put("fin", r.fin().toString());
        datos.put("precioFinal", r.precio().finalAPagar());
        datos.put("estado", r.estado().name());
        return datos;
    }

    private void registrar(long reservaId, String accion, Actor actor, String motivo, Map<String, Object> antes, Map<String, Object> despues) {
        historial.registrar(new EntradaHistorial(null, reservaId, accion, actor.id(), null, actor.rol(), motivo, antes, despues, reloj.instant()));
    }

    private void notificarCliente(Reserva r, String tipo, String titulo, String mensaje) {
        if (r.clienteId() != null) {
            notificador.notificar(r.clienteId(), tipo, titulo, mensaje, "reserva", r.id());
        }
    }

    private Pagina<VistaReserva> conVistas(Pagina<Reserva> filas, Instant ahora) {
        Map<Long, Relacionados> relacionados = reservas.relacionados(filas.contenido().stream().map(Reserva::id).toList());
        PoliticaReservas politica = configuracion.politica();
        List<VistaReserva> vistas = new ArrayList<>();
        for (Reserva r : filas.contenido()) {
            vistas.add(construirVista(r, relacionados.get(r.id()), politica, ahora, null));
        }
        return new Pagina<>(vistas, filas.pagina(), filas.tamano(), filas.totalElementos(), filas.totalPaginas());
    }

    private VistaReserva vista(Reserva r, boolean conHistorial) {
        Relacionados rel = reservas.relacionados(List.of(r.id())).get(r.id());
        return construirVista(r, rel, configuracion.politica(), reloj.instant(), conHistorial ? historial.deReserva(r.id()) : null);
    }

    private static VistaReserva construirVista(Reserva r, Relacionados rel, PoliticaReservas politica, Instant ahora, List<EntradaHistorial> historial) {
        return new VistaReserva(r, rel, r.puedeCancelar(ahora), r.puedeReprogramar(ahora, politica),
                r.seriaCancelacionTardia(ahora, politica), historial);
    }
}
