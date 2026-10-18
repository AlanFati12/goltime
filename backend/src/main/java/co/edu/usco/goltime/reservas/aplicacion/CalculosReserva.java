package co.edu.usco.goltime.reservas.aplicacion;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.compartido.dominio.ZonaHoraria;
import co.edu.usco.goltime.reservas.dominio.PoliticaReservas;
import co.edu.usco.goltime.reservas.dominio.Precio;
import co.edu.usco.goltime.reservas.dominio.ValidadorFranja;
import co.edu.usco.goltime.reservas.dominio.ValidadorFranja.ReglasCancha;
import co.edu.usco.goltime.sedes.aplicacion.ServicioFestivos;
import co.edu.usco.goltime.sedes.aplicacion.Vistas.VistaCancha;
import co.edu.usco.goltime.sedes.dominio.CalculadoraPrecio;
import co.edu.usco.goltime.sedes.dominio.HorarioDia;

/** Cálculos compartidos por las reservas del cliente y del administrador: validar la franja, el precio y qué canchas ocupa. */
@Component
public class CalculosReserva {

    private final ServicioFestivos festivos;

    public CalculosReserva(ServicioFestivos festivos) {
        this.festivos = festivos;
    }

    public void validarFranja(VistaCancha cancha, Instant inicio, Instant fin, Instant ahora, PoliticaReservas politica) {
        if (inicio == null || fin == null) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Indica la hora de inicio y de fin.");
        }
        HorarioDia horario = cancha.horarioEfectivo().get(ZonaHoraria.aHoraLocal(inicio).getDayOfWeek().getValue() - 1);
        ValidadorFranja.validar(horario, new ReglasCancha(cancha.cancha().duracionBloqueMin(), cancha.cancha().duracionMinMin(),
                cancha.cancha().duracionMaxMin()), inicio, fin, ahora, politica);
    }

    public Precio calcularPrecio(VistaCancha cancha, Instant inicio, Instant fin) {
        LocalDate desde = ZonaHoraria.fechaLocal(inicio);
        Set<LocalDate> festivosDelRango = festivos.fechasEntre(desde, ZonaHoraria.fechaLocal(fin));
        return Precio.sinDescuento(CalculadoraPrecio.calcular(cancha.cancha().precioBaseHora(), cancha.tarifas(), festivosDelRango, inicio, fin).total());
    }

    /** La cancha reservada y, si es compuesta, todas sus componentes. */
    public static Set<Long> idsQueOcupa(VistaCancha cancha) {
        Set<Long> ids = new LinkedHashSet<>();
        ids.add(cancha.cancha().id());
        ids.addAll(cancha.cancha().componentes());
        return ids;
    }
}
