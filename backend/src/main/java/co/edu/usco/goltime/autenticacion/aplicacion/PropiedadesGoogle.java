package co.edu.usco.goltime.autenticacion.aplicacion;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Ingreso con Google ({@code goltime.google.*}). {@code clientId} es el «ID de cliente» público de OAuth de Google
 * Cloud. {@code modoPrueba} acepta credenciales de mentira para desarrollar sin Google: solo existe en desarrollo y
 * pruebas, y la aplicación se niega a arrancar si se activa con el perfil de producción.
 */
@ConfigurationProperties("goltime.google")
public record PropiedadesGoogle(String clientId, boolean modoPrueba) {
}
