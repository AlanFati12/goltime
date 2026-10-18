package co.edu.usco.goltime.estadisticas;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import co.edu.usco.goltime.reservas.BaseReservasIT;

class EstadisticasIT extends BaseReservasIT {

    private Sesion admin;
    private long sedeId;
    private long canchaId;
    private LocalDate dia;
    private Sesion clienteA;

    @BeforeEach
    void preparar() throws Exception {
        admin = ingresarAdmin();
        sedeId = crearSede(admin.token());
        canchaId = crearCancha(admin.token(), sedeId, "Cancha E");
        dia = diaHabil(-12);
        clienteA = nuevoClienteConSesion();
    }

    // ----- Datos directos: las estadísticas leen reservas y pagos ya registrados -----

    private static OffsetDateTime utc(Instant i) {
        return OffsetDateTime.ofInstant(i, ZoneOffset.UTC);
    }

    private long mostrador(String nombre) {
        return jdbc.queryForObject("INSERT INTO clientes_mostrador (nombre, telefono) VALUES (?, ?) RETURNING id", Long.class, nombre,
                "300" + (int) (Math.random() * 9_000_000 + 1_000_000));
    }

    private long reserva(long cancha, LocalDate fecha, String desde, String hasta, String estado, String origen, int precio, int pagado, Long cliente,
            Long mostradorId) {
        Instant inicio = a(fecha, desde);
        Instant fin = a(fecha, hasta);
        long id = jdbc.queryForObject("""
                INSERT INTO reservas (codigo, cliente_id, cliente_mostrador_id, cancha_id, inicio, fin, estado, origen, precio_base, precio_final, monto_pagado)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id""", Long.class, "E-" + UUID.randomUUID().toString().substring(0, 12), cliente, mostradorId,
                cancha, utc(inicio), utc(fin), estado, origen, precio, precio, pagado);
        if (List.of("CONFIRMADA", "COMPLETADA", "NO_ASISTIO", "PENDIENTE").contains(estado)) {
            jdbc.update("INSERT INTO ocupaciones_cancha (cancha_id, rango, tipo, reserva_id) VALUES (?, tstzrange(?, ?, '[)'), 'RESERVA', ?)", cancha, utc(inicio),
                    utc(fin), id);
        }
        return id;
    }

    private void pago(long reservaId, LocalDate fecha, String hora, String tipo, int monto) {
        jdbc.update("INSERT INTO pagos (reserva_id, monto, metodo, tipo, registrado_por, fecha) VALUES (?, ?, 'EFECTIVO', ?, ?, ?)", reservaId, monto, tipo,
                admin.id(), utc(a(fecha, hora)));
    }

    /** Un día completo de actividad: 2 completadas, 1 cancelada, 1 no asistió y 1 partido de torneo. */
    private void diaDeActividad() {
        long mostradorB = mostrador("Cliente Mostrador B");
        long r1 = reserva(canchaId, dia, "18:00", "19:00", "COMPLETADA", "WEB", 80_000, 80_000, clienteA.id(), null);
        long r2 = reserva(canchaId, dia, "19:00", "21:00", "COMPLETADA", "ADMIN", 160_000, 100_000, clienteA.id(), null);
        reserva(canchaId, dia, "20:00", "21:00", "CANCELADA", "WEB", 80_000, 0, clienteA.id(), null);
        reserva(canchaId, dia, "21:00", "22:00", "NO_ASISTIO", "ADMIN", 80_000, 0, null, mostradorB);
        reserva(canchaId, dia, "22:00", "23:00", "CONFIRMADA", "TORNEO", 0, 0, null, null);
        pago(r1, dia, "17:30", "PAGO", 80_000);
        pago(r2, dia, "18:00", "ABONO", 100_000);
        pago(r2, dia, "20:00", "REEMBOLSO", 20_000);
    }

