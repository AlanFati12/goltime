package co.edu.usco.goltime.usuarios.dominio;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;

class PoliticaClaveTest {

    @Test
    void aceptaUnaClaveConLetrasYNumeros() {
        assertThatCode(() -> PoliticaClave.validar("Clave1234")).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "abc12", "soloLetrasAqui", "123456789", "abcdefg!"})
    void rechazaClavesCortasOSinLetraONumero(String clave) {
        assertThatThrownBy(() -> PoliticaClave.validar(clave)).isInstanceOf(ExcepcionNegocio.class);
    }

    @Test
    void rechazaClavesMasLargasQueElLimiteDeBcrypt() {
        assertThatThrownBy(() -> PoliticaClave.validar("a1".repeat(40))).isInstanceOf(ExcepcionNegocio.class);
    }
}
