package co.edu.usco.goltime.torneos.aplicacion;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.usco.goltime.auditoria.aplicacion.RegistroAuditoria;
import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.compartido.dominio.Rol;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.Equipo;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.Jugador;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.JugadorConDorsal;
import co.edu.usco.goltime.torneos.aplicacion.Modelos.ResumenEquipo;
import co.edu.usco.goltime.usuarios.aplicacion.RepositorioUsuarios;

/** Equipos y jugadores. Un equipo y sus jugadores se reutilizan en todos los torneos. */
@Service
public class ServicioEquipos {

    private static final String ENTIDAD = "equipos";

    private final RepositorioEquipos equipos;
    private final RepositorioUsuarios usuarios;
    private final RegistroAuditoria auditoria;

    public ServicioEquipos(RepositorioEquipos equipos, RepositorioUsuarios usuarios, RegistroAuditoria auditoria) {
        this.equipos = equipos;
        this.usuarios = usuarios;
        this.auditoria = auditoria;
    }

    public record DatosEquipo(String nombre, String color, String delegadoNombre, String delegadoTelefono, Long usuarioResponsableId) {
    }

    public record DatosJugador(String nombres, String apellidos, String documento, String posicion, Integer dorsal) {
    }

    public record DetalleEquipo(Equipo equipo, List<JugadorConDorsal> plantilla) {
    }

    // ----- Equipos -----

    @Transactional
    public DetalleEquipo crear(DatosEquipo d) {
        String nombre = nombreValido(d.nombre());
        validarResponsable(d.usuarioResponsableId());
        if (equipos.existeNombre(nombre, null)) {
            throw new ExcepcionNegocio(CodigoError.YA_EXISTE, "Ya existe un equipo con ese nombre.");
        }
        Equipo guardado = equipos.guardar(new Equipo(null, nombre, limpiar(d.color()), limpiar(d.delegadoNombre()), limpiar(d.delegadoTelefono()),
                d.usuarioResponsableId(), true));
        auditoria.registrar("CREAR_EQUIPO", ENTIDAD, guardado.id(), Map.of("nombre", guardado.nombre()), null);
        return new DetalleEquipo(guardado, List.of());
    }

    @Transactional
    public DetalleEquipo actualizar(long id, DatosEquipo d) {
        Equipo actual = cargar(id);
        String nombre = nombreValido(d.nombre());
        validarResponsable(d.usuarioResponsableId());
        if (equipos.existeNombre(nombre, id)) {
            throw new ExcepcionNegocio(CodigoError.YA_EXISTE, "Ya existe un equipo con ese nombre.");
        }
        Equipo nuevo = new Equipo(id, nombre, limpiar(d.color()), limpiar(d.delegadoNombre()), limpiar(d.delegadoTelefono()), d.usuarioResponsableId(),
                actual.activo());
        equipos.guardar(nuevo);
        Map<String, Object> cambios = new LinkedHashMap<>();
        if (!actual.nombre().equals(nuevo.nombre())) {
            cambios.put("nombre", Map.of("antes", actual.nombre(), "despues", nuevo.nombre()));
        }
        auditoria.registrar("ACTUALIZAR_EQUIPO", ENTIDAD, id, cambios, null);
        return detalle(id);
    }

    @Transactional
    public DetalleEquipo cambiarActivo(long id, boolean activo) {
        Equipo actual = cargar(id);
        equipos.guardar(new Equipo(id, actual.nombre(), actual.color(), actual.delegadoNombre(), actual.delegadoTelefono(), actual.usuarioResponsableId(),
                activo));
        auditoria.registrar(activo ? "ACTIVAR_EQUIPO" : "DESACTIVAR_EQUIPO", ENTIDAD, id, Map.of("activo", Map.of("antes", actual.activo(), "despues", activo)), null);
        return detalle(id);
    }

    @Transactional(readOnly = true)
    public DetalleEquipo detalle(long id) {
        return new DetalleEquipo(cargar(id), equipos.plantilla(id));
    }

    @Transactional(readOnly = true)
    public List<ResumenEquipo> listar(String busqueda, boolean incluirInactivos) {
        return equipos.listar(busqueda, incluirInactivos);
    }

    // ----- Jugadores -----

    /** Crea un jugador nuevo y lo agrega a la plantilla del equipo. */
    @Transactional
    public DetalleEquipo agregarJugadorNuevo(long equipoId, DatosJugador d) {
        crearEnPlantilla(equipoId, d);
        return detalle(equipoId);
    }

    /** Igual que {@link #agregarJugadorNuevo} pero devuelve el jugador creado. */
    @Transactional
    public Jugador crearEnPlantilla(long equipoId, DatosJugador d) {
        cargar(equipoId);
        String nombres = textoObligatorio(d.nombres(), "Los nombres del jugador son obligatorios.", 100);
        String apellidos = textoObligatorio(d.apellidos(), "Los apellidos del jugador son obligatorios.", 100);
        String documento = limpiar(d.documento());
        validarDorsal(equipoId, d.dorsal(), null);
        Jugador jugador;
        try {
            jugador = equipos.guardarJugador(new Jugador(null, nombres, apellidos, documento, limpiar(d.posicion()), null, true));
        } catch (org.springframework.dao.DuplicateKeyException ex) {
            throw new ExcepcionNegocio(CodigoError.YA_EXISTE, "Ya existe un jugador con ese documento. Búscalo y agrégalo al equipo.");
        }
        equipos.agregarAPlantilla(equipoId, jugador.id(), d.dorsal());
        auditoria.registrar("AGREGAR_JUGADOR", ENTIDAD, equipoId, Map.of("jugadorId", jugador.id(), "jugador", jugador.nombreCompleto()), null);
        return jugador;
    }

