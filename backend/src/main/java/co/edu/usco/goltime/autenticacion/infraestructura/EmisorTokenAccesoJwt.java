package co.edu.usco.goltime.autenticacion.infraestructura;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import co.edu.usco.goltime.autenticacion.aplicacion.EmisorTokenAcceso;
import co.edu.usco.goltime.autenticacion.aplicacion.PropiedadesAutenticacion;
import co.edu.usco.goltime.autenticacion.aplicacion.TokenAcceso;
import co.edu.usco.goltime.compartido.infraestructura.seguridad.ConfiguracionSeguridad;
import co.edu.usco.goltime.compartido.infraestructura.seguridad.ValidadorEstadoUsuario;
import co.edu.usco.goltime.usuarios.dominio.Usuario;

/** Emite el access token: sub (id), rol, tv (version_token), jti, iat y exp. */
@Component
class EmisorTokenAccesoJwt implements EmisorTokenAcceso {

    private final JwtEncoder codificador;
    private final PropiedadesAutenticacion propiedades;
    private final Clock reloj;

    EmisorTokenAccesoJwt(JwtEncoder codificador, PropiedadesAutenticacion propiedades, Clock reloj) {
        this.codificador = codificador;
        this.propiedades = propiedades;
        this.reloj = reloj;
    }

    @Override
    public TokenAcceso emitir(Usuario usuario) {
        Instant ahora = reloj.instant();
        Duration vigencia = Duration.ofMinutes(propiedades.accesoMinutos());
        JwtClaimsSet reclamos = JwtClaimsSet.builder()
                .issuer("goltime")
                .subject(String.valueOf(usuario.id()))
                .claim(ConfiguracionSeguridad.RECLAMO_ROL, usuario.rol().name())
                .claim(ValidadorEstadoUsuario.RECLAMO_VERSION, usuario.versionToken())
                .id(UUID.randomUUID().toString())
                .issuedAt(ahora)
                .expiresAt(ahora.plus(vigencia))
                .build();
        String valor = codificador.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), reclamos))
                .getTokenValue();
        return new TokenAcceso(valor, vigencia.toSeconds());
    }
}
