package co.edu.usco.goltime.reservas.infraestructura;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import co.edu.usco.goltime.reservas.aplicacion.RepositorioMostrador;

@Repository
class RepositorioMostradorJdbc implements RepositorioMostrador {

    private final JdbcTemplate jdbc;

    RepositorioMostradorJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public long buscarOCrear(String nombre, String telefono) {
        List<Long> existentes = jdbc.queryForList("SELECT id FROM clientes_mostrador WHERE telefono = ? AND whatsapp_lid IS NULL ORDER BY id LIMIT 1",
                Long.class, telefono);
        if (!existentes.isEmpty()) {
            return existentes.getFirst();
        }
        return jdbc.queryForObject("INSERT INTO clientes_mostrador (nombre, telefono) VALUES (?, ?) RETURNING id", Long.class, nombre, telefono);
    }

    @Override
    public Optional<ClienteMostrador> buscarPorTelefono(String telefono) {
        return jdbc.query("SELECT id, nombre, telefono FROM clientes_mostrador WHERE telefono = ? AND whatsapp_lid IS NULL ORDER BY id LIMIT 1",
                (rs, i) -> new ClienteMostrador(rs.getLong("id"), rs.getString("nombre"), rs.getString("telefono")), telefono).stream().findFirst();
    }

    @Override
    public Optional<ClienteMostrador> buscarPorLid(String whatsappLid) {
        return jdbc.query("SELECT id, nombre, telefono FROM clientes_mostrador WHERE whatsapp_lid = ?",
                (rs, i) -> new ClienteMostrador(rs.getLong("id"), rs.getString("nombre"), rs.getString("telefono")), whatsappLid).stream().findFirst();
    }

    @Override
    public long crearConLid(String nombre, String telefono, String whatsappLid) {
        return jdbc.queryForObject("INSERT INTO clientes_mostrador (nombre, telefono, whatsapp_lid) VALUES (?, ?, ?) RETURNING id", Long.class,
                nombre, telefono, whatsappLid);
    }

    @Override
    public Optional<String> lidDe(long id) {
        return jdbc.queryForList("SELECT whatsapp_lid FROM clientes_mostrador WHERE id = ? AND whatsapp_lid IS NOT NULL", String.class, id)
                .stream().findFirst();
    }

    @Override
    public List<ClienteMostrador> buscar(String texto, int limite) {
        String patron = "%" + (texto == null ? "" : texto.trim().toLowerCase()) + "%";
        return jdbc.query("SELECT id, nombre, telefono FROM clientes_mostrador WHERE lower(nombre) LIKE ? OR coalesce(telefono, '') LIKE ? ORDER BY nombre LIMIT ?",
                (rs, i) -> new ClienteMostrador(rs.getLong("id"), rs.getString("nombre"), rs.getString("telefono")), patron, patron, limite);
    }
}
