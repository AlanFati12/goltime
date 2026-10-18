package co.edu.usco.goltime.compartido.infraestructura.seguridad;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import co.edu.usco.goltime.compartido.aplicacion.ConsultaEstadoUsuario;

/**
 * Seguridad base: API sin estado, JWT firmado con HS256 validado por el resource server de Spring
 * (Nimbus), roles ADMIN/CLIENTE y errores 401/403 en formato ProblemDetail.
 *
 * <p>Convención de rutas: lo público de solo lectura vive bajo {@code /api/v1/publico/**},
 * la autenticación bajo {@code /api/v1/autenticacion/**} y lo administrativo bajo
 * {@code /api/v1/admin/**}. Todo lo demás exige un token válido.
 */
@Configuration
@EnableMethodSecurity
public class ConfiguracionSeguridad {

    public static final String RECLAMO_ROL = "rol";

    @Bean
    SecurityFilterChain cadenaDeSeguridad(HttpSecurity http, RespuestasSeguridadJson respuestas)
            throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/v1/publico/**").permitAll()
                        .requestMatchers("/api/v1/autenticacion/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(convertidorDeJwt()))
                        .authenticationEntryPoint(respuestas)
                        .accessDeniedHandler(respuestas))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(respuestas)
                        .accessDeniedHandler(respuestas))
                .headers(h -> h
                        .contentTypeOptions(Customizer.withDefaults())
                        .frameOptions(f -> f.deny()));
        return http.build();
    }

    /** Convierte el reclamo {@code rol} del token en la autoridad {@code ROLE_<rol>}. */
    private JwtAuthenticationConverter convertidorDeJwt() {
        JwtGrantedAuthoritiesConverter autoridades = new JwtGrantedAuthoritiesConverter();
        autoridades.setAuthoritiesClaimName(RECLAMO_ROL);
        autoridades.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter convertidor = new JwtAuthenticationConverter();
        convertidor.setJwtGrantedAuthoritiesConverter(autoridades);
        return convertidor;
    }

    private SecretKey claveJwt(PropiedadesSeguridad propiedades) {
        return new SecretKeySpec(propiedades.jwtSecreto().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtDecoder decodificadorJwt(PropiedadesSeguridad propiedades, ConsultaEstadoUsuario estadoUsuario) {
        NimbusJwtDecoder decodificador = NimbusJwtDecoder.withSecretKey(claveJwt(propiedades))
                .macAlgorithm(MacAlgorithm.HS256).build();
        decodificador.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(), new ValidadorEstadoUsuario(estadoUsuario)));
        return decodificador;
    }

    @Bean
    JwtEncoder codificadorJwt(PropiedadesSeguridad propiedades) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(claveJwt(propiedades)));
    }

    /** BCrypt con costo 12 como algoritmo por defecto, delegando por prefijo para poder migrar luego. */
    @Bean
    PasswordEncoder codificadorDeClaves() {
        String idPorDefecto = "bcrypt";
        Map<String, PasswordEncoder> codificadores = new HashMap<>();
        codificadores.put(idPorDefecto, new BCryptPasswordEncoder(12));
        return new DelegatingPasswordEncoder(idPorDefecto, codificadores);
    }

    @Bean
    CorsConfigurationSource configuracionCors(PropiedadesCors propiedades) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(propiedades.origenesPermitidos());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key"));
        cors.setExposedHeaders(List.of("Content-Disposition"));
        cors.setAllowCredentials(true);
        cors.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", cors);
        return fuente;
    }
}
