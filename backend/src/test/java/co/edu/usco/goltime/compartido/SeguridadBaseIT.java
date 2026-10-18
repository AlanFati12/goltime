package co.edu.usco.goltime.compartido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.test.web.servlet.MockMvc;

import co.edu.usco.goltime.BaseIntegracion;

class SeguridadBaseIT extends BaseIntegracion {

    @Autowired
    MockMvc mvc;

    @Autowired
    JwtEncoder codificador;

    @Autowired
    JdbcTemplate jdbc;

    private String token(String rol) {
        Instant ahora = Instant.now();
        JwtClaimsSet reclamos = JwtClaimsSet.builder()
                .subject("1")
                .claim("rol", rol)
                .issuedAt(ahora)
                .expiresAt(ahora.plus(15, ChronoUnit.MINUTES))
                .build();
        return codificador.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), reclamos))
                .getTokenValue();
    }

    @Test
    void flywayAplicaLaMigracionBaseYLaExtensionDeExclusion() {
        Integer tablas = jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_name IN "
                        + "('usuarios','refresh_tokens','tokens_verificacion','configuracion','audit_log')",
                Integer.class);
        Integer extension = jdbc.queryForObject(
                "SELECT count(*) FROM pg_extension WHERE extname = 'btree_gist'", Integer.class);
        assertThat(tablas).isEqualTo(5);
        assertThat(extension).isEqualTo(1);
    }

    @Test
    void elEstadoEsPublico() throws Exception {
        mvc.perform(get("/api/v1/publico/estado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("OK"))
                .andExpect(jsonPath("$.zonaHoraria").value("America/Bogota"));
    }

    @Test
    void sinTokenSeRespondeProblemDetailConCodigo() throws Exception {
        mvc.perform(get("/api/v1/me/reservas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    void unClienteNoPuedeEntrarARutasDeAdmin() throws Exception {
        mvc.perform(get("/api/v1/admin/clientes").header("Authorization", "Bearer " + token("CLIENTE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
    }

    @Test
    void unAdminPasaLaSeguridadYRecibeUn404SiLaRutaNoExiste() throws Exception {
        mvc.perform(get("/api/v1/admin/no-existe").header("Authorization", "Bearer " + token("ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RECURSO_NO_ENCONTRADO"));
    }

    @Test
    void unTokenFirmadoConOtraClaveSeRechaza() throws Exception {
        String falso = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sIjoiQURNSU4ifQ.firma-falsa";
        mvc.perform(get("/api/v1/admin/clientes").header("Authorization", "Bearer " + falso))
                .andExpect(status().isUnauthorized());
    }
}
