package co.edu.usco.goltime.compartido.infraestructura.persistencia;

import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import co.edu.usco.goltime.compartido.aplicacion.LectorConfiguracion;

@Component
class LectorConfiguracionJdbc implements LectorConfiguracion {

    private final JdbcTemplate jdbc;

    LectorConfiguracionJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<String> texto(String clave) {
        return jdbc.query("SELECT valor FROM configuracion WHERE clave = ?", (rs, i) -> rs.getString(1), clave)
                .stream().findFirst();
    }
}
