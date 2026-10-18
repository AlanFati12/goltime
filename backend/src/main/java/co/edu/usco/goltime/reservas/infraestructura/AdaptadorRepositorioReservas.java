package co.edu.usco.goltime.reservas.infraestructura;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import co.edu.usco.goltime.compartido.dominio.Pagina;
import co.edu.usco.goltime.compartido.dominio.SolicitudPagina;
import co.edu.usco.goltime.reservas.aplicacion.Relacionados;
import co.edu.usco.goltime.reservas.aplicacion.RepositorioReservas;
import co.edu.usco.goltime.reservas.dominio.EstadoReserva;
import co.edu.usco.goltime.reservas.dominio.Precio;
import co.edu.usco.goltime.reservas.dominio.Reserva;

@Repository
class AdaptadorRepositorioReservas implements RepositorioReservas {

    private static final Set<EstadoReserva> ACTIVAS = Set.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA);

    private final RepositorioReservasJpa jpa;
    private final JdbcTemplate jdbc;

    AdaptadorRepositorioReservas(RepositorioReservasJpa jpa, JdbcTemplate jdbc) {
        this.jpa = jpa;
        this.jdbc = jdbc;
    }

    @Override
    public Reserva guardar(Reserva r) {
        ReservaEntidad e;
        if (r.id() == null) {
            e = new ReservaEntidad();
            e.codigo = r.codigo();
            e.clienteId = r.clienteId();
            e.clienteMostradorId = r.clienteMostradorId();
            e.claveIdempotencia = r.claveIdempotencia();
        } else {
            e = jpa.findById(r.id()).orElseThrow();
        }
        e.canchaId = r.canchaId();
        e.inicio = r.inicio();
        e.fin = r.fin();
        e.estado = r.estado();
        e.origen = r.origen();
        e.precioBase = r.precio().base();
        e.descuento = r.precio().descuento();
        e.precioFinal = r.precio().finalAPagar();
        e.estadoPago = r.estadoPago();
        e.montoPagado = r.montoPagado();
        e.nombreEquipo = r.nombreEquipo();
        e.notas = r.notas();
        e.canceladaPor = r.canceladaPor();
        e.rolCancelador = r.rolCancelador();
        e.motivoCancelacion = r.motivoCancelacion();
        e.fechaCancelacion = r.fechaCancelacion();
        e.cancelacionTardia = r.cancelacionTardia();
        e.vecesReprogramada = r.vecesReprogramada();
        e.aprobadaPor = r.aprobadaPor();
        e.fechaAprobacion = r.fechaAprobacion();
        e.motivoRechazo = r.motivoRechazo();
        e.expiraEn = r.expiraEn();
        e.recordatorioEnviadoEn = r.recordatorioEnviadoEn();
        return aDominio(jpa.saveAndFlush(e));
    }

    @Override
    public Optional<Reserva> buscarPorId(long id) {
        return jpa.findById(id).map(AdaptadorRepositorioReservas::aDominio);
    }

    @Override
    public Optional<Reserva> buscarPorClaveIdempotencia(long clienteId, String clave) {
        return jpa.findByClienteIdAndClaveIdempotencia(clienteId, clave).map(AdaptadorRepositorioReservas::aDominio);
    }

    @Override
    public void bloquearPorClave(String clave) {
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))", rs -> { }, clave);
    }

    @Override
    public String siguienteCodigo(int anio) {
        Long numero = jdbc.queryForObject("SELECT nextval('reservas_codigo_seq')", Long.class);
        return "GT-%d-%06d".formatted(anio, numero);
    }

    @Override
    public int contarActivasFuturas(long clienteId, Instant ahora) {
        return jpa.contarFuturas(clienteId, ACTIVAS, ahora);
    }

    @Override
    public List<Long> idsFuturasActivasDeCliente(long clienteId, Instant ahora) {
        return jpa.idsFuturas(clienteId, ACTIVAS, ahora);
    }

    @Override
    public Pagina<Reserva> deCliente(long clienteId, PestanaReservas pestana, Instant ahora, SolicitudPagina p) {
        Page<ReservaEntidad> pagina = switch (pestana) {
            case PROXIMAS -> jpa.proximas(clienteId, ACTIVAS, ahora, pedir(p, Sort.by("inicio").ascending().and(Sort.by("id"))));
            case HISTORIAL -> jpa.historial(clienteId, Set.of(EstadoReserva.COMPLETADA, EstadoReserva.NO_ASISTIO),
                    EstadoReserva.CONFIRMADA, ahora, pedir(p, Sort.by("inicio").descending().and(Sort.by("id").descending())));
            case CANCELADAS -> jpa.conEstados(clienteId, Set.of(EstadoReserva.CANCELADA, EstadoReserva.RECHAZADA, EstadoReserva.EXPIRADA),
                    pedir(p, Sort.by("inicio").descending().and(Sort.by("id").descending())));
        };
        return aPagina(pagina);
    }

    @Override
    public Pagina<Reserva> deAdmin(FiltroReservasAdmin filtro, SolicitudPagina p) {
        StringBuilder desde = new StringBuilder("""
                FROM reservas r
                JOIN canchas c ON c.id = r.cancha_id
                LEFT JOIN usuarios u ON u.id = r.cliente_id
                LEFT JOIN clientes_mostrador m ON m.id = r.cliente_mostrador_id
                WHERE 1 = 1""");
        List<Object> parametros = new ArrayList<>();
        if (filtro.estado() != null) {
            desde.append(" AND r.estado = ?");
            parametros.add(filtro.estado().name());
        }
        if (filtro.sedeId() != null) {
            desde.append(" AND c.sede_id = ?");
            parametros.add(filtro.sedeId());
        }
        if (filtro.canchaId() != null) {
            desde.append(" AND r.cancha_id = ?");
            parametros.add(filtro.canchaId());
        }
        if (filtro.estadoPago() != null) {
            desde.append(" AND r.estado_pago = ?");
            parametros.add(filtro.estadoPago().name());
        }
        if (filtro.origen() != null) {
            desde.append(" AND r.origen = ?");
            parametros.add(filtro.origen().name());
        }
        if (filtro.desde() != null) {
            desde.append(" AND r.inicio >= ?");
            parametros.add(java.time.OffsetDateTime.ofInstant(filtro.desde(), java.time.ZoneOffset.UTC));
        }
        if (filtro.hasta() != null) {
            desde.append(" AND r.inicio < ?");
            parametros.add(java.time.OffsetDateTime.ofInstant(filtro.hasta(), java.time.ZoneOffset.UTC));
        }
        if (filtro.busqueda() != null && !filtro.busqueda().isBlank()) {
            desde.append(" AND (lower(r.codigo) LIKE ? OR lower(coalesce(u.nombres || ' ' || u.apellidos, m.nombre)) LIKE ? OR lower(coalesce(u.correo, '')) LIKE ?)");
            String patron = "%" + filtro.busqueda().trim().toLowerCase() + "%";
            parametros.add(patron);
            parametros.add(patron);
            parametros.add(patron);
        }
        Long total = jdbc.queryForObject("SELECT count(*) " + desde, Long.class, parametros.toArray());
        List<Object> conPagina = new ArrayList<>(parametros);
        conPagina.add(p.tamano());
        conPagina.add((long) p.pagina() * p.tamano());
        List<Long> ids = jdbc.queryForList("SELECT r.id " + desde
                + " ORDER BY (r.estado = 'PENDIENTE') DESC, CASE WHEN r.estado = 'PENDIENTE' THEN r.expira_en END ASC, r.inicio DESC, r.id DESC LIMIT ? OFFSET ?",
                Long.class, conPagina.toArray());
        Map<Long, ReservaEntidad> porId = new HashMap<>();
        jpa.findAllById(ids).forEach(e -> porId.put(e.id, e));
        List<Reserva> contenido = ids.stream().map(porId::get).map(AdaptadorRepositorioReservas::aDominio).toList();
        return new Pagina<>(contenido, p.pagina(), p.tamano(), total, (int) Math.ceil(total / (double) p.tamano()));
    }

    @Override
    public Map<Long, Relacionados> relacionados(Collection<Long> reservaIds) {
        if (reservaIds.isEmpty()) {
            return Map.of();
        }
        String marcadores = String.join(", ", reservaIds.stream().map(i -> "?").toList());
        Map<Long, Relacionados> resultado = new HashMap<>();
        jdbc.query("""
                SELECT r.id, r.cancha_id, c.nombre AS cancha, s.id AS sede_id, s.nombre AS sede, c.modalidad,
                       coalesce(u.nombres || ' ' || u.apellidos, m.nombre) AS cliente, u.correo,
                       coalesce(u.telefono, m.telefono) AS telefono
                FROM reservas r
                JOIN canchas c ON c.id = r.cancha_id
                JOIN sedes s ON s.id = c.sede_id
                LEFT JOIN usuarios u ON u.id = r.cliente_id
                LEFT JOIN clientes_mostrador m ON m.id = r.cliente_mostrador_id
                WHERE r.id IN (""" + marcadores + ")", rs -> {
                    resultado.put(rs.getLong("id"), new Relacionados(rs.getLong("cancha_id"), rs.getString("cancha"),
                            rs.getLong("sede_id"), rs.getString("sede"), rs.getString("modalidad"), rs.getString("cliente"),
                            rs.getString("correo"), rs.getString("telefono")));
                }, reservaIds.toArray());
        return resultado;
    }

    @Override
    public List<Long> idsPendientesVencidas(Instant ahora, int limite) {
        return jpa.idsPendientesVencidas(EstadoReserva.PENDIENTE, ahora, PageRequest.of(0, limite));
    }

    @Override
    public List<Long> idsConfirmadasTerminadas(Instant ahora, int limite) {
        return jpa.idsTerminadas(EstadoReserva.CONFIRMADA, ahora, PageRequest.of(0, limite));
    }

    @Override
    public List<Long> idsPorRecordar(Instant ahora, Instant hasta, int limite) {
        return jpa.idsPorRecordar(EstadoReserva.CONFIRMADA, ahora, hasta, PageRequest.of(0, limite));
    }

    private static Pageable pedir(SolicitudPagina p, Sort orden) {
        return PageRequest.of(p.pagina(), p.tamano(), orden);
    }

    private static Pagina<Reserva> aPagina(Page<ReservaEntidad> pagina) {
        return new Pagina<>(pagina.getContent().stream().map(AdaptadorRepositorioReservas::aDominio).toList(), pagina.getNumber(),
                pagina.getSize(), pagina.getTotalElements(), pagina.getTotalPages());
    }

    private static Reserva aDominio(ReservaEntidad e) {
        return Reserva.reconstruir(e.id, e.codigo, e.clienteId, e.clienteMostradorId, e.canchaId, e.inicio, e.fin, e.estado,
                e.origen, new Precio(e.precioBase, e.descuento, e.precioFinal), e.estadoPago, e.montoPagado, e.nombreEquipo,
                e.notas, e.canceladaPor, e.rolCancelador, e.motivoCancelacion, e.fechaCancelacion, e.cancelacionTardia,
                e.vecesReprogramada, e.aprobadaPor, e.fechaAprobacion, e.motivoRechazo, e.expiraEn, e.claveIdempotencia,
                e.recordatorioEnviadoEn, e.getCreadoEn());
    }
}
