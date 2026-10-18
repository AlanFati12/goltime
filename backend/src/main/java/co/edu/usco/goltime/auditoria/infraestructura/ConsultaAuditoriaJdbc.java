package co.edu.usco.goltime.auditoria.infraestructura;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import co.edu.usco.goltime.auditoria.aplicacion.ConsultaAuditoria;
import co.edu.usco.goltime.compartido.dominio.Pagina;
import co.edu.usco.goltime.compartido.dominio.Rol;
import co.edu.usco.goltime.compartido.dominio.SolicitudPagina;

@Repository
class ConsultaAuditoriaJdbc implements ConsultaAuditoria {

    private static final TypeReference<Map<String, Object>> MAPA = new TypeReference<>() { };

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapeador;

    ConsultaAuditoriaJdbc(JdbcTemplate jdbc, ObjectMapper mapeador) {
        this.jdbc = jdbc;
        this.mapeador = mapeador;
    }

    @Override
    public Pagina<RegistroAuditado> buscar(FiltroAuditoria f, SolicitudPagina p) {
        StringBuilder desde = new StringBuilder(" FROM audit_log a LEFT JOIN usuarios u ON u.id = a.actor_id WHERE 1 = 1");
        List<Object> parametros = new ArrayList<>();
        if (f.entidad() != null && !f.entidad().isBlank()) {
            desde.append(" AND a.entidad = ?");
            parametros.add(f.entidad());
        }
        if (f.entidadId() != null) {
            desde.append(" AND a.entidad_id = ?");
            parametros.add(f.entidadId());
        }
        if (f.accion() != null && !f.accion().isBlank()) {
            desde.append(" AND a.accion = ?");
            parametros.add(f.accion());
        }
        if (f.actorId() != null) {
            desde.append(" AND a.actor_id = ?");
            parametros.add(f.actorId());
        }
        if (f.desde() != null) {
            desde.append(" AND a.fecha >= ?");
            parametros.add(OffsetDateTime.ofInstant(f.desde(), ZoneOffset.UTC));
        }
        if (f.hasta() != null) {
            desde.append(" AND a.fecha < ?");
            parametros.add(OffsetDateTime.ofInstant(f.hasta(), ZoneOffset.UTC));
        }
        Long total = jdbc.queryForObject("SELECT count(*)" + desde, Long.class, parametros.toArray());
        List<Object> conPagina = new ArrayList<>(parametros);
        conPagina.add(p.tamano());
        conPagina.add((long) p.pagina() * p.tamano());
        List<RegistroAuditado> filas = jdbc.query("SELECT a.*, u.nombres || ' ' || u.apellidos AS actor_nombre" + desde
                + " ORDER BY a.fecha DESC, a.id DESC LIMIT ? OFFSET ?", (rs, i) -> new RegistroAuditado(rs.getLong("id"),
                rs.getTimestamp("fecha").toInstant(), (Long) rs.getObject("actor_id"), rs.getString("actor_nombre"),
                Rol.valueOf(rs.getString("actor_rol")), rs.getString("accion"), rs.getString("entidad"), (Long) rs.getObject("entidad_id"),
                leer(rs.getString("cambios")), rs.getString("motivo"), rs.getString("ip")), conPagina.toArray());
        return new Pagina<>(filas, p.pagina(), p.tamano(), total, (int) Math.ceil(total / (double) p.tamano()));
    }

    @Override
    public List<String> entidades() {
        return jdbc.queryForList("SELECT DISTINCT entidad FROM audit_log ORDER BY entidad", String.class);
    }

    @Override
    public List<String> acciones() {
        return jdbc.queryForList("SELECT DISTINCT accion FROM audit_log ORDER BY accion", String.class);
    }

    private Map<String, Object> leer(String json) {
        try {
            return json == null ? null : mapeador.readValue(json, MAPA);
        } catch (JsonProcessingException ex) {
            return Map.of("error", "No se pudo leer el detalle");
        }
    }
}
