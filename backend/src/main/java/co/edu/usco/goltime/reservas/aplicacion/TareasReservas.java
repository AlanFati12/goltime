package co.edu.usco.goltime.reservas.aplicacion;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import co.edu.usco.goltime.reservas.dominio.PoliticaReservas;

/**
 * Tareas programadas (actor SISTEMA): expirar solicitudes sin respuesta, pasar a COMPLETADA las reservas
 * confirmadas ya jugadas y recordar a los clientes. Cada reserva se procesa en su propia transacción:
 * si una falla, las demás siguen.
 */
@Component
public class TareasReservas {

    private static final Logger log = LoggerFactory.getLogger(TareasReservas.class);
    private static final int LOTE = 200;

    private final RepositorioReservas reservas;
    private final ServicioReservas servicio;
    private final ConfiguracionReservas configuracion;
    private final Clock reloj;

    public TareasReservas(RepositorioReservas reservas, ServicioReservas servicio, ConfiguracionReservas configuracion, Clock reloj) {
        this.reservas = reservas;
        this.servicio = servicio;
        this.configuracion = configuracion;
        this.reloj = reloj;
    }

    @Scheduled(fixedDelayString = "${goltime.reservas.intervalo-tareas-ms:60000}", initialDelayString = "${goltime.reservas.intervalo-tareas-ms:60000}")
    public void ejecutarCiclo() {
        expirarSolicitudes();
        completarTerminadas();
        enviarRecordatorios();
    }

    public int expirarSolicitudes() {
        int procesadas = 0;
        for (long id : reservas.idsPendientesVencidas(reloj.instant(), LOTE)) {
            try {
                servicio.expirar(id);
                procesadas++;
            } catch (RuntimeException ex) {
                log.warn("No se pudo expirar la reserva {}: {}", id, ex.getMessage());
            }
        }
        return procesadas;
    }

    public int completarTerminadas() {
        if (!configuracion.politica().completarAutomaticamente()) {
            return 0;
        }
        int procesadas = 0;
        for (long id : reservas.idsConfirmadasTerminadas(reloj.instant(), LOTE)) {
            try {
                servicio.completar(id);
                procesadas++;
            } catch (RuntimeException ex) {
                log.warn("No se pudo completar la reserva {}: {}", id, ex.getMessage());
            }
        }
        return procesadas;
    }

    public int enviarRecordatorios() {
        PoliticaReservas politica = configuracion.politica();
        Instant ahora = reloj.instant();
        int procesadas = 0;
        for (long id : reservas.idsPorRecordar(ahora, ahora.plus(Duration.ofHours(politica.horasRecordatorio())), LOTE)) {
            try {
                servicio.recordar(id);
                procesadas++;
            } catch (RuntimeException ex) {
                log.warn("No se pudo recordar la reserva {}: {}", id, ex.getMessage());
            }
        }
        return procesadas;
    }
}