    private String url(String ruta, LocalDate desde, LocalDate hasta) {
        return "/api/v1/admin/estadisticas/" + ruta + "?desde=" + desde + "&hasta=" + hasta + "&sedeId=" + sedeId;
    }

    // ----- Pruebas -----

    @Test
    void elResumenSeparaIngresosRealesRecaudoYPorCobrarSinContarLosTorneos() throws Exception {
        diaDeActividad();

        MvcResult r = obtener(url("resumen", dia, dia), admin.token());

        esperarEstado(r, 200);
        assertThat(((Number) leer(r, "$.actual.ingresosReales")).longValue()).isEqualTo(240_000); // solo COMPLETADA
        assertThat(((Number) leer(r, "$.actual.recaudado")).longValue()).isEqualTo(160_000); // 80.000 + 100.000 - 20.000
        assertThat(((Number) leer(r, "$.actual.porCobrar")).longValue()).isEqualTo(60_000);
        assertThat((Integer) leer(r, "$.actual.reservas")).isEqualTo(4); // sin el partido de torneo
        assertThat((Integer) leer(r, "$.actual.completadas")).isEqualTo(2);
        assertThat((Integer) leer(r, "$.actual.canceladas")).isEqualTo(1);
        assertThat((Integer) leer(r, "$.actual.noAsistio")).isEqualTo(1);
        assertThat(((Number) leer(r, "$.actual.tasaCancelacion")).doubleValue()).isEqualTo(25.0);
        assertThat(((Number) leer(r, "$.actual.tasaNoAsistencia")).doubleValue()).isEqualTo(33.3);
        assertThat(((Number) leer(r, "$.actual.horasJugadas")).doubleValue()).isEqualTo(3.0);
        assertThat(((Number) leer(r, "$.actual.ticketPromedio")).longValue()).isEqualTo(120_000);
        assertThat((Integer) leer(r, "$.actual.clientesUnicos")).isEqualTo(1);
        assertThat((Integer) leer(r, "$.actual.partidosTorneo")).isEqualTo(1);
        assertThat((Integer) leer(r, "$.actual.porEstado.COMPLETADA")).isEqualTo(2);
        assertThat((Integer) leer(r, "$.actual.porEstado.CANCELADA")).isEqualTo(1);
    }

    @Test
    void laOcupacionUsaElHorarioEfectivoYCuentaLosPartidosDeTorneo() throws Exception {
        diaDeActividad();
        int abiertos = jdbc.queryForObject("SELECT minuto_cierre - minuto_apertura FROM sede_horarios WHERE sede_id = ? AND dia_semana = ? AND NOT cerrado",
                Integer.class, sedeId, dia.getDayOfWeek().getValue());

        MvcResult r = obtener(url("resumen", dia, dia), admin.token());

        // 60 + 120 + 60 (no asistió también ocupó la cancha) + 60 del torneo = 300 minutos
        double esperado = Math.round(1000.0 * 300 / abiertos) / 10.0;
        assertThat(((Number) leer(r, "$.actual.ocupacionPct")).doubleValue()).isEqualTo(esperado);

        // si la cancha tiene un horario propio más corto, la ocupación sube
        jdbc.update("INSERT INTO cancha_horarios (cancha_id, dia_semana, cerrado, minuto_apertura, minuto_cierre) VALUES (?, ?, FALSE, 17 * 60, 23 * 60)", canchaId,
                dia.getDayOfWeek().getValue());
        MvcResult propio = obtener(url("resumen", dia, dia), admin.token());
        assertThat(((Number) leer(propio, "$.actual.ocupacionPct")).doubleValue()).isEqualTo(Math.round(1000.0 * 300 / 360) / 10.0);
    }

