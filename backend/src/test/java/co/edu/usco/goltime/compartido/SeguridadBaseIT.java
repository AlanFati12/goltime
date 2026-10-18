package co.edu.usco.goltime.compartido;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

import co.edu.usco.goltime.BaseIntegracion;

class SeguridadBaseIT extends BaseIntegracion {

    @Autowired
    JwtEncoder codificador;

    private String tokenManual(long id, String rol, Integer versionToken, Instant expira) {
        JwtClaimsSet.Builder reclamos = JwtClaimsSet.builder().subject(String.valueOf(id)).claim("rol", rol)
                .issuedAt(expira.minus(10, ChronoUnit.MINUTES)).expiresAt(expira);
        if (versionToken != null) {
            reclamos.claim("tv", versionToken);
        }
        return codificador.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), reclamos.build()))
                .getTokenValue();
    }

    @Test
    void flywayAplicaLasMigracionesYLaExtensionDeExclusion() {
        Integer tablas = jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_name IN "
                        + "('usuarios','refresh_tokens','tokens_verificacion','configuracion','audit_log')",
                Integer.class);
        Integer extension = jdbc.queryForObject("SELECT count(*) FROM pg_extension WHERE extname = 'btree_gist'", Integer.class);
        Integer recordar = jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.columns WHERE table_name='refresh_tokens' AND column_name='recordar'",
                Integer.class);
        assertThat(tablas).isEqualTo(5);
        assertThat(extension).isEqualTo(1);
        assertThat(recordar).isEqualTo(1);
    }

    @Test
    void elEstadoEsPublico() throws Exception {
        MvcResult r = consultar("/api/v1/publico/estado", null);
        esperarEstado(r, 200);
        assertThat((String) JsonPath.read(r.getResponse().getContentAsString(), "$.zonaHoraria")).isEqualTo("America/Bogota");
    }

    @Test
    void sinTokenSeRespondeProblemDetailConCodigo() throws Exception {
        MvcResult r = consultar("/api/v1/me", null);
        esperarEstado(r, 401);
        assertThat((String) JsonPath.read(r.getResponse().getContentAsString(), "$.codigo")).isEqualTo("NO_AUTENTICADO");
    }

    @Test
    void unClienteNoPuedeEntrarARutasDeAdmin() throws Exception {
        Sesion cliente = nuevoClienteConSesion();
        for (String ruta : new String[] {"/api/v1/admin/clientes", "/api/v1/admin/administradores", "/api/v1/admin/no-existe"}) {
            MvcResult r = consultar(ruta, cliente.token());
            esperarEstado(r, 403);
            assertThat((String) JsonPath.read(r.getResponse().getContentAsString(), "$.codigo")).isEqualTo("ACCESO_DENEGADO");
        }
    }

    @Test
    void unAdminPasaLaSeguridadYRecibeUn404SiLaRutaNoExiste() throws Exception {
        MvcResult r = consultar("/api/v1/admin/no-existe", ingresarAdmin().token());
        esperarEstado(r, 404);
        assertThat((String) JsonPath.read(r.getResponse().getContentAsString(), "$.codigo")).isEqualTo("RECURSO_NO_ENCONTRADO");
    }

    @Test
    void rechazaTokensFalsificadosExpiradosOSinVersionVigente() throws Exception {
        Sesion cliente = nuevoClienteConSesion();
        Instant futuro = Instant.now().plus(5, ChronoUnit.MINUTES);

        String falso = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sIjoiQURNSU4ifQ.firma-falsa";
        esperarEstado(consultar("/api/v1/me", falso), 401);
        esperarEstado(consultar("/api/v1/me", tokenManual(cliente.id(), "CLIENTE", 0, Instant.now().minus(5, ChronoUnit.MINUTES))), 401);
        esperarEstado(consultar("/api/v1/me", tokenManual(cliente.id(), "CLIENTE", null, futuro)), 401);
        esperarEstado(consultar("/api/v1/me", tokenManual(cliente.id(), "CLIENTE", 99, futuro)), 401);
        esperarEstado(consultar("/api/v1/me", tokenManual(987654321L, "CLIENTE", 0, futuro)), 401);
        esperarEstado(consultar("/api/v1/me", tokenManual(cliente.id(), "CLIENTE", 0, futuro)), 200);
    }
}
