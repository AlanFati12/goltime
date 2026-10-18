package co.edu.usco.goltime.compartido.infraestructura.persistencia;

import java.time.Clock;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/** Auditoría JPA: el autor es el {@code sub} del JWT (id del usuario); sin token queda nulo (sistema). */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorActual", dateTimeProviderRef = "proveedorFechaAuditoria")
public class ConfiguracionAuditoria {

    @Bean
    Clock reloj() {
        return Clock.systemUTC();
    }

    @Bean
    DateTimeProvider proveedorFechaAuditoria(Clock reloj) {
        return () -> Optional.of(reloj.instant());
    }

    @Bean
    AuditorAware<Long> auditorActual() {
        return () -> {
            Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
            if (autenticacion != null && autenticacion.getPrincipal() instanceof Jwt jwt) {
                try {
                    return Optional.of(Long.valueOf(jwt.getSubject()));
                } catch (NumberFormatException ex) {
                    return Optional.empty();
                }
            }
            return Optional.empty();
        };
    }
}
