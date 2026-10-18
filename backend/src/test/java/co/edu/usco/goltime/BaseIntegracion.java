package co.edu.usco.goltime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;

/**
 * Base de las pruebas de integración: Spring completo contra un PostgreSQL real (Testcontainers).
 * El contenedor es único para toda la ejecución, así se reutiliza el contexto entre clases.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegracion {

    public static final String CLAVE_VALIDA = "Clave1234";
    public static final String ADMIN_CORREO = "admin-pruebas@goltime.test";
    public static final String ADMIN_CLAVE = "ClaveAdmin2026";
    public static final String COOKIE = "goltime_refresco";

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    @Autowired
    protected JdbcTemplate jdbc;

    /** Sesión de prueba: access token, valor de la cookie de refresh e id del usuario. */
    public record Sesion(String token, String refresco, long id) {
    }

    protected static String correoUnico() {
        return "cliente-" + UUID.randomUUID().toString().substring(0, 8) + "@prueba.test";
    }

    protected Map<String, Object> datosRegistro(String correo, String clave) {
        return Map.ofEntries(
                Map.entry("nombres", "Ana"), Map.entry("apellidos", "Pérez"), Map.entry("correo", correo),
                Map.entry("telefono", "3001234567"), Map.entry("clave", clave), Map.entry("confirmacionClave", clave),
                Map.entry("aceptaTerminos", true));
    }

    protected MvcResult enviar(String url, Object cuerpo, Cookie... cookies) throws Exception {
        MockHttpServletRequestBuilder peticion = post(url).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(cuerpo));
        if (cookies.length > 0) {
            peticion.cookie(cookies);
        }
        return mvc.perform(peticion).andReturn();
    }

    protected MvcResult consultar(String url, String token) throws Exception {
        MockHttpServletRequestBuilder peticion = get(url);
        if (token != null) {
            peticion.header("Authorization", "Bearer " + token);
        }
        return mvc.perform(peticion).andReturn();
    }

    protected MvcResult enviarConToken(String url, Object cuerpo, String token) throws Exception {
        return mvc.perform(post(url).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(cuerpo))).andReturn();
    }

    protected void registrar(String correo, String clave) throws Exception {
        MvcResult r = enviar("/api/v1/autenticacion/registrar", datosRegistro(correo, clave));
        org.assertj.core.api.Assertions.assertThat(r.getResponse().getStatus()).isEqualTo(201);
    }

    protected Sesion ingresar(String correo, String clave, boolean recordar) throws Exception {
        MvcResult r = enviar("/api/v1/autenticacion/ingresar",
                Map.of("correo", correo, "clave", clave, "recordarme", recordar));
        org.assertj.core.api.Assertions.assertThat(r.getResponse().getStatus()).isEqualTo(200);
        return sesionDe(r);
    }

    protected Sesion ingresarAdmin() throws Exception {
        return ingresar(ADMIN_CORREO, ADMIN_CLAVE, false);
    }

    /** Registra un cliente nuevo y devuelve su sesión. */
    protected Sesion nuevoClienteConSesion() throws Exception {
        String correo = correoUnico();
        registrar(correo, CLAVE_VALIDA);
        return ingresar(correo, CLAVE_VALIDA, false);
    }

    protected Sesion sesionDe(MvcResult r) throws Exception {
        String cuerpo = r.getResponse().getContentAsString();
        return new Sesion(JsonPath.read(cuerpo, "$.tokenAcceso"), valorCookie(r),
                ((Number) JsonPath.read(cuerpo, "$.usuario.id")).longValue());
    }

    /** Valor de la cookie de refresh enviada en Set-Cookie (null si no hay). */
    protected static String valorCookie(MvcResult r) {
        for (String cabecera : r.getResponse().getHeaders("Set-Cookie")) {
            Matcher m = Pattern.compile(COOKIE + "=([^;]*)").matcher(cabecera);
            if (m.find()) {
                return m.group(1);
            }
        }
        return null;
    }

    protected static Cookie cookie(String valor) {
        return new Cookie(COOKIE, valor);
    }

    protected void esperarEstado(MvcResult r, int estado) {
        org.assertj.core.api.Assertions.assertThat(r.getResponse().getStatus()).isEqualTo(estado);
    }

}
