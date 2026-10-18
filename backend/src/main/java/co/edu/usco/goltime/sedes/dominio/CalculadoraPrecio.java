package co.edu.usco.goltime.sedes.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.compartido.dominio.ZonaHoraria;
import co.edu.usco.goltime.sedes.dominio.DesglosePrecio.Origen;
import co.edu.usco.goltime.sedes.dominio.DesglosePrecio.Tramo;

/**
 * Cálculo del precio de una reserva, siempre en el backend. El intervalo se parte en cada cambio de
 * franja (y a medianoche) y cada tramo se cobra proporcionalmente a sus minutos:
 * {@code minutos / 60 * precioHora}. Prioridad por minuto: franja de festivo (si la fecha es festivo)
 * > franja normal del día > precio base de la cancha. El total se redondea al peso.
 */
public final class CalculadoraPrecio {

    private CalculadoraPrecio() {
    }

    public static DesglosePrecio calcular(int precioBaseHora, List<Tarifa> tarifas, Set<LocalDate> festivos,
            Instant inicio, Instant fin) {
        if (inicio == null || fin == null || !fin.isAfter(inicio)) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "La hora de fin debe ser posterior a la de inicio.");
        }
        LocalDateTime cursor = ZonaHoraria.aHoraLocal(inicio);
        LocalDateTime limite = ZonaHoraria.aHoraLocal(fin);

        List<Tramo> tramos = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        while (cursor.isBefore(limite)) {
            LocalDate fecha = cursor.toLocalDate();
            boolean festivo = festivos.contains(fecha);
            int minuto = cursor.getHour() * 60 + cursor.getMinute();

            Optional<Tarifa> aplicable = franjaQueRige(tarifas, fecha, festivo, minuto);
            int precioHora = aplicable.map(Tarifa::precioHora).orElse(precioBaseHora);
            Origen origen = aplicable.map(t -> t.aplicaFestivo() ? Origen.FESTIVO : Origen.TARIFA).orElse(Origen.BASE);

            int finDelTramo = minutoDondeCambia(tarifas, fecha, festivo, minuto);
            LocalDateTime finTramo = fecha.atStartOfDay().plusMinutes(finDelTramo);
            if (finTramo.isAfter(limite)) {
                finTramo = limite;
            }
            int minutos = (int) java.time.Duration.between(cursor, finTramo).toMinutes();

            tramos.add(new Tramo(ZonaHoraria.aInstante(fecha, cursor.toLocalTime()),
                    ZonaHoraria.aInstante(finTramo.toLocalDate(), finTramo.toLocalTime()), minutos, precioHora, origen));
            total = total.add(BigDecimal.valueOf(precioHora).multiply(BigDecimal.valueOf(minutos))
                    .divide(BigDecimal.valueOf(60), 6, RoundingMode.HALF_UP));
            cursor = finTramo;
        }
        return new DesglosePrecio(total.setScale(0, RoundingMode.HALF_UP).longValueExact(), List.copyOf(fusionar(tramos)));
    }

    /** Franja de festivo si la fecha es festivo y hay una que rija; si no, la franja normal del día. */
    private static Optional<Tarifa> franjaQueRige(List<Tarifa> tarifas, LocalDate fecha, boolean festivo, int minuto) {
        if (festivo) {
            Optional<Tarifa> deFestivo = tarifas.stream()
                    .filter(t -> t.aplicaFestivo() && t.rigeEn(fecha, true, minuto)).findFirst();
            if (deFestivo.isPresent()) {
                return deFestivo;
            }
        }
        return tarifas.stream().filter(t -> !t.aplicaFestivo() && t.rigeEn(fecha, false, minuto)).findFirst();
    }

    /** Primer minuto del día (posterior a {@code minuto}) en que puede cambiar la franja vigente. */
    private static int minutoDondeCambia(List<Tarifa> tarifas, LocalDate fecha, boolean festivo, int minuto) {
        int siguiente = MinutoDelDia.FIN_DEL_DIA;
        for (Tarifa t : tarifas) {
            if (!t.vigenteEn(fecha) || (t.aplicaFestivo() && !festivo)) {
                continue;
            }
            if (t.minutoInicio() > minuto) {
                siguiente = Math.min(siguiente, t.minutoInicio());
            }
            if (t.minutoFin() > minuto) {
                siguiente = Math.min(siguiente, t.minutoFin());
            }
        }
        return siguiente;
    }

    /** Une tramos contiguos con el mismo precio y origen para un desglose más legible. */
    private static List<Tramo> fusionar(List<Tramo> tramos) {
        List<Tramo> resultado = new ArrayList<>();
        for (Tramo t : tramos) {
            if (!resultado.isEmpty()) {
                Tramo ultimo = resultado.getLast();
                if (ultimo.precioHora() == t.precioHora() && ultimo.origen() == t.origen() && ultimo.fin().equals(t.inicio())) {
                    resultado.set(resultado.size() - 1,
                            new Tramo(ultimo.inicio(), t.fin(), ultimo.minutos() + t.minutos(), t.precioHora(), t.origen()));
                    continue;
                }
            }
            resultado.add(t);
        }
        return resultado;
    }
}
