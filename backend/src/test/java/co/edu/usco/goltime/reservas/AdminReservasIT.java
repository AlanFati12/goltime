package co.edu.usco.goltime.reservas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

class AdminReservasIT extends BaseReservasIT {

    private String admin;
    private Long adminId;
    private long sedeId;
    private long canchaId;
    private Sesion cliente;
    private LocalDate fecha;

    @BeforeEach
    void preparar() throws Exception {
        Sesion s = ingresarAdmin();
        admin = s.token();
        adminId = s.id();
        sedeId = crearSede(admin);
        canchaId = crearCancha(admin, sedeId, "Cancha A");
        cliente = nuevoClienteConSesion();
        fecha = diaHabil(5);
    }

    // ----- Ayudantes -----

    private Map<String, Object> cuerpoAdmin(long cancha, Instant inicio, Instant fin, Long clienteId, String nombre, String telefono) {
        Map<String, Object> m = new HashMap<>();
        m.put("canchaId", cancha);
        m.put("inicio", inicio.toString());
        m.put("fin", fin.toString());
        if (clienteId != null) {
            m.put("clienteId", clienteId);
        }
        if (nombre != null) {
            m.put("mostrador", Map.of("nombre", nombre, "telefono", telefono));
        }
        m.put("nombreEquipo", "Los Veteranos");
        return m;
    }

    private MvcResult crearAdmin(long cancha, LocalDate f, String desde, String hasta, Long clienteId, String nombre, String telefono) throws Exception {
        return postear("/api/v1/admin/reservas", admin, cuerpoAdmin(cancha, a(f, desde), a(f, hasta), clienteId, nombre, telefono));
    }

    private long crearParaCliente(LocalDate f, String desde, String hasta) throws Exception {
        MvcResult r = crearAdmin(canchaId, f, desde, hasta, cliente.id(), null, null);
        esperarEstado(r, 201);
        return id(r);
    }

    private void mover(long reservaId, Instant inicio, Instant fin) {
        OffsetDateTime i = OffsetDateTime.ofInstant(inicio, ZoneOffset.UTC);
        OffsetDateTime f = OffsetDateTime.ofInstant(fin, ZoneOffset.UTC);
        jdbc.update("UPDATE reservas SET inicio = ?, fin = ? WHERE id = ?", i, f, reservaId);
        jdbc.update("UPDATE ocupaciones_cancha SET rango = tstzrange(?, ?, '[)') WHERE reserva_id = ?", i, f, reservaId);
    }

    private MvcResult pagar(long reservaId, int monto, String metodo, String referencia) throws Exception {
        Map<String, Object> m = new HashMap<>(Map.of("monto", monto, "metodo", metodo));
        if (referencia != null) {
            m.put("referencia", referencia);
        }
        return postear("/api/v1/admin/reservas/" + reservaId + "/pagos", admin, m);
    }

    private int auditorias(String accion, long entidadId) {
        return jdbc.queryForObject("SELECT count(*) FROM audit_log WHERE accion = ? AND entidad_id = ?", Integer.class, accion, entidadId);
    }

    // ===================== Crear a nombre de un cliente =====================

    @Test
    void elAdminCreaUnaReservaConfirmadaParaUnClienteRegistradoYSeLeAvisa() throws Exception {
        MvcResult r = crearAdmin(canchaId, fecha, "19:00", "20:00", cliente.id(), null, null);

        esperarEstado(r, 201);
        long reservaId = id(r);
        assertThat((String) leer(r, "$.estado")).isEqualTo("CONFIRMADA");
        assertThat((String) leer(r, "$.origen")).isEqualTo("ADMIN");
        assertThat((String) leer(r, "$.cliente.nombre")).isEqualTo("Ana Pérez");
        assertThat((Integer) leer(r, "$.precioFinal")).isEqualTo(80_000);
        assertThat(jdbc.queryForObject("SELECT aprobada_por FROM reservas WHERE id = ?", Long.class, reservaId)).isEqualTo(adminId);
        assertThat(ocupaciones(reservaId)).isEqualTo(1);
        assertThat(acciones(reservaId)).containsExactly("CREADA");
        assertThat(auditorias("CREAR_RESERVA_ADMIN", reservaId)).isEqualTo(1);
        // el cliente la ve entre sus próximas reservas y recibe el aviso
        assertThat((List<?>) leer(obtener("/api/v1/me/reservas", cliente.token()), "$.contenido")).hasSize(1);
        assertThat(notificaciones(cliente.id(), "RESERVA_CREADA_ADMIN")).isEqualTo(1);
        // y no quedó disponible para otros
        esperarEstado(solicitar(nuevoClienteConSesion().token(), canchaId, fecha, "19:00", "20:00"), 409);
    }

