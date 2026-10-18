package co.edu.usco.goltime.sedes.dominio;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;

/** Horario de operación de la semana: un {@link HorarioDia} por cada día (1 a 7), sin repetir. */
public record HorarioSemanal(List<HorarioDia> dias) {

    public HorarioSemanal {
        Set<Integer> vistos = new HashSet<>();
        for (HorarioDia d : dias) {
            if (!vistos.add(d.diaSemana())) {
                throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "El día " + d.diaSemana() + " está repetido en el horario.");
            }
        }
        dias = dias.stream().sorted((a, b) -> Integer.compare(a.diaSemana(), b.diaSemana())).toList();
    }

    /** Horario por defecto del negocio: lunes a sábado 8:00-23:00 y domingo 8:00-20:00. */
    public static HorarioSemanal porDefecto() {
        List<HorarioDia> dias = new ArrayList<>();
        for (int d = 1; d <= 6; d++) {
            dias.add(HorarioDia.abierto(d, "08:00", "23:00"));
        }
        dias.add(HorarioDia.abierto(7, "08:00", "20:00"));
        return new HorarioSemanal(dias);
    }

    public Optional<HorarioDia> deDia(DayOfWeek dia) {
        return dias.stream().filter(d -> d.diaSemana() == dia.getValue()).findFirst();
    }

    public boolean estaVacio() {
        return dias.isEmpty();
    }

    /**
     * Horario que aplica a una cancha: el propio de la cancha si lo tiene definido para ese día;
     * si no, el de la sede. Un día sin horario definido se considera cerrado.
     */
    public static HorarioDia efectivo(DayOfWeek dia, HorarioSemanal sede, HorarioSemanal canchaPropio) {
        if (canchaPropio != null) {
            Optional<HorarioDia> propio = canchaPropio.deDia(dia);
            if (propio.isPresent()) {
                return propio.get();
            }
        }
        return sede.deDia(dia).orElse(HorarioDia.cerradoEl(dia.getValue()));
    }
}
