package co.edu.usco.goltime.autenticacion.infraestructura;

import java.time.Duration;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.client.RestTemplate;

import co.edu.usco.goltime.autenticacion.aplicacion.IdentidadGoogle;
import co.edu.usco.goltime.autenticacion.aplicacion.VerificadorGoogle;
import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;

/**
 * Verifica el token de identidad de Google contra sus claves públicas (JWKS): firma, emisor, destinatario (que sea
 * de ESTA aplicación, por el ID de cliente) y vigencia. No hace falta el secreto del cliente: nada se pide a Google
 * en nombre de la persona, solo se comprueba una firma.
 */
final class VerificadorGoogleNimbus implements VerificadorGoogle {

    private static final Logger log = LoggerFactory.getLogger(VerificadorGoogleNimbus.class);

    private static final String CLAVES_PUBLICAS = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> EMISORES = Set.of("https://accounts.google.com", "accounts.google.com");

    private final NimbusJwtDecoder decodificador;

    VerificadorGoogleNimbus(String clientId) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(3));
        fabrica.setReadTimeout(Duration.ofSeconds(5));
        this.decodificador = NimbusJwtDecoder.withJwkSetUri(CLAVES_PUBLICAS).restOperations(new RestTemplate(fabrica)).build();
        OAuth2TokenValidator<Jwt> emisor = jwt -> EMISORES.contains(String.valueOf(jwt.getClaims().get("iss")))
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("emisor_invalido"));
        OAuth2TokenValidator<Jwt> destinatario = jwt -> jwt.getAudience() != null && jwt.getAudience().contains(clientId)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("destinatario_invalido"));
        decodificador.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(Duration.ofSeconds(60)), emisor, destinatario));
    }

    @Override
    public IdentidadGoogle verificar(String credencial) {
        Jwt jwt;
        try {
            jwt = decodificador.decode(credencial);
        } catch (JwtException e) {
            // Solo el motivo (firma, destinatario, vigencia...), nunca el token.
            log.warn("Credencial de Google rechazada: {}", e.getMessage());
            throw new ExcepcionNegocio(CodigoError.CREDENCIALES_INVALIDAS, "No pudimos verificar tu cuenta de Google. Intenta de nuevo.");
        }
        String correo = jwt.getClaimAsString("email");
        if (jwt.getSubject() == null || correo == null) {
            throw new ExcepcionNegocio(CodigoError.CREDENCIALES_INVALIDAS, "Google no compartió el correo de tu cuenta.");
        }
        Object verificado = jwt.getClaims().get("email_verified");
        boolean correoVerificado = Boolean.TRUE.equals(verificado) || "true".equals(String.valueOf(verificado));
        String completo = jwt.getClaimAsString("name");
        String nombres = jwt.getClaimAsString("given_name");
        String apellidos = jwt.getClaimAsString("family_name");
        if (nombres == null || nombres.isBlank()) {
            // Sin nombre de pila separado: se parte el nombre completo, o se usa la parte local del correo.
            String base = completo != null && !completo.isBlank() ? completo.trim() : correo.substring(0, correo.indexOf('@'));
            int espacio = base.indexOf(' ');
            nombres = espacio < 0 ? base : base.substring(0, espacio);
            apellidos = espacio < 0 ? "" : base.substring(espacio + 1);
        }
        return new IdentidadGoogle(jwt.getSubject(), correo, correoVerificado, nombres, apellidos == null ? "" : apellidos);
    }
}
