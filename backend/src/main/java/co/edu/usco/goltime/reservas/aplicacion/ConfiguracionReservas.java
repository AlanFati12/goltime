package co.edu.usco.goltime.reservas.aplicacion;

import org.springframework.stereotype.Component;

import co.edu.usco.goltime.compartido.aplicacion.LectorConfiguracion;
import co.edu.usco.goltime.reservas.dominio.PoliticaReservas;

/** Lee de la configuración del negocio las reglas de reservas (con los valores por defecto aprobados). */
@Component
public class ConfiguracionReservas {

    private final LectorConfiguracion configuracion;

    public ConfiguracionReservas(LectorConfiguracion configuracion) {
        this.configuracion = configuracion;
    }

    public PoliticaReservas politica() {
        PoliticaReservas d = PoliticaReservas.porDefecto();
        return new PoliticaReservas(
                configuracion.entero("horas_minimas_cancelacion", d.horasMinCancelacion()),
                configuracion.entero("max_reprogramaciones", d.maxReprogramaciones()),
                configuracion.entero("antelacion_minima_horas", d.antelacionMinimaHoras()),
                configuracion.entero("antelacion_maxima_dias", d.antelacionMaximaDias()),
                configuracion.entero("max_reservas_activas_cliente", d.maxReservasActivasCliente()),
                configuracion.entero("horas_expiracion_aprobacion", d.horasExpiracionAprobacion()),
                configuracion.entero("horas_expiracion_antes_inicio", d.horasExpiracionAntesInicio()),
                configuracion.booleano("completar_reservas_automaticamente", d.completarAutomaticamente()),
                configuracion.entero("horas_recordatorio", d.horasRecordatorio()));
    }
}
