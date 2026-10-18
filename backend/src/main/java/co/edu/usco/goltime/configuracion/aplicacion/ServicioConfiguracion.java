package co.edu.usco.goltime.configuracion.aplicacion;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.usco.goltime.auditoria.aplicacion.RegistroAuditoria;
import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.configuracion.dominio.ReglaConfiguracion;

/** Consulta y edición de la configuración del negocio. Los cambios aplican de inmediato (se leen en cada uso) y quedan auditados. */
@Service
public class ServicioConfiguracion {

    public record Parametro(String clave, String valor, String tipo, String descripcion, boolean editable, Integer minimo, Integer maximo, Instant actualizadoEn) {
    }

    public interface Repositorio {

        List<Parametro> listar();

        java.util.Optional<Parametro> buscar(String clave);

        void actualizar(String clave, String valor, Long actorId);
    }

    private final Repositorio repositorio;
    private final RegistroAuditoria auditoria;

    public ServicioConfiguracion(Repositorio repositorio, RegistroAuditoria auditoria) {
        this.repositorio = repositorio;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<Parametro> listar() {
        return repositorio.listar();
    }

    @Transactional
    public Parametro actualizar(String clave, String valor, Long actorId) {
        ReglaConfiguracion regla = ReglaConfiguracion.de(clave)
                .orElseThrow(() -> new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "Ese parámetro no existe o no se puede editar."));
        Parametro actual = repositorio.buscar(clave).orElseThrow(() -> new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "Ese parámetro no existe."));
        String nuevo = regla.validar(clave, valor);
        if (!nuevo.equals(actual.valor())) {
            repositorio.actualizar(clave, nuevo, actorId);
            auditoria.registrar("ACTUALIZAR_CONFIGURACION", "configuracion", null, Map.of(clave, Map.of("antes", actual.valor(), "despues", nuevo)), null);
        }
        return repositorio.buscar(clave).orElseThrow();
    }
}
