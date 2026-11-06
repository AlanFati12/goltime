package co.edu.usco.goltime.sedes.aplicacion;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.usco.goltime.auditoria.aplicacion.RegistroAuditoria;
import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.sedes.aplicacion.Vistas.VistaCancha;
import co.edu.usco.goltime.sedes.aplicacion.Vistas.VistaSede;
import co.edu.usco.goltime.sedes.dominio.Foto;
import co.edu.usco.goltime.sedes.dominio.HorarioSemanal;
import co.edu.usco.goltime.sedes.dominio.Sede;
import co.edu.usco.goltime.sedes.dominio.Sede.DatosSede;

/** Casos de uso de sedes. Toda modificación queda en la bitácora de auditoría. */
@Service
public class ServicioSedes {

    private static final String ENTIDAD = "sedes";

    private final RepositorioSedes sedes;
    private final ServicioCanchas canchas;
    private final ReservasDeEspacio reservas;
    private final RegistroAuditoria auditoria;

    public ServicioSedes(RepositorioSedes sedes, ServicioCanchas canchas, ReservasDeEspacio reservas,
            RegistroAuditoria auditoria) {
        this.sedes = sedes;
        this.canchas = canchas;
        this.reservas = reservas;
        this.auditoria = auditoria;
    }

    /** Sedes activas con sus canchas reservables (vista pública). */
    @Transactional(readOnly = true)
    public List<VistaSede> listarPublicas() {
        return conCanchasYFotos(sedes.listar(true), true);
    }

    /** Todas las sedes, activas o no, con todas sus canchas (vista de administración). */
    @Transactional(readOnly = true)
    public List<VistaSede> listarAdmin() {
        return conCanchasYFotos(sedes.listar(false), false);
    }

    /** Fotos y canchas de todas las sedes pedidas con un número fijo de consultas, sin importar cuántas sean. */
    private List<VistaSede> conCanchasYFotos(List<Sede> lista, boolean soloReservables) {
        if (lista.isEmpty()) {
            return List.of();
        }
        Map<Long, List<Foto>> fotos = sedes.fotosDe(lista.stream().map(Sede::id).toList());
        Map<Long, List<VistaCancha>> canchasPorSede = canchas.vistasDeSedes(lista, soloReservables);
        return lista.stream().map(s -> new VistaSede(s, fotos.getOrDefault(s.id(), List.of()), canchasPorSede.getOrDefault(s.id(), List.of()))).toList();
    }

    @Transactional(readOnly = true)
    public VistaSede detallePublico(long id) {
        Sede sede = cargar(id);
        if (!sede.activa()) {
            throw noExiste();
        }
        return conCanchasYFotos(List.of(sede), true).get(0);
    }

    @Transactional(readOnly = true)
    public VistaSede detalleAdmin(long id) {
        Sede sede = cargar(id);
        return conCanchasYFotos(List.of(sede), false).get(0);
    }

    @Transactional
    public Sede crear(DatosSede datos, HorarioSemanal horario) {
        Sede sede = Sede.nueva(datos, horario == null ? HorarioSemanal.porDefecto() : horario);
        verificarNombre(sede.nombre(), null);
        Sede guardada = sedes.guardar(sede);
        auditoria.registrar("CREAR_SEDE", ENTIDAD, guardada.id(), Map.of("nombre", guardada.nombre()), null);
        return guardada;
    }

    @Transactional
    public Sede actualizar(long id, DatosSede datos) {
        Sede sede = cargar(id);
        String antes = sede.nombre();
        sede.actualizar(datos);
        verificarNombre(sede.nombre(), id);
        Sede guardada = sedes.guardar(sede);
        Map<String, Object> cambios = new LinkedHashMap<>();
        cambios.put("nombre", Map.of("antes", antes, "despues", guardada.nombre()));
        auditoria.registrar("ACTUALIZAR_SEDE", ENTIDAD, id, cambios, null);
        return guardada;
    }

    @Transactional
    public Sede definirHorario(long id, HorarioSemanal horario) {
        Sede sede = cargar(id);
        sede.definirHorario(horario);
        Sede guardada = sedes.guardar(sede);
        auditoria.registrar("ACTUALIZAR_HORARIO_SEDE", ENTIDAD, id, Map.of("dias", horario.dias().size()), null);
        return guardada;
    }

    /** Reservas futuras que se cancelarían al desactivar la sede. */
    @Transactional(readOnly = true)
    public int previsualizarDesactivacion(long id) {
        cargar(id);
        return reservas.contarFuturasActivasEnSede(id);
    }

    /**
     * Activa o desactiva la sede. Al desactivar con reservas futuras exige confirmación explícita y
     * las cancela en la misma transacción; el motivo es obligatorio.
     */
    @Transactional
    public Sede cambiarEstado(long id, boolean activa, String motivo, boolean confirmarConflictos) {
        Sede sede = cargar(id);
        if (sede.activa() == activa) {
            return sede;
        }
        int canceladas = 0;
        if (!activa) {
            if (motivo == null || motivo.isBlank()) {
                throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "El motivo es obligatorio para desactivar una sede.");
            }
            int futuras = reservas.contarFuturasActivasEnSede(id);
            if (futuras > 0 && !confirmarConflictos) {
                throw new ExcepcionNegocio(CodigoError.CONFLICTOS_SIN_CONFIRMAR,
                        "La sede tiene " + futuras + " reservas futuras. Confirma para cancelarlas.");
            }
            if (futuras > 0) {
                canceladas = reservas.cancelarFuturasEnSede(id, "Sede desactivada: " + motivo.trim());
            }
        }
        sede.cambiarEstado(activa);
        Sede guardada = sedes.guardar(sede);
        auditoria.registrar(activa ? "ACTIVAR_SEDE" : "DESACTIVAR_SEDE", ENTIDAD, id,
                Map.of("activa", Map.of("antes", !activa, "despues", activa), "reservasCanceladas", canceladas),
                motivo == null ? null : motivo.trim());
        return guardada;
    }

    Sede cargar(long id) {
        return sedes.buscarPorId(id).orElseThrow(ServicioSedes::noExiste);
    }

    private void verificarNombre(String nombre, Long excluirId) {
        if (sedes.existeNombre(nombre, excluirId)) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Ya existe una sede con el nombre '" + nombre + "'.");
        }
    }

    private static ExcepcionNegocio noExiste() {
        return new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "La sede no existe.");
    }
}
