package co.edu.usco.goltime.compartido.infraestructura.seguridad;

import java.nio.charset.StandardCharsets;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parámetros de seguridad leídos de {@code goltime.seguridad.*}. El secreto del JWT
 * llega siempre por variable de entorno (nunca queda en el código).
 */
@ConfigurationProperties("goltime.seguridad")
public record PropiedadesSeguridad(
        String jwtSecreto,
        int accesoMinutos,
        int refrescoDias,
        boolean cookieSegura) {

    private static final int MINIMO_BYTES_SECRETO = 32;

    public PropiedadesSeguridad {
        if (jwtSecreto == null || jwtSecreto.getBytes(StandardCharsets.UTF_8).length < MINIMO_BYTES_SECRETO) {
            throw new IllegalStateException(
                    "JWT_SECRETO debe tener al menos 32 caracteres (256 bits) para firmar con HS256");
        }
    }
}
