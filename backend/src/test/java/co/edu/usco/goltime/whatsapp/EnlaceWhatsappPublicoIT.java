package co.edu.usco.goltime.whatsapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.jayway.jsonpath.JsonPath;

import co.edu.usco.goltime.BaseIntegracion;

/** La web pública solo muestra el botón de WhatsApp cuando el bot está activo y tiene número. */
class EnlaceWhatsappPublicoIT extends BaseIntegracion {

    private static final String RUTA = "/api/v1/publico/whatsapp";

    @AfterEach
    void restaurar() {
        jdbc.update("UPDATE configuracion SET valor = '' WHERE clave = 'whatsapp_numero_negocio'");
        jdbc.update("UPDATE configuracion SET valor = 'true' WHERE clave = 'whatsapp_bot_activo'");
    }

    private String consultar() throws Exception {
        return mvc.perform(get(RUTA)).andReturn().getResponse().getContentAsString();
    }

    @Test
    void sinNumeroConfiguradoNoHayBoton() throws Exception {
        assertThat((Boolean) JsonPath.read(consultar(), "$.disponible")).isFalse();
    }

    @Test
    void conNumeroYBotActivoDaElEnlaceDelChat() throws Exception {
        jdbc.update("UPDATE configuracion SET valor = '+573239085762' WHERE clave = 'whatsapp_numero_negocio'");
        String r = consultar();
        assertThat((Boolean) JsonPath.read(r, "$.disponible")).isTrue();
        assertThat((String) JsonPath.read(r, "$.enlace")).startsWith("https://wa.me/573239085762?text=");
    }

    @Test
    void conElBotApagadoNoHayBoton() throws Exception {
        jdbc.update("UPDATE configuracion SET valor = '+573239085762' WHERE clave = 'whatsapp_numero_negocio'");
        jdbc.update("UPDATE configuracion SET valor = 'false' WHERE clave = 'whatsapp_bot_activo'");
        assertThat((Boolean) JsonPath.read(consultar(), "$.disponible")).isFalse();
    }
}
