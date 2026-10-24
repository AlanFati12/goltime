package co.edu.usco.goltime.compartido.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

class NumeroTelefonoTest {

    @ParameterizedTest
    @ValueSource(strings = {"3001234567", "300 123 4567", "300-123-4567", "+57 300 123 4567", "+573001234567",
            "573001234567", "0057 3001234567", "(+57) 300.123.4567"})
    void todasLasFormasDelMismoCelularColombianoQuedanIguales(String texto) {
        assertThat(NumeroTelefono.de(texto).valor()).isEqualTo("+573001234567");
    }

    @Test
    void aceptaNumerosDeOtrosPaisesConIndicativo() {
        assertThat(NumeroTelefono.de("+58 412 555 1234").valor()).isEqualTo("+584125551234");
        assertThat(NumeroTelefono.de("+1 (305) 555-0101").valor()).isEqualTo("+13055550101");
    }

    @ParameterizedTest
    @ValueSource(strings = {"8712345", "6018765432", "+57 601 876 5432", "30012345", "+5730012345671", "abc3001234567", "+0123456789"})
    void rechazaLoQueNoEsUnCelularValido(String texto) {
        assertThatThrownBy(() -> NumeroTelefono.de(texto))
                .isInstanceOf(ExcepcionNegocio.class)
                .extracting("codigo").isEqualTo(CodigoError.DATOS_INVALIDOS);
    }

    @Test
    void vacioEsObligatorioSalvoEnElOpcional() {
        assertThatThrownBy(() -> NumeroTelefono.de("  ")).isInstanceOf(ExcepcionNegocio.class);
        assertThat(NumeroTelefono.normalizarOpcional(" ")).isNull();
        assertThat(NumeroTelefono.normalizarOpcional("3001234567")).isEqualTo("+573001234567");
    }

    @Test
    void seMuestraAgrupadoParaColombia() {
        assertThat(NumeroTelefono.de("3001234567").legible()).isEqualTo("+57 300 123 4567");
        assertThat(NumeroTelefono.de("+584125551234").legible()).isEqualTo("+584125551234");
    }
}
