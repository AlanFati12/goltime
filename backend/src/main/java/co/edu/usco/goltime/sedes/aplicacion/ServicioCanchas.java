package co.edu.usco.goltime.sedes.aplicacion;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.usco.goltime.auditoria.aplicacion.RegistroAuditoria;
import co.edu.usco.goltime.compartido.aplicacion.LectorConfiguracion;
import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.sedes.aplicacion.Vistas.VistaCancha;
import co.edu.usco.goltime.sedes.dominio.Cancha;
import co.edu.usco.goltime.sedes.dominio.Cancha.DatosCancha;
import co.edu.usco.goltime.sedes.dominio.EstadoCancha;
import co.edu.usco.goltime.sedes.dominio.Foto;
import co.edu.usco.goltime.sedes.dominio.HorarioDia;
import co.edu.usco.goltime.sedes.dominio.HorarioSemanal;
import co.edu.usco.goltime.sedes.dominio.Sede;
import co.edu.usco.goltime.sedes.dominio.Tarifa;

/** Casos de uso de canchas: alta, edición, composición, horario propio y cambios de estado. */
@Service
public class ServicioCanchas {

    private static final String ENTIDAD = "canchas";

    private final RepositorioCanchas canchas;
    private final RepositorioSedes sedes;
    private final RepositorioTarifas tarifas;
    private final ReservasDeEspacio reservas;
    private final LectorConfiguracion configuracion;
    private final RegistroAuditoria auditoria;

    public ServicioCanchas(RepositorioCanchas canchas, RepositorioSedes sedes, RepositorioTarifas tarifas,
            ReservasDeEspacio reservas, LectorConfiguracion configuracion, RegistroAuditoria auditoria) {
        this.canchas = canchas;
        this.sedes = sedes;
        this.tarifas = tarifas;
        this.reservas = reservas;
        this.configuracion = configuracion;
        this.auditoria = auditoria;
    }

    // --- Lectura ---

    /** Vistas de todas las canchas de varias sedes con un número fijo de consultas (clave: id de sede). */
    Map<Long, List<VistaCancha>> vistasDeSedes(List<Sede> sedesPedidas, boolean soloReservables) {
        Map<Long, Sede> porId = sedesPedidas.stream().collect(Collectors.toMap(Sede::id, s -> s));
        List<Cancha> todas = canchas.listar(null).stream()
                .filter(c -> porId.containsKey(c.sedeId()))
                .filter(c -> !soloReservables || c.esReservable())
                .toList();
        return vistas(todas, porId).stream().collect(Collectors.groupingBy(v -> v.sede().id(), LinkedHashMap::new, Collectors.toList()));
    }

    @Transactional(readOnly = true)
    public List<VistaCancha> listarAdmin(Long sedeId) {
        List<Cancha> lista = canchas.listar(sedeId);
        return vistas(lista, sedesDe(lista));
    }

    /** Canchas reservables de sedes activas, opcionalmente filtradas por sede y modalidad (ej. "F5"). */
    @Transactional(readOnly = true)
    public List<VistaCancha> listarPublicas(Long sedeId, String modalidad) {
        List<Cancha> lista = canchas.listar(sedeId).stream()
                .filter(Cancha::esReservable)
                .filter(c -> modalidad == null || modalidad.isBlank() || c.modalidad().equalsIgnoreCase(modalidad.trim()))
                .toList();
        return vistas(lista, sedesDe(lista)).stream().filter(v -> v.sede().activa()).toList();
    }

    @Transactional(readOnly = true)
    public VistaCancha detalleAdmin(long id) {
        Cancha cancha = cargar(id);
        return vista(cancha, sedeDe(cancha));
    }

    @Transactional(readOnly = true)
    public VistaCancha detallePublico(long id) {
        Cancha cancha = cargar(id);
        Sede sede = sedeDe(cancha);
        if (!cancha.esReservable() || !sede.activa()) {
            throw noExiste();
        }
        return vista(cancha, sede);
    }

    // --- Escritura ---

