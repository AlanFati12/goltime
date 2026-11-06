package co.edu.usco.goltime.autenticacion.aplicacion;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Parámetros de autenticación ({@code goltime.autenticacion.*}). */
@ConfigurationProperties("goltime.autenticacion")
public record PropiedadesAutenticacion(
        int accesoMinutos,
        int refrescoDias,
        int refrescoSesionHoras,
        String urlPublica) {
}
