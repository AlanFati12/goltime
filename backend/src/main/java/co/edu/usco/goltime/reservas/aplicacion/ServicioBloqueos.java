package co.edu.usco.goltime.reservas.aplicacion;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.usco.goltime.auditoria.aplicacion.RegistroAuditoria;
import co.edu.usco.goltime.compartido.dominio.Actor;
import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.reservas.aplicacion.Vistas.VistaReserva;
import co.edu.usco.goltime.reservas.dominio.Bloqueo;
import co.edu.usco.goltime.reservas.dominio.Bloqueo.Intervalo;
import co.edu.usco.goltime.reservas.dominio.MotivoBloqueo;
import co.edu.usco.goltime.reservas.dominio.Reserva;
import co.edu.usco.goltime.sedes.aplicacion.ServicioCanchas;
import co.edu.usco.goltime.sedes.aplicacion.Vistas.VistaCancha;

/**
 * Bloqueos de horario con el flujo de conflictos obligatorio:
 * <ol>
 *   <li>{@link #previsualizar} devuelve las reservas activas que se verían afectadas y una <b>huella</b> de esa lista.</li>
 *   <li>Si hay conflictos, el administrador debe confirmar enviando esa huella al crear o editar el bloqueo.</li>
 *   <li>Al confirmar, en una sola transacción se cancelan esas reservas (actor ADMIN, motivo "Bloqueo de horario: ..."),
 *       se avisa a los clientes y se ocupan las canchas.</li>
 *   <li>Si entre la previsualización y la confirmación cambió la lista (aparecieron o se movieron reservas), la huella ya
 *       no coincide y se vuelve a pedir confirmación: no se cancela ninguna reserva que el administrador no haya visto.</li>
 * </ol>
 */
@Service
public class ServicioBloqueos {

    private static final String ENTIDAD = "bloqueos";

    private final RepositorioBloqueos bloqueos;
    private final RepositorioOcupaciones ocupaciones;
    private final RepositorioReservas reservas;
    private final ServicioReservas servicioReservas;
    private final ServicioCanchas canchas;
    private final RegistroAuditoria auditoria;
    private final Clock reloj;

    public ServicioBloqueos(RepositorioBloqueos bloqueos, RepositorioOcupaciones ocupaciones, RepositorioReservas reservas,
            ServicioReservas servicioReservas, ServicioCanchas canchas, RegistroAuditoria auditoria, Clock reloj) {
        this.bloqueos = bloqueos;
        this.ocupaciones = ocupaciones;
        this.reservas = reservas;
        this.servicioReservas = servicioReservas;
        this.canchas = canchas;
        this.auditoria = auditoria;
        this.reloj = reloj;
    }

    /** Datos del bloqueo tal como los envía el administrador. {@code canchaIds} vacío = toda la sede. */
    public record DatosBloqueo(long sedeId, Set<Long> canchaIds, Bloqueo.Tipo tipo, Instant inicio, Instant fin, boolean todoElDia,
            Set<DayOfWeek> diasSemana, int minutoInicio, int minutoFin, LocalDate fechaDesde, LocalDate fechaHasta,
            MotivoBloqueo motivo, String detalle) {

        Bloqueo aBloqueo(Long id, boolean activo) {
            return tipo == Bloqueo.Tipo.PUNTUAL
                    ? Bloqueo.puntual(id, sedeId, canchaIds, inicio, fin, todoElDia, motivo, detalle, activo)
                    : Bloqueo.recurrente(id, sedeId, canchaIds, diasSemana, minutoInicio, minutoFin, fechaDesde, fechaHasta, motivo, detalle, activo);
        }
    }

    public record ConflictoReserva(long reservaId, String codigo, String cliente, String canchaNombre, Instant inicio, Instant fin,
            int precioFinal, boolean conPago, String estadoPago, String estado) {
    }

    public record PrevisualizacionBloqueo(List<ConflictoReserva> conflictos, int intervalos, int canchasAfectadas, String huella) {
        public boolean requiereConfirmacion() {
            return !conflictos.isEmpty();
        }
    }

