package co.edu.usco.goltime.reservas.dominio;

import java.time.Duration;
import java.time.Instant;

/**
 * Parámetros configurables del negocio que gobiernan las reservas (tabla {@code configuracion}).
 *
 * @param horasMinCancelacion        horas mínimas antes del inicio para cancelar/reprogramar sin penalidad
 * @param maxReprogramaciones        máximo de reprogramaciones por reserva (cliente)
 * @param antelacionMinimaHoras      con cuánta antelación mínima se puede reservar
 * @param antelacionMaximaDias       con cuánta antelación máxima se puede reservar
 * @param maxReservasActivasCliente  reservas activas simultáneas por cliente
 * @param horasExpiracionAprobacion  horas que una solicitud espera aprobación
 * @param horasExpiracionAntesInicio una solicitud también expira esas horas antes de su inicio
 * @param completarAutomaticamente   pasar a COMPLETADA las confirmadas ya terminadas
 * @param horasRecordatorio          horas antes del inicio para recordar al cliente
 */
public record PoliticaReservas(int horasMinCancelacion, int maxReprogramaciones, int antelacionMinimaHoras,
        int antelacionMaximaDias, int maxReservasActivasCliente, int horasExpiracionAprobacion,
        int horasExpiracionAntesInicio, boolean completarAutomaticamente, int horasRecordatorio) {

    /** Valores por defecto aprobados: 4 h para cancelar, 2 reprogramaciones, solicitudes que expiran a las 12 h o 2 h antes. */
    public static PoliticaReservas porDefecto() {
        return new PoliticaReservas(4, 2, 3, 30, 5, 12, 2, true, 2);
    }

    /** Una solicitud expira a las N horas de creada o M horas antes del inicio, lo que ocurra primero. */
    public Instant calcularExpiracion(Instant ahora, Instant inicio) {
        Instant porTiempo = ahora.plus(Duration.ofHours(horasExpiracionAprobacion));
        Instant porInicio = inicio.minus(Duration.ofHours(horasExpiracionAntesInicio));
        return porTiempo.isBefore(porInicio) ? porTiempo : porInicio;
    }

    /** ¿Faltan al menos las horas mínimas para el inicio? (cancelación sin penalidad / reprogramación) */
    public boolean conAntelacionSuficiente(Instant ahora, Instant inicio) {
        return !Duration.between(ahora, inicio).minus(Duration.ofHours(horasMinCancelacion)).isNegative();
    }
}
