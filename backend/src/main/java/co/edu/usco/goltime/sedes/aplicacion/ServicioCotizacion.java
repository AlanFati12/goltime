package co.edu.usco.goltime.sedes.aplicacion;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.compartido.dominio.ZonaHoraria;
import co.edu.usco.goltime.sedes.dominio.CalculadoraPrecio;
import co.edu.usco.goltime.sedes.dominio.Cancha;
import co.edu.usco.goltime.sedes.dominio.DesglosePrecio;

/**
 * Cotiza el precio de jugar en una cancha un intervalo dado. El precio lo calcula siempre el backend;
 * la Fase 3 reutiliza este mismo cálculo para guardar la instantánea de la reserva.
 */
@Service
public class ServicioCotizacion {

    private final ServicioCanchas canchas;
    private final RepositorioTarifas tarifas;
    private final RepositorioFestivos festivos;

    public ServicioCotizacion(ServicioCanchas canchas, RepositorioTarifas tarifas, RepositorioFestivos festivos) {
        this.canchas = canchas;
        this.tarifas = tarifas;
        this.festivos = festivos;
    }

    @Transactional(readOnly = true)
    public DesglosePrecio cotizar(long canchaId, Instant inicio, Instant fin) {
        Cancha cancha = canchas.detallePublico(canchaId).cancha();
        if (inicio == null || fin == null || !fin.isAfter(inicio)) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "La hora de fin debe ser posterior a la de inicio.");
        }
        long minutos = java.time.Duration.between(inicio, fin).toMinutes();
        if (minutos % cancha.duracionBloqueMin() != 0 || minutos < cancha.duracionMinMin() || minutos > cancha.duracionMaxMin()) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "La duración debe ser múltiplo de " + cancha.duracionBloqueMin()
                    + " minutos, entre " + cancha.duracionMinMin() + " y " + cancha.duracionMaxMin() + ".");
        }
        LocalDate desde = ZonaHoraria.fechaLocal(inicio);
        LocalDate hasta = ZonaHoraria.fechaLocal(fin);
        Set<LocalDate> fechasFestivas = festivos.fechasEntre(desde, hasta);
        return CalculadoraPrecio.calcular(cancha.precioBaseHora(), tarifas.deCancha(canchaId), fechasFestivas, inicio, fin);
    }
}
