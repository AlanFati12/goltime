package co.edu.usco.goltime.compartido.aplicacion;

import java.util.Optional;

/** Puerto de lectura de la configuración general del negocio (tabla {@code configuracion}). */
public interface LectorConfiguracion {

    Optional<String> texto(String clave);

    default int entero(String clave, int pordefecto) {
        return texto(clave).map(String::trim).filter(v -> v.matches("-?\\d+")).map(Integer::parseInt).orElse(pordefecto);
    }

    default boolean booleano(String clave, boolean pordefecto) {
        return texto(clave).map(v -> v.trim().equalsIgnoreCase("true")).orElse(pordefecto);
    }
}
