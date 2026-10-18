package co.edu.usco.goltime.autenticacion.infraestructura;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import co.edu.usco.goltime.autenticacion.aplicacion.ControlIntentos;
import co.edu.usco.goltime.autenticacion.aplicacion.PropiedadesAutenticacion;

/**
 * Cuenta fallos de ingreso en memoria por clave (correo + IP). Tras {@code intentosMaximos} fallos
 * dentro de la ventana, bloquea esa clave durante {@code bloqueoMinutos}. La clave incluye la IP
 * para que un tercero no pueda bloquear la cuenta de otra persona desde otra red.
 */
@Component
class ControlIntentosMemoria implements ControlIntentos {

    private static final int LIMITE_ENTRADAS = 50_000;

    private final ConcurrentHashMap<String, Registro> registros = new ConcurrentHashMap<>();
    private final PropiedadesAutenticacion propiedades;
    private final Clock reloj;

    ControlIntentosMemoria(PropiedadesAutenticacion propiedades, Clock reloj) {
        this.propiedades = propiedades;
        this.reloj = reloj;
    }

    @Override
    public boolean estaBloqueado(String clave) {
        Registro registro = registros.get(clave);
        if (registro == null) {
            return false;
        }
        Instant ahora = reloj.instant();
        if (registro.bloqueadoHasta != null && registro.bloqueadoHasta.isAfter(ahora)) {
            return true;
        }
        if (registro.expiro(ahora)) {
            registros.remove(clave, registro);
        }
        return false;
    }

    @Override
    public void registrarFallo(String clave) {
        if (registros.size() > LIMITE_ENTRADAS) {
            registros.clear();
        }
        Instant ahora = reloj.instant();
        Duration bloqueo = Duration.ofMinutes(propiedades.bloqueoMinutos());
        registros.compute(clave, (k, actual) -> {
            Registro registro = (actual == null || actual.expiro(ahora)) ? new Registro(ahora.plus(bloqueo)) : actual;
            registro.fallos++;
            if (registro.fallos >= propiedades.intentosMaximos()) {
                registro.bloqueadoHasta = ahora.plus(bloqueo);
                registro.ventanaHasta = registro.bloqueadoHasta;
            }
            return registro;
        });
    }

    @Override
    public void limpiar(String clave) {
        registros.remove(clave);
    }

    private static final class Registro {
        int fallos;
        Instant ventanaHasta;
        Instant bloqueadoHasta;

        Registro(Instant ventanaHasta) {
            this.ventanaHasta = ventanaHasta;
        }

        boolean expiro(Instant ahora) {
            return !ventanaHasta.isAfter(ahora);
        }
    }
}
