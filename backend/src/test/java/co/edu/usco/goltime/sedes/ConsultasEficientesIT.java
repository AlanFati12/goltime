package co.edu.usco.goltime.sedes;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

import co.edu.usco.goltime.BaseIntegracion;
import co.edu.usco.goltime.compartido.ContadorSentencias;

/**
 * Vigila las consultas N+1 en los listados de sedes y canchas: las sentencias SQL de una petición no deben crecer
 * con la cantidad de sedes ni de canchas.
 */
@Import(ContadorSentencias.class)
class ConsultasEficientesIT extends BaseIntegracion {

    private String admin;

    @BeforeEach
    void iniciarSesionAdmin() throws Exception {
        admin = ingresarAdmin().token();
    }

    private long crearSedeConCanchas(int canchas) throws Exception {
        Map<String, Object> sede = new HashMap<>();
        sede.put("nombre", "Sede " + UUID.randomUUID().toString().substring(0, 8));
        sede.put("direccion", "Calle 10 # 5-20");
        sede.put("ciudad", "Neiva");
        sede.put("descripcion", "Sede de prueba");
        sede.put("servicios", List.of("Parqueadero"));
        MvcResult r = enviarConToken("/api/v1/admin/sedes", sede, admin);
        esperarEstado(r, 201);
        long sedeId = ((Number) JsonPath.read(r.getResponse().getContentAsString(), "$.id")).longValue();
        for (int i = 0; i < canchas; i++) {
            Map<String, Object> cancha = new HashMap<>();
            cancha.put("nombre", "Cancha " + i + " " + UUID.randomUUID().toString().substring(0, 6));
            cancha.put("jugadoresPorEquipo", 5);
            cancha.put("precioBaseHora", 80000);
            esperarEstado(enviarConToken("/api/v1/admin/sedes/" + sedeId + "/canchas", cancha, admin), 201);
        }
        return sedeId;
    }

    private long sentencias(String url) throws Exception {
        consultar(url, admin); // calienta cachés y pools para medir solo lo que cuesta la petición
        long antes = ContadorSentencias.total();
        esperarEstado(consultar(url, admin), 200);
        return ContadorSentencias.total() - antes;
    }

    @Test
    void los_listados_no_crecen_en_sentencias_con_la_cantidad_de_sedes_y_canchas() throws Exception {
        String[] urls = { "/api/v1/publico/sedes", "/api/v1/admin/sedes", "/api/v1/publico/canchas", "/api/v1/admin/canchas" };
        crearSedeConCanchas(1);
        Map<String, Long> pocas = new HashMap<>();
        for (String url : urls) {
            pocas.put(url, sentencias(url));
        }

        for (int i = 0; i < 5; i++) {
            crearSedeConCanchas(3);
        }
        for (String url : urls) {
            // Con N+1 las 5 sedes y 15 canchas nuevas sumarían decenas de sentencias; por lotes, a lo sumo un par.
            assertThat(sentencias(url)).as("sentencias de %s con muchas sedes y canchas", url).isLessThanOrEqualTo(pocas.get(url) + 3);
        }
    }
}
