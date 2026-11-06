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

    public static final String ADMIN_CORREO = "admin-pruebas@goltime.test";
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

    /** Credencial de prueba que acepta el verificador de Google del perfil `test` (ver VerificadorGooglePrueba). */
    protected static String credencialGoogle(String correo, String nombres, String apellidos) {
        return "prueba|" + correo + "|" + nombres + "|" + apellidos;
    }

    /** Ingreso con Google (de mentira): crea la cuenta si es la primera vez. Devuelve la respuesta completa. */
    protected MvcResult ingresoGoogle(String credencial, boolean recordar) throws Exception {
        return enviar("/api/v1/autenticacion/google", Map.of("credencial", credencial, "recordarme", recordar));
    }

    /**
     * Entra con Google con ese correo (la cuenta se crea la primera vez como cliente «Ana Pérez»). Si la cuenta aún no
     * tiene WhatsApp se le pone uno de prueba, como tenían todos los clientes cuando se registraban con formulario.
     */
    protected Sesion entrar(String correo, boolean recordar) throws Exception {
        MvcResult r = ingresoGoogle(credencialGoogle(correo, "Ana", "Pérez"), recordar);
        org.assertj.core.api.Assertions.assertThat(r.getResponse().getStatus()).isEqualTo(200);
        Sesion sesion = sesionDe(r);
        String cuerpo = r.getResponse().getContentAsString();
        Map<String, Object> usuario = JsonPath.read(cuerpo, "$.usuario");
        // la API omite los campos nulos: sin clave «telefono» es que la cuenta no tiene WhatsApp
        if ("CLIENTE".equals(usuario.get("rol")) && usuario.get("telefono") == null) {
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/me")
                    .header("Authorization", "Bearer " + sesion.token()).contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(Map.of("nombres", "Ana", "apellidos", "Pérez", "telefono", "3001234567"))))
                    .andReturn();
        }
        return sesion;
    }

    /** Entra con Google con ese correo y deja ese WhatsApp en su perfil (para las pruebas del bot). */
    protected Sesion entrarConWhatsapp(String correo, String telefono) throws Exception {
        Sesion sesion = entrar(correo, false);
        MvcResult r = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/me")
                .header("Authorization", "Bearer " + sesion.token()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("nombres", "Ana", "apellidos", "Pérez", "telefono", telefono)))).andReturn();
        org.assertj.core.api.Assertions.assertThat(r.getResponse().getStatus()).isEqualTo(200);
        return sesion;
    }

    /** El administrador inicial (lo autoriza AdministradorInicial desde el correo del perfil `test`). */
    protected Sesion ingresarAdmin() throws Exception {
        return entrar(ADMIN_CORREO, false);
    }

    /** Un cliente nuevo con su sesión. */
    protected Sesion nuevoClienteConSesion() throws Exception {
        return entrar(correoUnico(), false);
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
