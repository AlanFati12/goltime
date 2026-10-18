package co.edu.usco.goltime.reservas.aplicacion;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.compartido.dominio.ZonaHoraria;
import co.edu.usco.goltime.reservas.aplicacion.RepositorioAgenda.ElementoAgenda;
import co.edu.usco.goltime.sedes.aplicacion.ServicioCanchas;
import co.edu.usco.goltime.sedes.aplicacion.Vistas.VistaCancha;

/** Agenda del administrador: qué hay en cada cancha de una sede durante un rango de fechas (máximo 31 días). */
@Service
public class ServicioAgenda {

    static final int DIAS_MAXIMOS = 31;

    private final RepositorioAgenda agenda;
    private final ServicioCanchas canchas;

    public ServicioAgenda(RepositorioAgenda agenda, ServicioCanchas canchas) {
        this.agenda = agenda;
        this.canchas = canchas;
    }

    @Transactional(readOnly = true)
    public Agenda delRango(long sedeId, LocalDate desde, LocalDate hasta) {
        if (hasta.isBefore(desde)) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "La fecha final no puede ser anterior a la inicial.");
        }
        if (Duration.between(desde.atStartOfDay(), hasta.atStartOfDay()).toDays() >= DIAS_MAXIMOS) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "La agenda muestra como máximo " + DIAS_MAXIMOS + " días.");
        }
        Instant inicio = ZonaHoraria.inicioDelDia(desde);
        Instant fin = ZonaHoraria.inicioDelDia(hasta.plusDays(1));
        List<VistaCancha> columnas = canchas.listarAdmin(sedeId);
        return new Agenda(columnas, agenda.elementos(sedeId, inicio, fin));
    }

    public record Agenda(List<VistaCancha> canchas, List<ElementoAgenda> elementos) {
    }
}