    @Test
    void elAdminCreaReservasParaClientesDeMostradorYReutilizaAlMismoPorTelefono() throws Exception {
        MvcResult primera = crearAdmin(canchaId, fecha, "19:00", "20:00", null, "Don Pedro", "3201112233");
        MvcResult segunda = crearAdmin(canchaId, fecha, "20:00", "21:00", null, "Pedro Gómez", "3201112233");

        esperarEstado(primera, 201);
        esperarEstado(segunda, 201);
        assertThat((String) leer(primera, "$.cliente.nombre")).isEqualTo("Don Pedro");
        assertThat((String) leer(primera, "$.cliente.telefono")).isEqualTo("+573201112233");
        assertThat(jdbc.queryForObject("SELECT cliente_id IS NULL FROM reservas WHERE id = ?", Boolean.class, id(primera))).isTrue();
        Integer mostradores = jdbc.queryForObject("SELECT count(*) FROM clientes_mostrador WHERE telefono = '+573201112233'", Integer.class);
        assertThat(mostradores).as("mismo teléfono = mismo cliente").isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(DISTINCT cliente_mostrador_id) FROM reservas WHERE id IN (?, ?)", Integer.class, id(primera), id(segunda))).isEqualTo(1);

        MvcResult busqueda = obtener("/api/v1/admin/clientes-mostrador?busqueda=pedro", admin);
        assertThat((List<?>) leer(busqueda, "$")).hasSize(1);
    }

    @Test
    void exigeUnClienteRegistradoOUnoDeMostradorPeroNoAmbosNiNinguno() throws Exception {
        esperarEstado(crearAdmin(canchaId, fecha, "19:00", "20:00", null, null, null), 400);
        esperarEstado(crearAdmin(canchaId, fecha, "19:00", "20:00", cliente.id(), "Don Pedro", "3201112233"), 400);
        MvcResult sinTelefono = postear("/api/v1/admin/reservas", admin, cuerpoAdminSinTelefono());
        esperarEstado(sinTelefono, 400);
        esperarEstado(crearAdmin(canchaId, fecha, "19:00", "20:00", 987654321L, null, null), 404);
        // ninguna dejó rastro
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reservas WHERE cancha_id = ?", Integer.class, canchaId)).isZero();
    }

    private Map<String, Object> cuerpoAdminSinTelefono() {
        Map<String, Object> m = cuerpoAdmin(canchaId, a(fecha, "19:00"), a(fecha, "20:00"), null, null, null);
        m.put("mostrador", Map.of("nombre", "Don Pedro", "telefono", " "));
        return m;
    }

    @Test
    void noSeCreaParaUnClienteDesactivado() throws Exception {
        Sesion otro = nuevoClienteConSesion();
        esperarEstado(postear("/api/v1/admin/clientes/" + otro.id() + "/desactivar", admin, Map.of("motivo", "Conducta indebida")), 200);
        MvcResult r = crearAdmin(canchaId, fecha, "19:00", "20:00", otro.id(), null, null);
        esperarEstado(r, 403);
        assertThat((String) leer(r, "$.codigo")).isEqualTo("USUARIO_DESACTIVADO");
    }

