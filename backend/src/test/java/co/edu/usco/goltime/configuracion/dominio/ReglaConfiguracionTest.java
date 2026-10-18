package co.edu.usco.goltime.configuracion.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;

class ReglaConfiguracionTest {

    private static ReglaConfiguracion regla(String clave) {
        return ReglaConfiguracion.de(clave).orElseThrow();
    }

    @Test
    void losEnterosSeValidanContraSuRango() {
        ReglaConfiguracion r = regla("max_reprogramaciones");

        assertThat(r.validar("max_reprogramaciones", " 3 ")).isEqualTo("3");
        assertThat(r.validar("max_reprogramaciones", "0")).isEqualTo("0");
        assertThatThrownBy(() -> r.validar("max_reprogramaciones", "11")).isInstanceOf(ExcepcionNegocio.class).hasMessageContaining("entre 0 y 10");
        assertThatThrownBy(() -> r.validar("max_reprogramaciones", "-1")).isInstanceOf(ExcepcionNegocio.class);
        assertThatThrownBy(() -> r.validar("max_reprogramaciones", "dos")).isInstanceOf(ExcepcionNegocio.class).hasMessageContaining("número entero");
        assertThatThrownBy(() -> r.validar("max_reprogramaciones", "")).isInstanceOf(ExcepcionNegocio.class);
    }

    @Test
    void losBooleanosSeNormalizan() {
        ReglaConfiguracion r = regla("completar_reservas_automaticamente");

        assertThat(r.validar("completar_reservas_automaticamente", "TRUE")).isEqualTo("true");
        assertThat(r.validar("completar_reservas_automaticamente", " false ")).isEqualTo("false");
        assertThatThrownBy(() -> r.validar("completar_reservas_automaticamente", "si")).isInstanceOf(ExcepcionNegocio.class);
    }

    @Test
    void lasModalidadesDebenSerNumerosSeparadosPorComa() {
        ReglaConfiguracion r = regla("modalidades_permitidas");

        assertThat(r.validar("modalidades_permitidas", "5,6,11")).isEqualTo("5,6,11");
        assertThatThrownBy(() -> r.validar("modalidades_permitidas", "5;6")).isInstanceOf(ExcepcionNegocio.class);
        assertThatThrownBy(() -> r.validar("modalidades_permitidas", "cinco")).isInstanceOf(ExcepcionNegocio.class);
    }

    @Test
    void losTextosTienenUnLimiteYUnaClaveDesconocidaNoEsEditable() {
        assertThat(regla("contacto_negocio").validar("contacto_negocio", " 310 123 4567 ")).isEqualTo("310 123 4567");
        assertThatThrownBy(() -> regla("contacto_negocio").validar("contacto_negocio", "x".repeat(1001))).isInstanceOf(ExcepcionNegocio.class);
        assertThat(ReglaConfiguracion.de("clave_inventada")).isEmpty();
    }
}
