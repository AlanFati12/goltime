package co.edu.usco.goltime.sedes.aplicacion;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.usco.goltime.auditoria.aplicacion.RegistroAuditoria;
import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.sedes.dominio.Tarifa;

/** Franjas de tarifa de una cancha. Dos franjas del mismo tipo no pueden solaparse. */
@Service
public class ServicioTarifas {

    private static final String ENTIDAD = "tarifas";

    private final RepositorioTarifas tarifas;
    private final ServicioCanchas canchas;
    private final RegistroAuditoria auditoria;

    public ServicioTarifas(RepositorioTarifas tarifas, ServicioCanchas canchas, RegistroAuditoria auditoria) {
        this.tarifas = tarifas;
        this.canchas = canchas;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<Tarifa> listar(long canchaId) {
        canchas.cargar(canchaId);
        return tarifas.deCancha(canchaId);
    }

    @Transactional
    public Tarifa crear(long canchaId, DatosTarifa d) {
        canchas.cargar(canchaId);
        Tarifa nueva = d.aTarifa(null, canchaId);
        verificarSolapamiento(nueva, null);
        Tarifa guardada = tarifas.guardar(nueva);
        auditoria.registrar("CREAR_TARIFA", ENTIDAD, guardada.id(), Map.of("canchaId", canchaId, "precioHora", d.precioHora()), null);
        return guardada;
    }

    @Transactional
    public Tarifa actualizar(long id, DatosTarifa d) {
        Tarifa actual = cargar(id);
        Tarifa nueva = d.aTarifa(id, actual.canchaId());
        verificarSolapamiento(nueva, id);
        Tarifa guardada = tarifas.guardar(nueva);
        auditoria.registrar("ACTUALIZAR_TARIFA", ENTIDAD, id,
                Map.of("precioHora", Map.of("antes", actual.precioHora(), "despues", nueva.precioHora())), null);
        return guardada;
    }

    @Transactional
    public void eliminar(long id) {
        Tarifa actual = cargar(id);
        tarifas.eliminar(id);
        auditoria.registrar("ELIMINAR_TARIFA", ENTIDAD, id, Map.of("canchaId", actual.canchaId(), "precioHora", actual.precioHora()), null);
    }

    private Tarifa cargar(long id) {
        return tarifas.buscarPorId(id).orElseThrow(
                () -> new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "La tarifa no existe."));
    }

    private void verificarSolapamiento(Tarifa nueva, Long excluirId) {
        boolean choca = tarifas.deCancha(nueva.canchaId()).stream()
                .filter(t -> !t.id().equals(excluirId))
                .anyMatch(nueva::seSolapaCon);
        if (choca) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS,
                    "La franja se solapa con otra tarifa de la cancha (mismos días, horas y vigencia).");
        }
    }

    /** Datos de una franja tal como llegan de la API. */
    public record DatosTarifa(Set<DayOfWeek> diasSemana, int minutoInicio, int minutoFin, boolean aplicaFestivo,
            int precioHora, LocalDate vigenteDesde, LocalDate vigenteHasta) {

        Tarifa aTarifa(Long id, long canchaId) {
            return new Tarifa(id, canchaId, diasSemana, minutoInicio, minutoFin, aplicaFestivo, precioHora, vigenteDesde, vigenteHasta);
        }
    }
}
