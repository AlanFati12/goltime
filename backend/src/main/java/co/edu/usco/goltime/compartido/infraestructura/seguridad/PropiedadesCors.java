package co.edu.usco.goltime.compartido.infraestructura.seguridad;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Orígenes del frontend autorizados para llamar a la API ({@code goltime.cors.*}). */
@ConfigurationProperties("goltime.cors")
public record PropiedadesCors(List<String> origenesPermitidos) {
}
