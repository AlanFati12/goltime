package co.edu.usco.goltime.estadisticas.aplicacion;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.usco.goltime.compartido.dominio.ZonaHoraria;
import co.edu.usco.goltime.estadisticas.aplicacion.Tablero.Consultas;
import co.edu.usco.goltime.estadisticas.aplicacion.Tablero.Resumen;

/** Arma el tablero del administrador a partir de consultas de solo lectura. */
@Service
public class ServicioTablero {

    private static final int DIAS_PROXIMOS = 7;
    private static final int SALDOS_A_MOSTRAR = 5;
    private static final Duration VENCE_PRONTO = Duration.ofHours(3);

    private final Consultas consultas;
    private final Clock reloj;

    public ServicioTablero(Consultas consultas, Clock reloj) {
        this.consultas = consultas;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public Resumen resumen() {
        Instant ahora = reloj.instant();
        LocalDate hoy = ZonaHoraria.fechaLocal(ahora);
        Instant inicioDia = ZonaHoraria.inicioDelDia(hoy);
        Instant finDia = ZonaHoraria.inicioDelDia(hoy.plusDays(1));
        Instant inicioMes = ZonaHoraria.inicioDelDia(hoy.withDayOfMonth(1));
        return new Resumen(ahora, consultas.hoy(inicioDia, finDia), consultas.pendientesPorVencer(ahora, ahora.plus(VENCE_PRONTO)),
                consultas.cobros(ahora, SALDOS_A_MOSTRAR), consultas.proximosDias(hoy, DIAS_PROXIMOS), consultas.canchas(ahora, inicioDia, finDia),
                consultas.torneos(ahora), consultas.clientes(ahora.minus(Duration.ofDays(7)), inicioMes));
    }
}
