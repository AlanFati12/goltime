package co.edu.usco.goltime.sedes.dominio;

import java.time.DayOfWeek;

import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;

/** Horario de operación de un día de la semana (ISO: 1=lunes ... 7=domingo). */
public record HorarioDia(int diaSemana, boolean cerrado, int minutoApertura, int minutoCierre) {

    public HorarioDia {
        if (diaSemana < 1 || diaSemana > 7) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "El día de la semana debe estar entre 1 y 7.");
        }
        if (cerrado) {
            minutoApertura = 0;
            minutoCierre = 0;
        } else if (minutoApertura < 0 || minutoCierre > MinutoDelDia.FIN_DEL_DIA || minutoCierre <= minutoApertura) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS,
                    "La hora de cierre debe ser posterior a la de apertura (día " + diaSemana + ").");
        }
    }

    public static HorarioDia cerradoEl(int diaSemana) {
        return new HorarioDia(diaSemana, true, 0, 0);
    }

    public static HorarioDia abierto(int diaSemana, String apertura, String cierre) {
        return new HorarioDia(diaSemana, false, MinutoDelDia.analizar(apertura), MinutoDelDia.analizar(cierre));
    }

    public DayOfWeek dia() {
        return DayOfWeek.of(diaSemana);
    }

    /** ¿El intervalo [desde, hasta) en minutos del día cabe dentro del horario de operación? */
    public boolean contiene(int desde, int hasta) {
        return !cerrado && desde >= minutoApertura && hasta <= minutoCierre && hasta > desde;
    }
}