    @Transactional
    public VistaCancha crear(long sedeId, DatosCancha datos, Set<Long> componentes) {
        Sede sede = sedes.buscarPorId(sedeId).orElseThrow(
                () -> new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "La sede no existe."));
        verificarModalidad(datos.jugadoresPorEquipo());
        Cancha cancha = Cancha.nueva(sedeId, datos, componentes);
        verificarNombre(sedeId, cancha.nombre(), null);
        verificarComponentes(cancha, componentes, null);
        Cancha guardada = canchas.guardar(cancha);
        auditoria.registrar("CREAR_CANCHA", ENTIDAD, guardada.id(),
                Map.of("nombre", guardada.nombre(), "sedeId", sedeId, "componentes", guardada.componentes().size()), null);
        return vista(guardada, sede);
    }

    @Transactional
    public VistaCancha actualizar(long id, DatosCancha datos, Set<Long> componentes) {
        Cancha cancha = cargar(id);
        verificarModalidad(datos.jugadoresPorEquipo());
        Map<String, Object> cambios = new LinkedHashMap<>();
        if (cancha.precioBaseHora() != datos.precioBaseHora()) {
            cambios.put("precioBaseHora", Map.of("antes", cancha.precioBaseHora(), "despues", datos.precioBaseHora()));
        }
        cancha.actualizar(datos);
        verificarNombre(cancha.sedeId(), cancha.nombre(), id);
        cancha.definirComponentes(componentes);
        verificarComponentes(cancha, componentes, id);
        Cancha guardada = canchas.guardar(cancha);
        cambios.put("componentes", guardada.componentes().size());
        auditoria.registrar("ACTUALIZAR_CANCHA", ENTIDAD, id, cambios, null);
        return vista(guardada, sedeDe(guardada));
    }

    @Transactional
    public VistaCancha definirHorarioPropio(long id, HorarioSemanal horario) {
        Cancha cancha = cargar(id);
        cancha.definirHorarioPropio(horario.estaVacio() ? null : horario);
        Cancha guardada = canchas.guardar(cancha);
        auditoria.registrar("ACTUALIZAR_HORARIO_CANCHA", ENTIDAD, id, Map.of("dias", horario.dias().size()), null);
        return vista(guardada, sedeDe(guardada));
    }

    @Transactional
    public VistaCancha quitarHorarioPropio(long id) {
        return definirHorarioPropio(id, new HorarioSemanal(List.of()));
    }

    /** Reservas futuras que se cancelarían al sacar la cancha de servicio. */
    @Transactional(readOnly = true)
    public int previsualizarCambioDeEstado(long id, EstadoCancha nuevo) {
        cargar(id);
        return nuevo == EstadoCancha.ACTIVA ? 0 : reservas.contarFuturasActivasEnCancha(id);
    }

    /**
     * Cambia el estado. Pasar a MANTENIMIENTO o INACTIVA con reservas futuras exige confirmación
     * explícita (mismo patrón que el bloqueo de horarios) y las cancela en la misma transacción.
     */
    @Transactional
    public VistaCancha cambiarEstado(long id, EstadoCancha nuevo, String motivo, boolean confirmarConflictos) {
        Cancha cancha = cargar(id);
        EstadoCancha anterior = cancha.estado();
        if (anterior == nuevo) {
            return vista(cancha, sedeDe(cancha));
        }
        int canceladas = 0;
        if (nuevo != EstadoCancha.ACTIVA) {
            if (motivo == null || motivo.isBlank()) {
                throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "El motivo es obligatorio para sacar la cancha de servicio.");
            }
            int futuras = reservas.contarFuturasActivasEnCancha(id);
            if (futuras > 0 && !confirmarConflictos) {
                throw new ExcepcionNegocio(CodigoError.CONFLICTOS_SIN_CONFIRMAR,
                        "La cancha tiene " + futuras + " reservas futuras. Confirma para cancelarlas.");
            }
            if (futuras > 0) {
                canceladas = reservas.cancelarFuturasEnCancha(id, "Cancha en " + nuevo.name().toLowerCase() + ": " + motivo.trim());
            }
        }
        cancha.cambiarEstado(nuevo);
        Cancha guardada = canchas.guardar(cancha);
        auditoria.registrar("CAMBIAR_ESTADO_CANCHA", ENTIDAD, id,
                Map.of("estado", Map.of("antes", anterior.name(), "despues", nuevo.name()), "reservasCanceladas", canceladas),
                motivo == null ? null : motivo.trim());
        return vista(guardada, sedeDe(guardada));
    }

    // --- Apoyo ---

    Cancha cargar(long id) {
        return canchas.buscarPorId(id).orElseThrow(ServicioCanchas::noExiste);
    }

    private Sede sedeDe(Cancha cancha) {
        return sedes.buscarPorId(cancha.sedeId()).orElseThrow();
    }

    /** Las sedes de unas canchas, buscadas todas juntas en una sola consulta. */
    private Map<Long, Sede> sedesDe(List<Cancha> lista) {
        Set<Long> ids = lista.stream().map(Cancha::sedeId).collect(Collectors.toSet());
        return sedes.buscarPorIds(ids).stream().collect(Collectors.toMap(Sede::id, s -> s));
    }

    private VistaCancha vista(Cancha cancha, Sede sede) {
        return vistas(List.of(cancha), Map.of(sede.id(), sede)).get(0);
    }

    /**
     * Arma las vistas de varias canchas pidiendo fotos, tarifas y componentes de todas a la vez. Armarlas una
     * por una dispara 3 consultas por cancha (el problema N+1) y encarece cada visita a la portada.
     */
    private List<VistaCancha> vistas(List<Cancha> lista, Map<Long, Sede> sedesPorId) {
        if (lista.isEmpty()) {
            return List.of();
        }
        List<Long> ids = lista.stream().map(Cancha::id).toList();
        Map<Long, List<Foto>> fotos = canchas.fotosDe(ids);
        Map<Long, List<Tarifa>> tarifasPorCancha = tarifas.deCanchas(ids);
        Set<Long> idsComponentes = lista.stream().flatMap(c -> c.componentes().stream()).collect(Collectors.toSet());
        Map<Long, Cancha> componentesPorId = idsComponentes.isEmpty() ? Map.of()
                : canchas.buscarPorIds(idsComponentes).stream().collect(Collectors.toMap(Cancha::id, c -> c));
        return lista.stream().map(cancha -> {
            Sede sede = sedesPorId.get(cancha.sedeId());
            List<HorarioDia> efectivo = new ArrayList<>();
            for (DayOfWeek dia : DayOfWeek.values()) {
                efectivo.add(HorarioSemanal.efectivo(dia, sede.horario(), cancha.horarioPropio()));
            }
            List<Cancha> componentes = cancha.componentes().stream().map(componentesPorId::get).filter(java.util.Objects::nonNull)
                    .sorted((a, b) -> a.nombre().compareToIgnoreCase(b.nombre())).toList();
            return new VistaCancha(cancha, sede, fotos.getOrDefault(cancha.id(), List.of()),
                    tarifasPorCancha.getOrDefault(cancha.id(), List.of()), efectivo, componentes);
        }).toList();
    }

    private void verificarModalidad(int jugadoresPorEquipo) {
        Set<Integer> permitidas = Arrays.stream(configuracion.texto("modalidades_permitidas").orElse("5,6,7,8,9,11").split(","))
                .map(String::trim).filter(s -> s.matches("\\d+")).map(Integer::parseInt).collect(Collectors.toSet());
        if (!permitidas.contains(jugadoresPorEquipo)) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "La modalidad F" + jugadoresPorEquipo
                    + " no está permitida. Modalidades válidas: " + permitidas.stream().sorted().map(n -> "F" + n).collect(Collectors.joining(", ")) + ".");
        }
    }

    private void verificarNombre(long sedeId, String nombre, Long excluirId) {
        if (canchas.existeNombre(sedeId, nombre, excluirId)) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Ya existe una cancha llamada '" + nombre + "' en esta sede.");
        }
    }

    /**
     * Reglas de las canchas dependientes: las componentes existen, son de la misma sede, no son
     * compuestas a su vez y la cancha no puede ser compuesta si ya es componente de otra.
     */
    private void verificarComponentes(Cancha cancha, Set<Long> ids, Long propioId) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        List<Cancha> componentes = canchas.buscarPorIds(ids);
        if (componentes.size() != ids.size()) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Alguna de las canchas de las que depende no existe.");
        }
        for (Cancha c : componentes) {
            if (c.sedeId() != cancha.sedeId()) {
                throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Las canchas de las que depende deben ser de la misma sede.");
            }
            if (c.esCompuesta()) {
                throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS,
                        "'" + c.nombre() + "' ya es una cancha compuesta; solo se admite un nivel de dependencia.");
            }
        }
        if (propioId != null && !canchas.compuestasQueUsan(propioId).isEmpty()) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS,
                    "Esta cancha es componente de otra compuesta, por lo que no puede depender de más canchas.");
        }
    }

    private static ExcepcionNegocio noExiste() {
        return new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "La cancha no existe.");
    }
}
