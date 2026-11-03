package co.edu.usco.goltime.whatsapp.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EnviadorWhatsappN8nTest {

    @Test
    void laFirmaEsHmacSha256EnHexadecimal() {
        // Vector de referencia de HMAC-SHA256 (Wikipedia, RFC 2104).
        assertThat(EnviadorWhatsappN8n.firmar("The quick brown fox jumps over the lazy dog", "key"))
                .isEqualTo("f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8");
    }
}
