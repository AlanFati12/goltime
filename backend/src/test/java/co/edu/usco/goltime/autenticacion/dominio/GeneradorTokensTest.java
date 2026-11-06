package co.edu.usco.goltime.autenticacion.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class GeneradorTokensTest {

    @Test
    void generaTokensDistintosYLargos() {
        String a = GeneradorTokens.generar();
        String b = GeneradorTokens.generar();
        assertThat(a).isNotEqualTo(b).hasSizeGreaterThanOrEqualTo(43);
    }

    @Test
    void elHashEsSha256Determinista() {
        assertThat(GeneradorTokens.hash("hola"))
                .isEqualTo("b221d9dbb083a7f33428d7c2a3c3198ae925614d70210e28716ccaa7cd4ddb79")
                .hasSize(64);
    }

    @Test
    void unTokenRefrescoRotadoOExpiradoSeDetecta() {
        Instant ahora = Instant.parse("2026-10-18T10:00:00Z");
        TokenRefresco token = TokenRefresco.nuevo(1L, UUID.randomUUID(), "h", ahora.plusSeconds(60), false, null, null);
        assertThat(token.fueUsado()).isFalse();
        assertThat(token.estaExpirado(ahora)).isFalse();
        assertThat(token.estaExpirado(ahora.plusSeconds(60))).isTrue();

        token.rotarA(5L);
        assertThat(token.fueUsado()).isTrue();
        assertThat(token.revocado()).isTrue();
    }

}
