package co.edu.usco.goltime.sedes.aplicacion;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.usco.goltime.auditoria.aplicacion.RegistroAuditoria;
import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.sedes.dominio.Festivo;

/** Festivos de Colombia administrables: afectan a las tarifas especiales de festivo. */
@Service
public class ServicioFestivos {

    private static final String ENTIDAD = "festivos";

    private final RepositorioFestivos festivos;
    private final RegistroAuditoria auditoria;

    public ServicioFestivos(RepositorioFestivos festivos, RegistroAuditoria auditoria) {
        this.festivos = festivos;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<Festivo> delAnio(int anio) {
        return festivos.delAnio(anio);
    }

    /** Fechas festivas dentro del rango (extremos incluidos). Lo usan los cálculos de precio de otros módulos. */
    @Transactional(readOnly = true)
    public java.util.Set<LocalDate> fechasEntre(LocalDate desde, LocalDate hasta) {
        return festivos.fechasEntre(desde, hasta);
    }

    @Transactional
    public Festivo crear(LocalDate fecha, String nombre) {
        Festivo nuevo = new Festivo(null, fecha, nombre);
        if (festivos.existe(fecha)) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Ya existe un festivo el " + fecha + ".");
        }
        Festivo guardado = festivos.guardar(nuevo);
        auditoria.registrar("CREAR_FESTIVO", ENTIDAD, guardado.id(), Map.of("fecha", fecha.toString(), "nombre", guardado.nombre()), null);
        return guardado;
    }

    @Transactional
    public void eliminar(long id) {
        Festivo festivo = festivos.buscarPorId(id).orElseThrow(
                () -> new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "El festivo no existe."));
        festivos.eliminar(id);
        auditoria.registrar("ELIMINAR_FESTIVO", ENTIDAD, id, Map.of("fecha", festivo.fecha().toString(), "nombre", festivo.nombre()), null);
    }
}
