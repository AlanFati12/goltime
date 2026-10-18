package co.edu.usco.goltime.promociones.aplicacion;

import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.usco.goltime.auditoria.aplicacion.RegistroAuditoria;
import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.compartido.dominio.ZonaHoraria;
import co.edu.usco.goltime.promociones.aplicacion.RepositorioPromociones.ResumenUsos;
import co.edu.usco.goltime.promociones.aplicacion.RepositorioPromociones.Uso;
import co.edu.usco.goltime.promociones.dominio.Promocion;

/** Administración y consulta de promociones. */
@Service
public class ServicioPromociones {

    private static final String ENTIDAD = "promociones";

    private final RepositorioPromociones promociones;
    private final RegistroAuditoria auditoria;
    private final Clock reloj;

    public ServicioPromociones(RepositorioPromociones promociones, RegistroAuditoria auditoria, Clock reloj) {
        this.promociones = promociones;
        this.auditoria = auditoria;
        this.reloj = reloj;
    }

    public record DetallePromocion(Promocion promocion, ResumenUsos usos, List<Uso> ultimosUsos) {
    }

    @Transactional
    public Promocion crear(Promocion datos) {
        validarCodigo(datos.codigo(), null);
        Promocion guardada = promociones.guardar(datos);
        auditoria.registrar("CREAR_PROMOCION", ENTIDAD, guardada.id(), Map.of("nombre", guardada.nombre(), "tipo", guardada.tipo().name(), "valor", guardada.valor()),
                null);
        return guardada;
    }

    @Transactional
    public Promocion actualizar(long id, Promocion datos) {
        Promocion actual = cargar(id);
        validarCodigo(datos.codigo(), id);
        Promocion guardada = promociones.guardar(new Promocion(id, datos.nombre(), datos.descripcion(), datos.codigo(), datos.tipo(), datos.valor(),
                datos.fechaDesde(), datos.fechaHasta(), datos.diasSemana(), datos.minutoInicio(), datos.minutoFin(), datos.sedeId(), datos.canchaId(),
                datos.usosMaximos(), datos.usosPorCliente(), datos.soloPrimeraReserva(), datos.publica(), actual.activa()));
        Map<String, Object> cambios = new LinkedHashMap<>();
        if (actual.valor() != guardada.valor() || actual.tipo() != guardada.tipo()) {
            cambios.put("descuento", Map.of("antes", actual.tipo().texto(actual.valor()), "despues", guardada.tipo().texto(guardada.valor())));
        }
        auditoria.registrar("ACTUALIZAR_PROMOCION", ENTIDAD, id, cambios, null);
        return guardada;
    }

    @Transactional
    public Promocion cambiarActiva(long id, boolean activa) {
        Promocion actual = cargar(id);
        Promocion guardada = promociones.guardar(actual.conActiva(activa));
        auditoria.registrar(activa ? "ACTIVAR_PROMOCION" : "DESACTIVAR_PROMOCION", ENTIDAD, id, Map.of("activa", Map.of("antes", actual.activa(), "despues", activa)),
                null);
        return guardada;
    }

    @Transactional(readOnly = true)
    public List<Promocion> listar(Boolean activa) {
        return promociones.listar(activa);
    }

    @Transactional(readOnly = true)
    public DetallePromocion detalle(long id) {
        Promocion p = cargar(id);
        return new DetallePromocion(p, promociones.resumenUsos(id), promociones.ultimosUsos(id, 20));
    }

    /** Promociones públicas que siguen vigentes, para mostrarlas a cualquier visitante (sin el código de las privadas). */
    @Transactional(readOnly = true)
    public List<Promocion> publicas() {
        LocalDate hoy = ZonaHoraria.fechaLocal(reloj.instant());
        return promociones.publicasVigentes(hoy);
    }

    private Promocion cargar(long id) {
        return promociones.buscar(id).orElseThrow(() -> new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "La promoción no existe."));
    }

    private void validarCodigo(String codigo, Long ignorarId) {
        if (codigo != null && promociones.existeCodigo(codigo, ignorarId)) {
            throw new ExcepcionNegocio(CodigoError.YA_EXISTE, "Ya existe una promoción con ese código.");
        }
    }
}
