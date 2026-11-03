package co.edu.usco.goltime.whatsapp.infraestructura;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import co.edu.usco.goltime.compartido.infraestructura.seguridad.RespuestasSeguridadJson;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Cadena de seguridad propia de {@code /api/v1/integracion/**}: solo entra quien trae la clave de integración
 * (la tiene n8n) en {@code X-Clave-Integracion}. Un JWT de cliente o de administrador no sirve aquí, y la
 * clave no sirve en ninguna otra ruta, porque esta cadena solo se aplica a este prefijo.
 */
@Configuration
public class SeguridadIntegracion {

    public static final String CABECERA_CLAVE = "X-Clave-Integracion";
    public static final String ROL = "INTEGRACION";

    @Bean
    @Order(1)
    SecurityFilterChain cadenaIntegracion(HttpSecurity http, PropiedadesWhatsapp propiedades, RespuestasSeguridadJson respuestas) throws Exception {
        http
                .securityMatcher("/api/v1/integracion/**")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new FiltroClaveIntegracion(propiedades), AnonymousAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole(ROL))
                .exceptionHandling(e -> e.authenticationEntryPoint(respuestas).accessDeniedHandler(respuestas));
        return http.build();
    }

    /** Compara la clave en tiempo constante; sin clave configurada no autentica a nadie. */
    static class FiltroClaveIntegracion extends OncePerRequestFilter {

        private final PropiedadesWhatsapp propiedades;

        FiltroClaveIntegracion(PropiedadesWhatsapp propiedades) {
            this.propiedades = propiedades;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
                throws ServletException, IOException {
            String recibida = peticion.getHeader(CABECERA_CLAVE);
            if (propiedades.integracionHabilitada() && recibida != null && MessageDigest.isEqual(
                    recibida.getBytes(StandardCharsets.UTF_8), propiedades.claveIntegracion().getBytes(StandardCharsets.UTF_8))) {
                SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                        "n8n", null, List.of(new SimpleGrantedAuthority("ROLE_" + ROL))));
            }
            cadena.doFilter(peticion, respuesta);
        }
    }
}
