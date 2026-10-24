package co.edu.usco.goltime.whatsapp.aplicacion;

import org.springframework.stereotype.Component;

import co.edu.usco.goltime.compartido.aplicacion.LectorConfiguracion;

/** Interruptores del bot que el administrador cambia desde Configuración (V14). */
@Component
public class ConfiguracionBot {

    private final LectorConfiguracion configuracion;

    public ConfiguracionBot(LectorConfiguracion configuracion) {
        this.configuracion = configuracion;
    }

    public boolean activo() {
        return configuracion.booleano("whatsapp_bot_activo", true);
    }

    public boolean reservasSinCuenta() {
        return configuracion.booleano("whatsapp_reservas_sin_cuenta", true);
    }

    public String contactoNegocio() {
        return configuracion.texto("contacto_negocio").map(String::trim).filter(t -> !t.isEmpty()).orElse(null);
    }
}