    @Test
    void elAdminSeSaltaLaAntelacionMinimaYElLimiteDeReservasPeroNoElHorarioNiElSolapamiento() throws Exception {
        // sede abierta 24 h para poder reservar dentro de las próximas horas sin depender de la hora del día
        List<Map<String, Object>> dias = new ArrayList<>();
        for (int d = 1; d <= 7; d++) {
            dias.add(Map.of("diaSemana", d, "cerrado", false, "apertura", "00:00", "cierre", "24:00"));
        }
        esperarEstado(mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/admin/sedes/" + sedeId + "/horario")
                .header("Authorization", "Bearer " + admin).contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("dias", dias)))).andReturn(), 200);

        // próxima hora en punto, a unos 30-90 min: dentro de la antelación mínima de 3 h de los clientes
        Instant inicio = Instant.now().plus(Duration.ofMinutes(30)).truncatedTo(ChronoUnit.HOURS).plus(Duration.ofHours(1));
        Instant fin = inicio.plus(Duration.ofHours(1));
        MvcResult cercana = postear("/api/v1/reservas", cliente.token(), cuerpoReserva(canchaId, inicio, fin));
        esperarEstado(cercana, 422);
        assertThat((String) leer(cercana, "$.codigo")).isEqualTo("FUERA_DE_VENTANA");
        esperarEstado(postear("/api/v1/admin/reservas", admin, cuerpoAdmin(canchaId, inicio, fin, cliente.id(), null, null)), 201);

        // más reservas activas que el máximo de un cliente (5)
        for (int i = 1; i <= 6; i++) {
            esperarEstado(crearAdmin(canchaId, enDias(i + 1), "19:00", "20:00", cliente.id(), null, null), 201);
        }
        // lo que sí se respeta: el solapamiento y el horario de la cancha
        MvcResult choque = crearAdmin(canchaId, enDias(2), "19:00", "20:00", cliente.id(), null, null);
        esperarEstado(choque, 409);
        assertThat((String) leer(choque, "$.codigo")).isEqualTo("RESERVA_SOLAPADA");
        esperarEstado(postear("/api/v1/admin/reservas", admin, cuerpoAdmin(canchaId, a(enDias(3), "19:30"), a(enDias(3), "20:30"), cliente.id(), null, null)), 400);
    }

    @Test
    void soloElAdminPuedeCrearReservasAOtrosClientes() throws Exception {
        MvcResult r = postear("/api/v1/admin/reservas", cliente.token(), cuerpoAdmin(canchaId, a(fecha, "19:00"), a(fecha, "20:00"), cliente.id(), null, null));
        esperarEstado(r, 403);
        esperarEstado(postear("/api/v1/admin/reservas", null, cuerpoAdmin(canchaId, a(fecha, "19:00"), a(fecha, "20:00"), cliente.id(), null, null)), 401);
    }

    // ===================== Reprogramar y estado =====================

    @Test
    void elAdminReprogramaConMotivoSinLosLimitesDelClienteYSeNotificaAlCliente() throws Exception {
        long reservaId = crearParaCliente(fecha, "19:00", "20:00");
        String base = "/api/v1/admin/reservas/" + reservaId;
        Map<String, Object> sinMotivo = Map.of("inicio", a(fecha, "20:00").toString(), "fin", a(fecha, "21:00").toString(), "motivo", " ");
        esperarEstado(parchear(base + "/reprogramar", admin, sinMotivo), 400);

        // más veces que el máximo del cliente (2), y con la reserva ya a punto de empezar
        String[] horas = {"20:00", "21:00", "22:00", "18:00"};
        for (int i = 0; i < horas.length; i++) {
            String desde = horas[i];
            String hasta = String.format("%02d:00", Integer.parseInt(desde.substring(0, 2)) + 1);
            Map<String, Object> cuerpo = Map.of("inicio", a(fecha, desde).toString(), "fin", a(fecha, hasta).toString(), "motivo", "Reorganización de la agenda");
            esperarEstado(parchear(base + "/reprogramar", admin, cuerpo), 200);
        }
        MvcResult detalle = obtener(base, admin);
        assertThat((Integer) leer(detalle, "$.vecesReprogramada")).isEqualTo(4);
        assertThat(notificaciones(cliente.id(), "RESERVA_REPROGRAMADA_ADMIN")).isEqualTo(4);
        assertThat((String) leer(detalle, "$.historial[1].actorRol")).isEqualTo("ADMIN");
        assertThat((String) leer(detalle, "$.historial[1].motivo")).isEqualTo("Reorganización de la agenda");

        // el cliente, en cambio, ya no puede
        esperarEstado(parchear("/api/v1/reservas/" + reservaId + "/reprogramar", cliente.token(),
                Map.of("inicio", a(fecha, "17:00").toString(), "fin", a(fecha, "18:00").toString())), 422);

        // la previsualización del admin tampoco tiene límites
        MvcResult previa = postear(base + "/reprogramar/previsualizar", admin,
                Map.of("inicio", a(fecha, "17:00").toString(), "fin", a(fecha, "18:00").toString(), "motivo", "x"));
        assertThat((Boolean) leer(previa, "$.disponible")).isTrue();
    }

    @Test
    void marcarComoCompletadaONoAsistioSiguenLasReglasDelTiempo() throws Exception {
        long reservaId = crearParaCliente(fecha, "19:00", "20:00");
        String url = "/api/v1/admin/reservas/" + reservaId + "/estado";

        esperarEstado(mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(url).header("Authorization", "Bearer " + admin)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{\"estado\":\"COMPLETADA\"}")).andReturn(), 409);
        esperarEstado(parchear(url, admin, Map.of("estado", "NO_ASISTIO")), 409);
        esperarEstado(parchear(url, admin, Map.of("estado", "CANCELADA")), 400);

        mover(reservaId, Instant.now().minus(Duration.ofHours(3)), Instant.now().minus(Duration.ofHours(2)));
        MvcResult completada = parchear(url, admin, Map.of("estado", "COMPLETADA"));
        esperarEstado(completada, 200);
        assertThat((String) leer(completada, "$.estado")).isEqualTo("COMPLETADA");

        // el admin corrige un error: en realidad no asistieron
        MvcResult corregida = parchear(url, admin, Map.of("estado", "NO_ASISTIO"));
        esperarEstado(corregida, 200);
        assertThat((String) leer(corregida, "$.estado")).isEqualTo("NO_ASISTIO");
        assertThat(acciones(reservaId)).containsExactly("CREADA", "COMPLETADA", "NO_ASISTIO");
        assertThat(auditorias("MARCAR_NO_ASISTIO_RESERVA", reservaId)).isEqualTo(1);
    }

    // ===================== Pagos y reembolsos =====================

    @Test
    void registraAbonosYPagosHastaCompletarElValorYDejaConstancia() throws Exception {
        long reservaId = crearParaCliente(fecha, "19:00", "20:00");

        MvcResult abono = pagar(reservaId, 30_000, "NEQUI", "NQ-123");
        esperarEstado(abono, 201);
        assertThat((String) leer(abono, "$.estadoPago")).isEqualTo("ABONADO");
        assertThat((Integer) leer(abono, "$.montoPagado")).isEqualTo(30_000);

        esperarEstado(pagar(reservaId, 60_000, "EFECTIVO", null), 400); // excede el saldo (50.000)
        esperarEstado(pagar(reservaId, 0, "EFECTIVO", null), 400);
        esperarEstado(postear("/api/v1/admin/reservas/" + reservaId + "/pagos", admin, Map.of("monto", 1000, "metodo", "CHEQUE")), 400);

        MvcResult resto = pagar(reservaId, 50_000, "TRANSFERENCIA", "TR-9");
        esperarEstado(resto, 201);
        assertThat((String) leer(resto, "$.estadoPago")).isEqualTo("PAGADO");
        esperarEstado(pagar(reservaId, 1_000, "EFECTIVO", null), 400);

        MvcResult lista = obtener("/api/v1/admin/reservas/" + reservaId + "/pagos", admin);
        List<Object> tipos = leer(lista, "$[*].tipo");
        assertThat(tipos).containsExactly("ABONO", "PAGO");
        assertThat((String) leer(lista, "$[0].registradoPor")).isEqualTo("Administrador Goltime");
        assertThat((String) leer(lista, "$[0].referencia")).isEqualTo("NQ-123");
        assertThat(acciones(reservaId)).containsExactly("CREADA", "PAGO_REGISTRADO", "PAGO_REGISTRADO");
        assertThat(auditorias("REGISTRAR_PAGO", reservaId)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT sum(monto) FROM pagos WHERE reserva_id = ?", Integer.class, reservaId)).isEqualTo(80_000);
    }

    @Test
    void soloSeRegistranPagosEnReservasConfirmadasOYaJugadas() throws Exception {
        long pendiente = solicitarOk(cliente.token(), canchaId, fecha, "19:00", "20:00");
        MvcResult r = pagar(pendiente, 10_000, "EFECTIVO", null);
        esperarEstado(r, 409);
        assertThat((String) leer(r, "$.codigo")).isEqualTo("ESTADO_INVALIDO");
        aprobar(admin, pendiente);
        esperarEstado(pagar(pendiente, 10_000, "EFECTIVO", null), 201);
        // el cliente no registra pagos ni los ve
        esperarEstado(postear("/api/v1/admin/reservas/" + pendiente + "/pagos", cliente.token(), Map.of("monto", 1000, "metodo", "EFECTIVO")), 403);
        esperarEstado(obtener("/api/v1/admin/reservas/" + pendiente + "/pagos", cliente.token()), 403);
    }

    @Test
    void cancelarUnaReservaPagadaDejaUnReembolsoPendienteQueElAdminSalda() throws Exception {
        long reservaId = crearParaCliente(fecha, "19:00", "20:00");
        pagar(reservaId, 80_000, "EFECTIVO", null);
        String base = "/api/v1/admin/reservas/" + reservaId;

        // sin cancelar no hay nada que reembolsar
        MvcResult prematuro = postear(base + "/reembolsos", admin, Map.of("monto", 10_000, "metodo", "EFECTIVO"));
        esperarEstado(prematuro, 409);

        MvcResult cancelada = postear(base + "/cancelar", admin, Map.of("motivo", "Daño en la cancha"));
        assertThat((String) leer(cancelada, "$.estado")).isEqualTo("CANCELADA");
        assertThat((String) leer(cancelada, "$.estadoPago")).isEqualTo("REEMBOLSO_PENDIENTE");
        assertThat((Integer) leer(cancelada, "$.montoPagado")).isEqualTo(80_000);

        esperarEstado(postear(base + "/reembolsos", admin, Map.of("monto", 90_000, "metodo", "EFECTIVO")), 400);
        MvcResult parcial = postear(base + "/reembolsos", admin, Map.of("monto", 30_000, "metodo", "TRANSFERENCIA", "referencia", "DEV-1"));
        esperarEstado(parcial, 201);
        assertThat((String) leer(parcial, "$.estadoPago")).isEqualTo("REEMBOLSO_PENDIENTE");
        assertThat((Integer) leer(parcial, "$.montoPagado")).isEqualTo(50_000);

        MvcResult total = postear(base + "/reembolsos", admin, Map.of("monto", 50_000, "metodo", "EFECTIVO"));
        assertThat((String) leer(total, "$.estadoPago")).isEqualTo("REEMBOLSADO");
        assertThat((Integer) leer(total, "$.montoPagado")).isZero();
        assertThat((List<Object>) leer(obtener(base + "/pagos", admin), "$[*].tipo")).containsExactly("PAGO", "REEMBOLSO", "REEMBOLSO");
        esperarEstado(postear(base + "/reembolsos", admin, Map.of("monto", 1_000, "metodo", "EFECTIVO")), 409);
        assertThat(auditorias("REGISTRAR_REEMBOLSO", reservaId)).isEqualTo(2);
    }

    @Test
    void laCancelacionTardiaDelClienteConAbonoPermiteRetenerUnaParte() throws Exception {
        long reservaId = crearParaCliente(fecha, "19:00", "20:00");
        pagar(reservaId, 80_000, "NEQUI", null);
        mover(reservaId, Instant.now().plus(Duration.ofHours(2)), Instant.now().plus(Duration.ofHours(3)));

        MvcResult cancelada = postear("/api/v1/reservas/" + reservaId + "/cancelar", cliente.token(), Map.of("motivo", "No podemos ir"));
        assertThat((Boolean) leer(cancelada, "$.cancelacionTardia")).isTrue();
        assertThat((String) leer(cancelada, "$.estadoPago")).isEqualTo("REEMBOLSO_PENDIENTE");

        // el admin devuelve 60.000 y se queda con 20.000 (penalidad): "cerrar" da el reembolso por saldado
        MvcResult r = postear("/api/v1/admin/reservas/" + reservaId + "/reembolsos", admin,
                Map.of("monto", 60_000, "metodo", "NEQUI", "cerrar", true));
        esperarEstado(r, 201);
        assertThat((String) leer(r, "$.estadoPago")).isEqualTo("REEMBOLSADO");
        assertThat((Integer) leer(r, "$.montoPagado")).isEqualTo(20_000);
    }

    @Test
    void reprogramarAUnPrecioMayorDejaElSaldoPendiente() throws Exception {
        Map<String, Object> franja = new HashMap<>();
        franja.put("diasSemana", List.of(1, 2, 3, 4, 5, 6, 7));
        franja.put("horaInicio", "20:00");
        franja.put("horaFin", "23:00");
        franja.put("precioHora", 100_000);
        esperarEstado(postear("/api/v1/admin/canchas/" + canchaId + "/tarifas", admin, franja), 201);

        long reservaId = crearParaCliente(fecha, "19:00", "20:00"); // 80.000
        pagar(reservaId, 80_000, "EFECTIVO", null);
        MvcResult r = parchear("/api/v1/admin/reservas/" + reservaId + "/reprogramar", admin,
                Map.of("inicio", a(fecha, "21:00").toString(), "fin", a(fecha, "22:00").toString(), "motivo", "Cambio de horario"));
        esperarEstado(r, 200);
        assertThat((Integer) leer(r, "$.precioFinal")).isEqualTo(100_000);
        assertThat((String) leer(r, "$.estadoPago")).isEqualTo("ABONADO");
        esperarEstado(pagar(reservaId, 20_000, "EFECTIVO", null), 201);
    }

    // ===================== Listado, filtros y CSV =====================

    @Test
    void elListadoSeFiltraPorPagoOrigenCanchaYFechas() throws Exception {
        long otraCancha = crearCancha(admin, sedeId, "Cancha B");
        long deAdmin = crearParaCliente(fecha, "19:00", "20:00");
        pagar(deAdmin, 80_000, "EFECTIVO", null);
        long deWeb = solicitarOk(cliente.token(), otraCancha, enDias(8), "19:00", "20:00");

        assertThat(idsDe("sedeId=" + sedeId + "&estadoPago=PAGADO")).containsExactly(deAdmin);
        assertThat(idsDe("sedeId=" + sedeId + "&origen=WEB")).containsExactly(deWeb);
        assertThat(idsDe("sedeId=" + sedeId + "&origen=ADMIN")).containsExactly(deAdmin);
        assertThat(idsDe("canchaId=" + otraCancha)).containsExactly(deWeb);
        assertThat(idsDe("sedeId=" + sedeId + "&desde=" + enDias(7) + "&hasta=" + enDias(9))).containsExactly(deWeb);
        assertThat(idsDe("sedeId=" + sedeId + "&desde=" + fecha + "&hasta=" + fecha)).containsExactly(deAdmin);
        assertThat(idsDe("sedeId=" + sedeId + "&estado=PENDIENTE")).containsExactly(deWeb);
    }

    private List<Long> idsDe(String consulta) throws Exception {
        List<Object> ids = leer(obtener("/api/v1/admin/reservas?" + consulta, admin), "$.contenido[*].id");
        return ids.stream().map(o -> ((Number) o).longValue()).sorted().toList();
    }

    @Test
    void exportaElListadoFiltradoACsvConTildesYSinInyeccionDeFormulas() throws Exception {
        Map<String, Object> cuerpo = cuerpoAdmin(canchaId, a(fecha, "19:00"), a(fecha, "20:00"), null, "=CMD|' /C calc'!A0", "3150000000");
        cuerpo.put("nombreEquipo", "@SUMA(1;1) Ñandú");
        esperarEstado(postear("/api/v1/admin/reservas", admin, cuerpo), 201);
        solicitarOk(cliente.token(), canchaId, fecha, "21:00", "22:00");

        MvcResult r = obtener("/api/v1/admin/reservas/exportar?sedeId=" + sedeId + "&origen=ADMIN", admin);

        esperarEstado(r, 200);
        assertThat(r.getResponse().getContentType()).startsWith("text/csv");
        assertThat(r.getResponse().getHeader("Content-Disposition")).contains("attachment").contains("reservas-goltime.csv");
        String csv = new String(r.getResponse().getContentAsByteArray(), java.nio.charset.StandardCharsets.UTF_8);
        assertThat(csv).startsWith("﻿\"".substring(0, 1) + "Código,Estado,Origen");
        String[] lineas = csv.split("\r\n");
        assertThat(lineas).hasSize(2); // encabezado + 1 reserva (el filtro excluye la de la web)
        assertThat(lineas[1]).contains("\"CONFIRMADA\"", "\"ADMIN\"", "\"Cancha A\"");
        assertThat(lineas[1]).contains("\"'=CMD|' /C calc'!A0\"").contains("\"'@SUMA(1;1) Ñandú\"");
        assertThat(lineas[1]).doesNotContain(",\"=");
        esperarEstado(obtener("/api/v1/admin/reservas/exportar", cliente.token()), 403);
    }

    // ===================== Agenda =====================

    @Test
    void laAgendaMuestraReservasYTambienLasComponentesDeUnaCanchaCompuesta() throws Exception {
        long b = crearCancha(admin, sedeId, "Cancha B");
        long f8 = crearCancha(admin, sedeId, "Cancha F8", 8, 160_000, List.of(canchaId, b));
        crearAdmin(f8, fecha, "19:00", "20:00", null, "Don Pedro", "3201112233");
        crearAdmin(canchaId, fecha, "21:00", "22:00", cliente.id(), null, null);

        MvcResult r = obtener("/api/v1/admin/agenda?sedeId=" + sedeId + "&desde=" + fecha + "&hasta=" + fecha, admin);

        esperarEstado(r, 200);
        List<Object> columnas = leer(r, "$.canchas[*].nombre");
        assertThat(columnas).containsExactlyInAnyOrder("Cancha A", "Cancha B", "Cancha F8");
        // la reserva de la F8 aparece en la F8 y en sus dos componentes (que quedan ocupadas)
        List<Object> deLaF8 = leer(r, "$.elementos[?(@.codigo && @.cliente=='Don Pedro')].canchaId");
        assertThat(deLaF8).hasSize(3);
        List<Object> vias = leer(r, "$.elementos[?(@.cliente=='Don Pedro' && @.canchaReservaNombre=='Cancha F8')].canchaId");
        assertThat(vias).as("en A y B se indica que se ocupan por la F8").hasSize(2);
        assertThat((List<?>) leer(r, "$.elementos[?(@.cliente=='Ana Pérez')]")).hasSize(1);
        assertThat((List<Object>) leer(r, "$.elementos[?(@.cliente=='Ana Pérez')].estado")).containsExactly("CONFIRMADA");
    }

    @Test
    void laAgendaValidaElRangoYSoloLaVeElAdmin() throws Exception {
        esperarEstado(obtener("/api/v1/admin/agenda?sedeId=" + sedeId + "&desde=" + fecha + "&hasta=" + fecha.plusDays(40), admin), 400);
        esperarEstado(obtener("/api/v1/admin/agenda?sedeId=" + sedeId + "&desde=" + fecha + "&hasta=" + fecha.minusDays(1), admin), 400);
        esperarEstado(obtener("/api/v1/admin/agenda?sedeId=" + sedeId + "&desde=" + fecha + "&hasta=" + fecha, cliente.token()), 403);
        esperarEstado(mvc.perform(get("/api/v1/admin/agenda?sedeId=" + sedeId + "&desde=" + fecha + "&hasta=" + fecha)).andReturn(), 401);
    }
}