    @Test
    void compararConElPeriodoAnteriorMideElCambio() throws Exception {
        diaDeActividad();
        reserva(canchaId, dia.minusDays(1), "18:00", "19:00", "COMPLETADA", "WEB", 60_000, 60_000, clienteA.id(), null);

        MvcResult r = obtener(url("resumen", dia, dia), admin.token());

        assertThat(((Number) leer(r, "$.anterior.ingresosReales")).longValue()).isEqualTo(60_000);
        assertThat(((Number) leer(r, "$.variaciones.ingresosReales")).doubleValue()).isEqualTo(300.0); // 240.000 vs 60.000
        assertThat((String) leer(r, "$.periodoAnterior.desde")).isEqualTo(dia.minusDays(1).toString());
    }

    @Test
    void laSerieIncluyeLosDiasSinMovimientoYAgrupaPorSemanaYMes() throws Exception {
        diaDeActividad();

        MvcResult serie = obtener(url("serie", dia.minusDays(1), dia.plusDays(1)) + "&agrupacion=DIA", admin.token());

        esperarEstado(serie, 200);
        List<String> fechas = leer(serie, "$.puntos[*].fecha");
        assertThat(fechas).containsExactly(dia.minusDays(1).toString(), dia.toString(), dia.plusDays(1).toString());
        List<Number> ingresos = leer(serie, "$.puntos[*].ingresos");
        assertThat(ingresos.stream().map(Number::longValue)).containsExactly(0L, 240_000L, 0L);

        MvcResult mes = obtener(url("serie", dia.withDayOfMonth(1), dia.withDayOfMonth(1).plusMonths(1)) + "&agrupacion=MES", admin.token());
        List<Number> porMes = leer(mes, "$.puntos[*].ingresos");
        assertThat(porMes.stream().mapToLong(Number::longValue).sum()).isEqualTo(240_000L);
        assertThat((List<?>) leer(mes, "$.puntos")).hasSize(2);
    }

    @Test
    void porCanchaTraeIngresosHorasYOcupacion() throws Exception {
        diaDeActividad();

        MvcResult r = obtener(url("canchas", dia, dia), admin.token());

        esperarEstado(r, 200);
        assertThat((List<?>) leer(r, "$")).hasSize(1);
        assertThat((String) leer(r, "$[0].cancha")).isEqualTo("Cancha E");
        assertThat((Integer) leer(r, "$[0].completadas")).isEqualTo(2);
        assertThat(((Number) leer(r, "$[0].horas")).doubleValue()).isEqualTo(3.0);
        assertThat(((Number) leer(r, "$[0].ingresos")).longValue()).isEqualTo(240_000);
        assertThat(((Number) leer(r, "$[0].ocupacionPct")).doubleValue()).isGreaterThan(0);
    }

    @Test
    void lasHorasPicoCuentanCadaHoraQueCubreLaReservaSinLosTorneos() throws Exception {
        diaDeActividad();

        MvcResult r = obtener(url("horas-pico", dia, dia), admin.token());

        esperarEstado(r, 200);
        int diaSemana = dia.getDayOfWeek().getValue();
        List<Integer> horas = leer(r, "$.celdas[?(@.diaSemana==" + diaSemana + ")].hora");
        assertThat(horas).containsExactly(18, 19, 20, 21); // 18-19, 19-21 (2 horas), 21-22; el torneo (22) no cuenta
        assertThat((Integer) leer(r, "$.maximo")).isEqualTo(1);
    }

    @Test
    void losClientesFrecuentesSoloIncluyenAQuienesCompletaronReservas() throws Exception {
        diaDeActividad();

        MvcResult r = obtener(url("clientes-frecuentes", dia, dia), admin.token());

        esperarEstado(r, 200);
        assertThat((List<?>) leer(r, "$")).hasSize(1); // el cliente de mostrador solo tuvo un no asistió
        assertThat((Integer) leer(r, "$[0].completadas")).isEqualTo(2);
        assertThat((Integer) leer(r, "$[0].canceladas")).isEqualTo(1);
        assertThat(((Number) leer(r, "$[0].gasto")).longValue()).isEqualTo(240_000);
        assertThat((Boolean) leer(r, "$[0].mostrador")).isFalse();
    }

