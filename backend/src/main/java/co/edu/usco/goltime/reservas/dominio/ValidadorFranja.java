package co.edu.usco.goltime.reservas.dominio;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.compartido.dominio.ZonaHoraria;
import co.edu.usco.goltime.sedes.dominio.HorarioDia;
import co.edu.usco.goltime.sedes.dominio.MinutoDelDia;

/**
 * Reglas que debe cumplir el horario de una reserva: dentro del horario de operación, alineado al
 * bloque de la cancha, con duración permitida y dentro de la ventana de antelación. No consulta la
 * disponibilidad (eso lo garantiza la base de datos).
 */
public final class ValidadorFranja {

    private ValidadorFranja() {
    }

    /** Datos de la cancha que importan para validar una franja. */
    public record ReglasCancha(int bloqueMin, int minimaMin, int maximaMin) {
    }

    public static void validar(HorarioDia horario, ReglasCancha cancha, Instant inicio, Instant fin, Instant ahora,
            PoliticaReservas politica) {
        if (inicio == null || fin == null || !fin.isAfter(inicio)) {
            throw invalida("La hora de fin debe ser posterior a la de inicio.");
        }
        LocalDateTime inicioLocal = ZonaHoraria.aHoraLocal(inicio);
        LocalDateTime finLocal = ZonaHoraria.aHoraLocal(fin);
        int minutoInicio = inicioLocal.getHour() * 60 + inicioLocal.getMinute();
        int minutoFin;
        if (finLocal.toLocalDate().equals(inicioLocal.toLocalDate())) {
            minutoFin = finLocal.getHour() * 60 + finLocal.getMinute();
        } else if (finLocal.toLocalDate().equals(inicioLocal.toLocalDate().plusDays(1)) && finLocal.toLocalTime().equals(java.time.LocalTime.MIDNIGHT)) {
            minutoFin = MinutoDelDia.FIN_DEL_DIA;
        } else {
            throw new ExcepcionNegocio(CodigoError.FUERA_DE_HORARIO, "Una reserva no puede cruzar la medianoche.");
        }

        if (horario.cerrado()) {
            throw new ExcepcionNegocio(CodigoError.FUERA_DE_HORARIO, "La cancha está cerrada ese día.");
        }
        if (!horario.contiene(minutoInicio, minutoFin)) {
            throw new ExcepcionNegocio(CodigoError.FUERA_DE_HORARIO, "La reserva debe estar dentro del horario de la cancha ("
                    + MinutoDelDia.formatear(horario.minutoApertura()) + " a " + MinutoDelDia.formatear(horario.minutoCierre()) + ").");
        }
        if ((minutoInicio - horario.minutoApertura()) % cancha.bloqueMin() != 0) {
            throw invalida("La reserva debe empezar en un bloque de " + cancha.bloqueMin() + " minutos contados desde la apertura ("
                    + MinutoDelDia.formatear(horario.minutoApertura()) + ").");
        }
        int duracion = minutoFin - minutoInicio;
        if (duracion % cancha.bloqueMin() != 0 || duracion < cancha.minimaMin() || duracion > cancha.maximaMin()) {
            throw invalida("La duración debe ser múltiplo de " + cancha.bloqueMin() + " minutos, entre " + cancha.minimaMin()
                    + " y " + cancha.maximaMin() + " minutos.");
        }
        if (inicio.isBefore(ahora.plus(Duration.ofHours(politica.antelacionMinimaHoras())))) {
            throw new ExcepcionNegocio(CodigoError.FUERA_DE_VENTANA,
                    "Debes reservar con al menos " + politica.antelacionMinimaHoras() + " horas de antelación.");
        }
        if (inicio.isAfter(ahora.plus(Duration.ofDays(politica.antelacionMaximaDias())))) {
            throw new ExcepcionNegocio(CodigoError.FUERA_DE_VENTANA,
                    "Solo se puede reservar con hasta " + politica.antelacionMaximaDias() + " días de antelación.");
        }
    }

    private static ExcepcionNegocio invalida(String mensaje) {
        return new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, mensaje);
    }
}