    /** Agrega a la plantilla un jugador que ya existe (por ejemplo, de otro equipo o torneo anterior). */
    @Transactional
    public DetalleEquipo agregarJugadorExistente(long equipoId, long jugadorId, Integer dorsal) {
        cargar(equipoId);
        Jugador jugador = equipos.buscarJugador(jugadorId).orElseThrow(() -> new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "El jugador no existe."));
        if (!jugador.activo()) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "El jugador está desactivado.");
        }
        validarDorsal(equipoId, dorsal, jugadorId);
        equipos.agregarAPlantilla(equipoId, jugadorId, dorsal);
        auditoria.registrar("AGREGAR_JUGADOR", ENTIDAD, equipoId, Map.of("jugadorId", jugadorId, "jugador", jugador.nombreCompleto()), null);
        return detalle(equipoId);
    }

    @Transactional
    public Jugador actualizarJugador(long jugadorId, DatosJugador d) {
        Jugador actual = equipos.buscarJugador(jugadorId).orElseThrow(() -> new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "El jugador no existe."));
        String nombres = textoObligatorio(d.nombres(), "Los nombres del jugador son obligatorios.", 100);
        String apellidos = textoObligatorio(d.apellidos(), "Los apellidos del jugador son obligatorios.", 100);
        try {
            Jugador guardado = equipos.guardarJugador(new Jugador(jugadorId, nombres, apellidos, limpiar(d.documento()), limpiar(d.posicion()),
                    actual.usuarioId(), actual.activo()));
            auditoria.registrar("ACTUALIZAR_JUGADOR", "jugadores", jugadorId, Map.of("jugador", guardado.nombreCompleto()), null);
            return guardado;
        } catch (org.springframework.dao.DuplicateKeyException ex) {
            throw new ExcepcionNegocio(CodigoError.YA_EXISTE, "Ya existe otro jugador con ese documento.");
        }
    }

    @Transactional
    public DetalleEquipo cambiarDorsal(long equipoId, long jugadorId, Integer dorsal) {
        cargar(equipoId);
        exigirEnPlantilla(equipoId, jugadorId);
        validarDorsal(equipoId, dorsal, jugadorId);
        equipos.actualizarDorsal(equipoId, jugadorId, dorsal);
        return detalle(equipoId);
    }

    /** Saca al jugador de la plantilla habitual (no afecta a los torneos en los que ya está inscrito). */
    @Transactional
    public DetalleEquipo quitarJugador(long equipoId, long jugadorId) {
        cargar(equipoId);
        exigirEnPlantilla(equipoId, jugadorId);
        equipos.quitarDePlantilla(equipoId, jugadorId);
        auditoria.registrar("QUITAR_JUGADOR", ENTIDAD, equipoId, Map.of("jugadorId", jugadorId), null);
        return detalle(equipoId);
    }

    @Transactional(readOnly = true)
    public List<Jugador> buscarJugadores(String texto) {
        return equipos.buscarJugadores(texto, 15);
    }

    // ----- Utilidades -----

    private Equipo cargar(long id) {
        return equipos.buscar(id).orElseThrow(() -> new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "El equipo no existe."));
    }

    private void validarResponsable(Long usuarioId) {
        if (usuarioId == null) {
            return;
        }
        boolean valido = usuarios.buscarPorId(usuarioId).filter(u -> u.rol() == Rol.CLIENTE && u.activo()).isPresent();
        if (!valido) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "El responsable debe ser un cliente registrado y activo.");
        }
    }

    private void exigirEnPlantilla(long equipoId, long jugadorId) {
        if (!equipos.enPlantilla(equipoId, jugadorId)) {
            throw new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "El jugador no está en la plantilla del equipo.");
        }
    }

    private void validarDorsal(long equipoId, Integer dorsal, Long jugadorId) {
        if (dorsal == null) {
            return;
        }
        if (dorsal < 0 || dorsal > 99) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "El dorsal debe estar entre 0 y 99.");
        }
        if (equipos.dorsalEnUso(equipoId, dorsal, jugadorId)) {
            throw new ExcepcionNegocio(CodigoError.YA_EXISTE, "El dorsal " + dorsal + " ya lo usa otro jugador del equipo.");
        }
    }

    private static String nombreValido(String nombre) {
        return textoObligatorio(nombre, "El nombre del equipo es obligatorio.", 100);
    }

    static String textoObligatorio(String texto, String mensaje, int maximo) {
        String limpio = limpiar(texto);
        if (limpio == null || limpio.length() > maximo) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, mensaje);
        }
        return limpio;
    }

    static String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
