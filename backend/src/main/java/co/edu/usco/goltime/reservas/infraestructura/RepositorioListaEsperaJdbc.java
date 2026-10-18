package co.edu.usco.goltime.reservas.infraestructura;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import co.edu.usco.goltime.reservas.aplicacion.RepositorioListaEspera;

@Repository
class RepositorioListaEsperaJdbc implements RepositorioListaEspera {

    private static final String SELECT = """
            SELECT l.*, c.nombre AS cancha, s.nombre AS sede FROM lista_espera l
            JOIN canchas c ON c.id = l.cancha_id JOIN sedes s ON s.id = c.sede_id
            """;

    private final JdbcTemplate jdbc;

    RepositorioListaEsperaJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static OffsetDateTime utc(Instant i) {
        return OffsetDateTime.ofInstant(i, ZoneOffset.UTC);
    }

    @Override
    public Espera crear(long clienteId, long canchaId, Instant inicio, Instant fin) {
        Long id = jdbc.queryForObject("INSERT INTO lista_espera (cliente_id, cancha_id, inicio, fin) VALUES (?, ?, ?, ?) RETURNING id", Long.class, clienteId, canchaId,
                utc(inicio), utc(fin));
        return buscar(id).orElseThrow();
    }

    @Override
    public Optional<Espera> buscar(long id) {
        return jdbc.query(SELECT + " WHERE l.id = ?", (rs, i) -> espera(rs), id).stream().findFirst();
    }

    @Override
    public List<Espera> activasDeCliente(long clienteId, Instant ahora) {
        return jdbc.query(SELECT + " WHERE l.cliente_id = ? AND l.estado IN ('ESPERANDO', 'NOTIFICADA') AND l.inicio > ? ORDER BY l.inicio", (rs, i) -> espera(rs), clienteId,
                utc(ahora));
    }

    @Override
    public int contarActivas(long clienteId, Instant ahora) {
        Integer n = jdbc.queryForObject("SELECT count(*) FROM lista_espera WHERE cliente_id = ? AND estado IN ('ESPERANDO', 'NOTIFICADA') AND inicio > ?", Integer.class,
                clienteId, utc(ahora));
        return n == null ? 0 : n;
    }

    @Override
    public boolean existeActiva(long clienteId, long canchaId, Instant inicio, Instant fin) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM lista_espera WHERE cliente_id = ? AND cancha_id = ? AND inicio = ? AND fin = ? AND estado IN ('ESPERANDO', 'NOTIFICADA'))""",
                Boolean.class, clienteId, canchaId, utc(inicio), utc(fin)));
    }

    @Override
    public List<Espera> esperandoEn(Collection<Long> canchaIds, Instant inicio, Instant fin, Instant ahora) {
        if (canchaIds.isEmpty()) {
            return List.of();
        }
        String marcadores = canchaIds.stream().map(i -> "?").collect(Collectors.joining(", "));
        List<Object> args = new java.util.ArrayList<>(canchaIds);
        args.add(utc(fin));
        args.add(utc(inicio));
        args.add(utc(ahora));
        return jdbc.query(SELECT + " WHERE l.estado = 'ESPERANDO' AND l.cancha_id IN (" + marcadores + ") AND l.inicio < ? AND l.fin > ? AND l.inicio > ? ORDER BY l.creado_en, l.id",
                (rs, i) -> espera(rs), args.toArray());
    }

    @Override
    public void cambiarEstado(long id, Estado estado, Instant notificadaEn) {
        jdbc.update("UPDATE lista_espera SET estado = ?, notificada_en = coalesce(?, notificada_en) WHERE id = ?", estado.name(), notificadaEn == null ? null : utc(notificadaEn), id);
    }

    @Override
    public int cumplir(long clienteId, Collection<Long> canchaIds, Instant inicio, Instant fin) {
        if (canchaIds.isEmpty()) {
            return 0;
        }
        String marcadores = canchaIds.stream().map(i -> "?").collect(Collectors.joining(", "));
        List<Object> args = new java.util.ArrayList<>();
        args.add(clienteId);
        args.addAll(canchaIds);
        args.add(utc(fin));
        args.add(utc(inicio));
        return jdbc.update("UPDATE lista_espera SET estado = 'CUMPLIDA' WHERE cliente_id = ? AND estado IN ('ESPERANDO', 'NOTIFICADA') AND cancha_id IN (" + marcadores
                + ") AND inicio < ? AND fin > ?", args.toArray());
    }

    private static Espera espera(ResultSet rs) throws SQLException {
        return new Espera(rs.getLong("id"), rs.getLong("cliente_id"), rs.getLong("cancha_id"), rs.getString("cancha"), rs.getString("sede"),
                rs.getObject("inicio", OffsetDateTime.class).toInstant(), rs.getObject("fin", OffsetDateTime.class).toInstant(), Estado.valueOf(rs.getString("estado")),
                rs.getObject("creado_en", OffsetDateTime.class).toInstant());
    }
}
