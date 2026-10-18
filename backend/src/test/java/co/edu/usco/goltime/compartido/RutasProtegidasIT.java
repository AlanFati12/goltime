package co.edu.usco.goltime.compartido;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import co.edu.usco.goltime.BaseIntegracion;

/**
 * Recorre TODAS las rutas de la API y comprueba su protección, así una ruta nueva sin proteger rompe la prueba:
 * las de administración responden 401 sin sesión y 403 a un cliente; las de {@code /me} y las de los demás recursos propios
 * exigen sesión; y lo público es solo lectura.
 */
class RutasProtegidasIT extends BaseIntegracion {

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping mapeo;

    private record Ruta(String metodo, String patron) {
        String url() {
            return patron.replaceAll("\\{[^}]+}", "1");
        }
    }

    private List<Ruta> rutas() {
        List<Ruta> rutas = new ArrayList<>();
        for (RequestMappingInfo info : mapeo.getHandlerMethods().keySet()) {
            Set<RequestMethod> metodos = info.getMethodsCondition().getMethods();
            if (info.getPathPatternsCondition() == null) {
                continue;
            }
            for (var patron : info.getPathPatternsCondition().getPatternValues()) {
                if (!patron.startsWith("/api/v1/")) {
                    continue;
                }
                if (metodos.isEmpty()) {
                    rutas.add(new Ruta("GET", patron));
                }
                metodos.forEach(m -> rutas.add(new Ruta(m.name(), patron)));
            }
        }
        return rutas;
    }

    private MvcResult llamar(Ruta ruta, String token) throws Exception {
        var peticion = MockMvcRequestBuilders.request(HttpMethod.valueOf(ruta.metodo()), ruta.url()).contentType("application/json").content("{}");
        if (token != null) {
            peticion.header("Authorization", "Bearer " + token);
        }
        return mvc.perform(peticion).andReturn();
    }

    @Test
    void todaRutaDeAdministracionExigeSerAdministrador() throws Exception {
        String cliente = nuevoClienteConSesion().token();
        List<String> fallos = new ArrayList<>();
        int revisadas = 0;
        for (Ruta r : rutas()) {
            if (!r.patron().startsWith("/api/v1/admin/")) {
                continue;
            }
            revisadas++;
            int sinSesion = llamar(r, null).getResponse().getStatus();
            int comoCliente = llamar(r, cliente).getResponse().getStatus();
            if (sinSesion != 401 || comoCliente != 403) {
                fallos.add(r.metodo() + " " + r.patron() + " -> sin sesión " + sinSesion + ", cliente " + comoCliente);
            }
        }
        assertThat(revisadas).as("rutas de administración revisadas").isGreaterThan(100);
        assertThat(fallos).isEmpty();
    }

    @Test
    void lasRutasDelClienteExigenSesion() throws Exception {
        List<String> fallos = new ArrayList<>();
        int revisadas = 0;
        for (Ruta r : rutas()) {
            boolean publica = r.patron().startsWith("/api/v1/publico/") || r.patron().startsWith("/api/v1/autenticacion/");
            if (publica || r.patron().startsWith("/api/v1/admin/")) {
                continue;
            }
            revisadas++;
            int estado = llamar(r, null).getResponse().getStatus();
            if (estado != 401) {
                fallos.add(r.metodo() + " " + r.patron() + " -> " + estado);
            }
        }
        assertThat(revisadas).as("rutas de cliente revisadas").isGreaterThan(15);
        assertThat(fallos).isEmpty();
    }

    @Test
    void loPublicoEsSoloLectura() {
        Set<String> escrituras = new TreeSet<>();
        rutas().stream().filter(r -> r.patron().startsWith("/api/v1/publico/") && !r.metodo().equals("GET")).forEach(r -> escrituras.add(r.metodo() + " " + r.patron()));
        assertThat(escrituras).isEmpty();
    }
}