    @Test
    void filtraPorCanchaYSedeSinMezclarDatosAjenos() throws Exception {
        diaDeActividad();
        long otraSede = crearSede(admin.token());
        long otraCancha = crearCancha(admin.token(), otraSede, "Otra");
        reserva(otraCancha, dia, "18:00", "19:00", "COMPLETADA", "WEB", 500_000, 500_000, clienteA.id(), null);

        MvcResult mia = obtener(url("resumen", dia, dia), admin.token());
        MvcResult soloOtraCancha = obtener("/api/v1/admin/estadisticas/resumen?desde=" + dia + "&hasta=" + dia + "&canchaId=" + otraCancha, admin.token());

        assertThat(((Number) leer(mia, "$.actual.ingresosReales")).longValue()).isEqualTo(240_000);
        assertThat(((Number) leer(soloOtraCancha, "$.actual.ingresosReales")).longValue()).isEqualTo(500_000);
    }

    @Test
    void validaElPeriodoYExigeSerAdministrador() throws Exception {
        esperarEstado(obtener(url("resumen", dia, dia.minusDays(1)), admin.token()), 400);
        esperarEstado(obtener(url("resumen", dia.minusDays(400), dia), admin.token()), 400);
        esperarEstado(obtener("/api/v1/admin/estadisticas/resumen", admin.token()), 200); // últimos 30 días
        esperarEstado(obtener("/api/v1/admin/estadisticas/resumen", clienteA.token()), 403);
        esperarEstado(obtener("/api/v1/admin/estadisticas/resumen", null), 401);
    }

    @Test
    void exportaCsvConBomYLosIndicadores() throws Exception {
        diaDeActividad();

        MvcResult r = obtener(url("exportar", dia, dia), admin.token());

        esperarEstado(r, 200);
        String csv = r.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(csv).startsWith("﻿Periodo,");
        assertThat(csv).contains("\"Ingresos reales (COP)\",240000").contains("\"Recaudado (COP)\",160000").contains("\"Cancha E\"");
        assertThat(r.getResponse().getHeader("Content-Disposition")).contains("estadisticas-goltime.csv");
        assertThat(Map.of("ok", true)).isNotEmpty();
    }

    @Test
    void elTableroMuestraLoDeHoyLoPorCobrarYElEstadoDeLasCanchas() throws Exception {
        LocalDate hoy = co.edu.usco.goltime.compartido.dominio.ZonaHoraria.fechaLocal(Instant.now());
        long conSaldo = reserva(canchaId, dia, "10:00", "11:00", "COMPLETADA", "WEB", 90_000, 30_000, clienteA.id(), null);
        jdbc.update("UPDATE reservas SET estado_pago = 'ABONADO' WHERE id = ?", conSaldo);
        reserva(canchaId, hoy.plusDays(2), "06:00", "07:00", "PENDIENTE", "WEB", 80_000, 0, clienteA.id(), null);

        MvcResult r = obtener("/api/v1/admin/estadisticas/tablero", admin.token());

        esperarEstado(r, 200);
        assertThat((Integer) leer(r, "$.cobros.reservasConSaldo")).isPositive();
        assertThat(((Number) leer(r, "$.cobros.saldoPendiente")).longValue()).isGreaterThanOrEqualTo(60_000);
        assertThat((List<?>) leer(r, "$.proximosDias")).hasSize(7);
        assertThat((Integer) leer(r, "$.proximosDias[2].pendientes")).isPositive();
        assertThat((List<Integer>) leer(r, "$.canchas[*].canchaId")).contains((int) canchaId);
        assertThat((Integer) leer(r, "$.clientes.activos")).isPositive();
        esperarEstado(obtener("/api/v1/admin/estadisticas/tablero", clienteA.token()), 403);
    }
}
