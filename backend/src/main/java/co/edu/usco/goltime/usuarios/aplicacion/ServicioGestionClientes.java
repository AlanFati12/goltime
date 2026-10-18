package co.edu.usco.goltime.usuarios.aplicacion;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.usco.goltime.auditoria.aplicacion.RegistroAuditoria;
import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.compartido.dominio.Pagina;
import co.edu.usco.goltime.compartido.dominio.Rol;
import co.edu.usco.goltime.compartido.dominio.SolicitudPagina;
import co.edu.usco.goltime.usuarios.dominio.Usuario;
import co.edu.usco.goltime.usuarios.dominio.UsuarioDesactivadoEvento;

/** Casos de uso del administrador sobre los clientes. Toda modificación queda en la bitácora. */
@Service
public class ServicioGestionClientes {

    private static final String ENTIDAD = "usuarios";

    private final RepositorioUsuarios usuarios;
    private final ReservasDeUsuario reservas;
    private final RegistroAuditoria auditoria;
    private final ApplicationEventPublisher eventos;
    private final Clock reloj;

    public ServicioGestionClientes(RepositorioUsuarios usuarios, ReservasDeUsuario reservas,
            RegistroAuditoria auditoria, ApplicationEventPublisher eventos, Clock reloj) {
        this.usuarios = usuarios;
        this.reservas = reservas;
        this.auditoria = auditoria;
        this.eventos = eventos;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public Pagina<Usuario> listar(FiltroClientes filtro, SolicitudPagina pagina) {
        return usuarios.buscarClientes(filtro, pagina);
    }

    @Transactional(readOnly = true)
    public Usuario detalle(long clienteId) {
        return cargarCliente(clienteId);
    }

    /** Cuántas reservas futuras se verían afectadas; el front lo muestra antes de confirmar. */
    @Transactional(readOnly = true)
    public int previsualizarDesactivacion(long clienteId) {
        cargarCliente(clienteId);
        return reservas.contarFuturasActivas(clienteId);
    }

    /**
     * Desactiva al cliente. Si tiene reservas futuras exige confirmación explícita
     * ({@code confirmarConflictos}); con ella se cancelan en la misma transacción.
     */
    @Transactional
    public Usuario desactivar(long clienteId, long adminId, String motivo, boolean confirmarConflictos) {
        Usuario cliente = cargarCliente(clienteId);
        int futuras = reservas.contarFuturasActivas(clienteId);
        if (futuras > 0 && !confirmarConflictos) {
            throw new ExcepcionNegocio(CodigoError.CONFLICTOS_SIN_CONFIRMAR,
                    "El cliente tiene " + futuras + " reservas futuras. Confirma para cancelarlas.");
        }
        cliente.desactivar(motivo, adminId, reloj.instant());
        int canceladas = futuras > 0 ? reservas.cancelarFuturas(clienteId, "Cuenta desactivada: " + motivo.trim()) : 0;
        Usuario guardado = usuarios.guardar(cliente);
        eventos.publishEvent(new UsuarioDesactivadoEvento(clienteId));

        Map<String, Object> cambios = new LinkedHashMap<>();
        cambios.put("activo", Map.of("antes", true, "despues", false));
        cambios.put("reservasCanceladas", canceladas);
        auditoria.registrar("DESACTIVAR_CLIENTE", ENTIDAD, clienteId, cambios, motivo.trim());
        return guardado;
    }

    @Transactional
    public Usuario activar(long clienteId) {
        Usuario cliente = cargarCliente(clienteId);
        cliente.activar();
        Usuario guardado = usuarios.guardar(cliente);
        auditoria.registrar("ACTIVAR_CLIENTE", ENTIDAD, clienteId,
                Map.of("activo", Map.of("antes", false, "despues", true)), null);
        return guardado;
    }

    @Transactional
    public Usuario habilitarInscripcionEquipos(long clienteId, boolean habilitado) {
        Usuario cliente = cargarCliente(clienteId);
        boolean antes = cliente.puedeInscribirEquipos();
        cliente.habilitarInscripcionEquipos(habilitado);
        Usuario guardado = usuarios.guardar(cliente);
        auditoria.registrar("HABILITAR_INSCRIPCION_EQUIPOS", ENTIDAD, clienteId,
                Map.of("puedeInscribirEquipos", Map.of("antes", antes, "despues", habilitado)), null);
        return guardado;
    }

    private Usuario cargarCliente(long clienteId) {
        return usuarios.buscarPorId(clienteId)
                .filter(u -> u.rol() == Rol.CLIENTE)
                .orElseThrow(() -> new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "El cliente no existe."));
    }
}