    // ----- Previsualizar -----

    @Transactional(readOnly = true)
    public PrevisualizacionBloqueo previsualizar(DatosBloqueo datos) {
        return calcular(datos.aBloqueo(null, true), reloj.instant());
    }

    // ----- Crear / editar / eliminar -----

    @Transactional
    public Bloqueo crear(Actor admin, DatosBloqueo datos, String huellaConfirmada) {
        Instant ahora = reloj.instant();
        Bloqueo nuevo = datos.aBloqueo(null, true);
        PrevisualizacionBloqueo previa = calcular(nuevo, ahora);
        int canceladas = cancelarConflictos(admin, previa, huellaConfirmada, nuevo);
        Bloqueo guardado = bloqueos.guardar(nuevo);
        ocupaciones.ocuparBloqueo(guardado.id(), idsAfectados(nuevo), nuevo.expandir(ahora));
        auditoria.registrar("CREAR_BLOQUEO", ENTIDAD, guardado.id(), resumen(guardado, previa, canceladas), guardado.motivoTexto());
        return guardado;
    }

    /** Edita el bloqueo: libera su ocupación anterior y vuelve a aplicar el flujo de conflictos con los datos nuevos. */
    @Transactional
    public Bloqueo actualizar(Actor admin, long id, DatosBloqueo datos, String huellaConfirmada) {
        Bloqueo actual = cargar(id);
        if (!actual.activo()) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "El bloqueo ya fue eliminado.");
        }
        if (actual.sedeId() != datos.sedeId()) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Un bloqueo no puede cambiar de sede.");
        }
        Instant ahora = reloj.instant();
        Bloqueo editado = datos.aBloqueo(id, true);
        PrevisualizacionBloqueo previa = calcular(editado, ahora);
        int canceladas = cancelarConflictos(admin, previa, huellaConfirmada, editado);
        ocupaciones.liberarBloqueo(id);
        Bloqueo guardado = bloqueos.guardar(editado);
        ocupaciones.ocuparBloqueo(id, idsAfectados(editado), editado.expandir(ahora));
        auditoria.registrar("ACTUALIZAR_BLOQUEO", ENTIDAD, id, resumen(guardado, previa, canceladas), guardado.motivoTexto());
        return guardado;
    }

    /** Elimina el bloqueo (borrado lógico) y libera las canchas. */
    @Transactional
    public void eliminar(Actor admin, long id) {
        Bloqueo bloqueo = cargar(id);
        if (!bloqueo.activo()) {
            return;
        }
        bloqueo.desactivar();
        bloqueos.guardar(bloqueo);
        ocupaciones.liberarBloqueo(id);
        auditoria.registrar("ELIMINAR_BLOQUEO", ENTIDAD, id, Map.of("sedeId", bloqueo.sedeId()), bloqueo.motivoTexto());
    }

    // ----- Consultas -----

    @Transactional(readOnly = true)
    public List<Bloqueo> listar(Long sedeId, boolean soloActivos) {
        return bloqueos.listar(sedeId, soloActivos);
    }

    @Transactional(readOnly = true)
    public Bloqueo detalle(long id) {
        return cargar(id);
    }

    // ----- Núcleo -----

    private PrevisualizacionBloqueo calcular(Bloqueo bloqueo, Instant ahora) {
        Set<Long> afectadas = idsAfectados(bloqueo);
        List<Intervalo> intervalos = bloqueo.expandir(ahora);
        List<Long> ids = ocupaciones.reservasActivasEnIntervalos(afectadas, intervalos);
        Map<Long, Relacionados> relacionados = reservas.relacionados(ids);
        List<ConflictoReserva> conflictos = new ArrayList<>();
        for (long id : ids) {
            Reserva r = reservas.buscarPorId(id).orElseThrow();
            Relacionados rel = relacionados.get(id);
            conflictos.add(new ConflictoReserva(id, r.codigo(), rel.clienteNombre(), rel.canchaNombre(), r.inicio(), r.fin(),
                    r.precio().finalAPagar(), r.montoPagado() > 0, r.estadoPago().name(), r.estado().name()));
        }
        conflictos.sort(Comparator.comparing(ConflictoReserva::inicio).thenComparing(ConflictoReserva::reservaId));
        return new PrevisualizacionBloqueo(conflictos, intervalos.size(), afectadas.size(), huella(conflictos));
    }

    /**
     * Si hay conflictos exige que el administrador haya confirmado exactamente esta lista; si la lista cambió desde la
     * previsualización, responde 409 y obliga a revisarla de nuevo. Luego cancela las reservas afectadas.
     */
    private int cancelarConflictos(Actor admin, PrevisualizacionBloqueo previa, String huellaConfirmada, Bloqueo bloqueo) {
        if (previa.conflictos().isEmpty()) {
            return 0;
        }
        if (huellaConfirmada == null || huellaConfirmada.isBlank()) {
            throw new ExcepcionNegocio(CodigoError.CONFLICTOS_SIN_CONFIRMAR,
                    "El bloqueo afecta " + previa.conflictos().size() + " reservas. Revisa la lista y confirma para cancelarlas.");
        }
        if (!previa.huella().equals(huellaConfirmada)) {
            throw new ExcepcionNegocio(CodigoError.CONFLICTOS_SIN_CONFIRMAR,
                    "Las reservas afectadas cambiaron desde que las revisaste (ahora son " + previa.conflictos().size()
                            + "). Revisa la lista de nuevo antes de confirmar.");
        }
        String motivo = "Bloqueo de horario: " + bloqueo.motivoTexto();
        for (ConflictoReserva c : previa.conflictos()) {
            VistaReserva cancelada = servicioReservas.cancelarPorAdmin(c.reservaId(), admin, motivo);
            if (cancelada.reserva().estado().ocupaHorario()) {
                throw new IllegalStateException("La reserva " + c.codigo() + " no se canceló");
            }
        }
        return previa.conflictos().size();
    }

    /** Canchas elegidas (o todas las de la sede) más las componentes de las compuestas: una reserva de la F8 ocupa también las F5. */
    private Set<Long> idsAfectados(Bloqueo bloqueo) {
        List<VistaCancha> deLaSede = canchas.listarAdmin(bloqueo.sedeId());
        if (deLaSede.isEmpty()) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "La sede no tiene canchas que bloquear.");
        }
        Set<Long> idsSede = deLaSede.stream().map(v -> v.cancha().id()).collect(Collectors.toSet());
        Set<Long> elegidas = bloqueo.canchaIds().isEmpty() ? idsSede : bloqueo.canchaIds();
        if (!idsSede.containsAll(elegidas)) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Alguna cancha elegida no pertenece a la sede.");
        }
        Set<Long> afectadas = new LinkedHashSet<>(elegidas);
        deLaSede.stream().filter(v -> elegidas.contains(v.cancha().id())).forEach(v -> afectadas.addAll(v.cancha().componentes()));
        return afectadas;
    }

    private Bloqueo cargar(long id) {
        return bloqueos.buscarPorId(id).orElseThrow(() -> new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "El bloqueo no existe."));
    }

    private static Map<String, Object> resumen(Bloqueo b, PrevisualizacionBloqueo previa, int canceladas) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("tipo", b.tipo().name());
        m.put("sedeId", b.sedeId());
        m.put("canchas", b.canchaIds().isEmpty() ? "toda la sede" : b.canchaIds().toString());
        m.put("intervalos", previa.intervalos());
        m.put("reservasCanceladas", canceladas);
        return m;
    }

    /** Huella de la lista de conflictos: cambia si aparece, se mueve o se cancela alguna reserva afectada. */
    static String huella(List<ConflictoReserva> conflictos) {
        String texto = conflictos.stream().map(c -> c.reservaId() + ":" + c.inicio().toEpochMilli() + ":" + c.fin().toEpochMilli() + ":" + c.estado())
                .sorted().collect(Collectors.joining("|"));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8))).substring(0, 16);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }
}
