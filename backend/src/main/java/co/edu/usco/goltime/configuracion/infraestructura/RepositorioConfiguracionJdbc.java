package co.edu.usco.goltime.configuracion.infraestructura;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import co.edu.usco.goltime.configuracion.aplicacion.ServicioConfiguracion.Parametro;
import co.edu.usco.goltime.configuracion.aplicacion.ServicioConfiguracion.Repositorio;
import co.edu.usco.goltime.configuracion.dominio.ReglaConfiguracion;

@Repository
class RepositorioConfiguracionJdbc implements Repositorio {

    private final JdbcTemplate jdbc;

    RepositorioConfiguracionJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Parametro> listar() {
        return jdbc.query("SELECT * FROM configuracion ORDER BY id", (rs, i) -> parametro(rs));
    }

    @Override
    public Optional<Parametro> buscar(String clave) {
        return jdbc.query("SELECT * FROM configuracion WHERE clave = ?", (rs, i) -> parametro(rs), clave).stream().findFirst();
    }

    @Override
    public void actualizar(String clave, String valor, Long actorId) {
        jdbc.update("UPDATE configuracion SET valor = ?, actualizado_en = now(), actualizado_por = ? WHERE clave = ?", valor, actorId, clave);
    }

    private static Parametro parametro(ResultSet rs) throws SQLException {
        String clave = rs.getString("clave");
        var regla = ReglaConfiguracion.de(clave);
        boolean numerico = regla.map(r -> r.tipo() == ReglaConfiguracion.Tipo.ENTERO).orElse(false);
        return new Parametro(clave, rs.getString("valor"), rs.getString("tipo"), rs.getString("descripcion"), regla.isPresent(), numerico ? regla.get().minimo() : null,
                numerico ? regla.get().maximo() : null, rs.getObject("actualizado_en", OffsetDateTime.class).toInstant());
    }
}
