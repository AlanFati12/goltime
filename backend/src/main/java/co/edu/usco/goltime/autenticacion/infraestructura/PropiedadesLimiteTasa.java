package co.edu.usco.goltime.autenticacion.infraestructura;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Límites de peticiones por IP ({@code goltime.limite-tasa.*}). */
@ConfigurationProperties("goltime.limite-tasa")
record PropiedadesLimiteTasa(long ingresoPorMinuto) {
}
