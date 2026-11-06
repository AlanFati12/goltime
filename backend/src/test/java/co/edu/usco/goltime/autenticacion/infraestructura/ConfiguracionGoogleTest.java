package co.edu.usco.goltime.autenticacion.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import co.edu.usco.goltime.autenticacion.aplicacion.PropiedadesGoogle;
import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;

class ConfiguracionGoogleTest {

    private final ConfiguracionGoogle configuracion = new ConfiguracionGoogle();

    private static MockEnvironment entorno(String... perfiles) {
        MockEnvironment e = new MockEnvironment();
        e.setActiveProfiles(perfiles);
        return e;
    }

    @Test
    void elModoDePruebaNuncaArrancaEnProduccion() {
        assertThatThrownBy(() -> configuracion.verificadorGoogle(new PropiedadesGoogle("", true), entorno("prod")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("producción");
    }

    @Test
    void enDesarrolloElModoDePruebaAceptaCredencialesDeMentira() {
        var verificador = configuracion.verificadorGoogle(new PropiedadesGoogle("", true), entorno("dev"));

        var identidad = verificador.verificar("prueba|Ana@Correo.com|Ana|Pérez");

        assertThat(identidad.correo()).isEqualTo("ana@correo.com");
        assertThat(identidad.correoVerificado()).isTrue();
        assertThat(verificador.verificar("noverificado|a@b.co").correoVerificado()).isFalse();
        assertThatThrownBy(() -> verificador.verificar("cualquier-cosa")).isInstanceOf(ExcepcionNegocio.class);
    }

    @Test
    void sinIdDeClienteNadiePuedeEntrarYElErrorLoDice() {
        var verificador = configuracion.verificadorGoogle(new PropiedadesGoogle("  ", false), entorno("prod"));

        assertThatThrownBy(() -> verificador.verificar("prueba|a@b.co"))
                .isInstanceOfSatisfying(ExcepcionNegocio.class, e -> assertThat(e.codigo()).isEqualTo(CodigoError.SERVICIO_NO_DISPONIBLE));
    }

    @Test
    void conIdDeClienteSeVerificaContraGoogleYUnTokenMalFormadoSeRechazaSinLlamarAlaRed() {
        var verificador = configuracion.verificadorGoogle(new PropiedadesGoogle("123-abc.apps.googleusercontent.com", false), entorno("prod"));

        assertThatThrownBy(() -> verificador.verificar("esto.no.esunjwt"))
                .isInstanceOfSatisfying(ExcepcionNegocio.class, e -> assertThat(e.codigo()).isEqualTo(CodigoError.CREDENCIALES_INVALIDAS));
    }
}
