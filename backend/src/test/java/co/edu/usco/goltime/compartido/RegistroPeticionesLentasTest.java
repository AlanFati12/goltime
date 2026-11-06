package co.edu.usco.goltime.compartido;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import co.edu.usco.goltime.compartido.infraestructura.web.RegistroPeticionesLentas;

class RegistroPeticionesLentasTest {

    private final ListAppender<ILoggingEvent> registro = new ListAppender<>();
    private final Logger logger = (Logger) LoggerFactory.getLogger(RegistroPeticionesLentas.class);

    @BeforeEach
    void escuchar() {
        registro.start();
        logger.addAppender(registro);
    }

    @AfterEach
    void dejarDeEscuchar() {
        logger.detachAppender(registro);
    }

    private void atender(RegistroPeticionesLentas filtro, String ruta, long duracionMs) throws Exception {
        MockHttpServletRequest peticion = new MockHttpServletRequest("GET", ruta);
        peticion.setQueryString("busqueda=juan%40correo.com");
        filtro.doFilter(peticion, new MockHttpServletResponse(), (rq, rs) -> {
            try {
                Thread.sleep(duracionMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    @Test
    void advierte_de_las_peticiones_que_pasan_el_umbral_sin_registrar_la_consulta() throws Exception {
        atender(new RegistroPeticionesLentas(20), "/api/v1/admin/clientes", 60);

        // Otras pruebas de la misma ejecución pueden dejar avisos propios en el mismo registro: solo importan los de esta.
        var propios = registro.list.stream().filter(e -> e.getFormattedMessage().contains("/api/v1/admin/clientes")).toList();
        assertThat(propios).hasSize(1);
        ILoggingEvent evento = propios.get(0);
        assertThat(evento.getLevel()).isEqualTo(Level.WARN);
        assertThat(evento.getFormattedMessage()).contains("GET /api/v1/admin/clientes").doesNotContain("juan");
    }

    @Test
    void no_dice_nada_de_las_rapidas_ni_de_actuator() throws Exception {
        atender(new RegistroPeticionesLentas(500), "/api/v1/publico/sedes", 1);
        atender(new RegistroPeticionesLentas(0), "/actuator/health", 1);

        assertThat(registro.list.stream().filter(e -> e.getFormattedMessage().contains("/api/v1/publico/sedes") || e.getFormattedMessage().contains("/actuator"))).isEmpty();
    }
}
