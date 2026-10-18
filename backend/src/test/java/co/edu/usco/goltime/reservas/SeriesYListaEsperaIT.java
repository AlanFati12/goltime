package co.edu.usco.goltime.reservas;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

class SeriesYListaEsperaIT extends BaseReservasIT {

    private String admin;
    private long canchaId;
    private Sesion cliente;
    private LocalDate fecha;

    @BeforeEach
    void preparar() throws Exception {
        admin = ingresarAdmin().token();
        canchaId = crearCancha(admin, crearSede(admin), "Cancha S");
        cliente = nuevoClienteConSesion();
        fecha = diaHabil(10);
    }

    // ----- Series -----

    private Map<String, Object> serie(LocalDate primera, String desde, String hasta, int semanas) {
        Map<String, Object> m = new HashMap<>();
        m.put("canchaId", canchaId);
        m.put("inicio", a(primera, desde).toString());
        m.put("fin", a(primera, hasta).toString());
        m.put("semanas", semanas);
        m.put("clienteId", cliente.id());
        m.put("nombreEquipo", "Los Martes");
        return m;
    }

    private List<String> estados(MvcResult r) throws Exception {
        return leer(r, "$[*].estado");
    }

    @Test
    void laPrevisualizacionMarcaCadaFechaYCrearExigeAceptarLasOcupadas() throws Exception {
        long ocupante = solicitarOk(nuevoClienteConSesion().token(), canchaId, fecha.plusWeeks(1), "19:00", "20:00"); // choca con la semana 2
        MvcResult previa = postear("/api/v1/admin/series/previsualizar", admin, serie(fecha, "19:00", "20:00", 4));

        esperarEstado(previa, 200);
        assertThat(estados(previa)).containsExactly("DISPONIBLE", "OCUPADA", "DISPONIBLE", "DISPONIBLE");
        List<Number> precios = leer(previa, "$[*].precio");
        assertThat(precios.stream().map(Number::intValue)).containsExactly(80_000, 0, 80_000, 80_000);

        // sin aceptar: nada se crea
        MvcResult sin = postear("/api/v1/admin/series", admin, serie(fecha, "19:00", "20:00", 4));
        esperarEstado(sin, 409);
        assertThat((String) leer(sin, "$.codigo")).isEqualTo("CONFLICTOS_SIN_CONFIRMAR");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reservas WHERE cliente_id = ?", Integer.class, cliente.id())).isZero();

        Map<String, Object> aceptando = serie(fecha, "19:00", "20:00", 4);
        aceptando.put("omitirOcupadas", true);
        MvcResult creada = postear("/api/v1/admin/series", admin, aceptando);

        esperarEstado(creada, 201);
        assertThat((List<?>) leer(creada, "$.reservas")).hasSize(3);
        assertThat((List<?>) leer(creada, "$.omitidas")).hasSize(1);
        assertThat((Integer) leer(creada, "$.omitidas[0].numero")).isEqualTo(2);
        long serieId = ((Number) leer(creada, "$.serieId")).longValue();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reservas WHERE serie_id = ? AND estado = 'CONFIRMADA' AND origen = 'RECURRENTE'", Integer.class, serieId))
                .isEqualTo(3);
        // un solo aviso para todas, no uno por reserva
        assertThat(notificaciones(cliente.id(), "RESERVA_CREADA_ADMIN")).isEqualTo(1);
        assertThat(estadoDe(ocupante)).isEqualTo("PENDIENTE");

        MvcResult lista = obtener("/api/v1/admin/series", admin);
        List<Number> activas = leer(lista, "$[?(@.id==" + serieId + ")].reservasActivas");
        assertThat(activas.get(0).intValue()).isEqualTo(3);
    }

    @Test
    void todasLasFechasMantienenLaMismaHoraDeParedEnBogota() throws Exception {
        Map<String, Object> cuerpo = serie(fecha, "20:00", "21:00", 3);
        MvcResult r = postear("/api/v1/admin/series", admin, cuerpo);

        esperarEstado(r, 201);
        List<Integer> horas = jdbc.queryForList("SELECT extract(hour FROM inicio AT TIME ZONE 'America/Bogota')::int FROM reservas WHERE serie_id = ? ORDER BY inicio", Integer.class,
                ((Number) leer(r, "$.serieId")).longValue());
        assertThat(horas).containsExactly(20, 20, 20);
        List<Integer> dias = jdbc.queryForList("SELECT extract(isodow FROM inicio AT TIME ZONE 'America/Bogota')::int FROM reservas WHERE serie_id = ?", Integer.class,
                ((Number) leer(r, "$.serieId")).longValue());
        assertThat(dias).containsOnly(fecha.getDayOfWeek().getValue());
    }

    @Test
    void cancelarLaSerieLiberaLasFechasFuturasYExigeMotivo() throws Exception {
        MvcResult r = postear("/api/v1/admin/series", admin, serie(fecha, "19:00", "20:00", 3));
        long serieId = ((Number) leer(r, "$.serieId")).longValue();
        List<Long> ids = jdbc.queryForList("SELECT id FROM reservas WHERE serie_id = ? ORDER BY inicio", Long.class, serieId);

        esperarEstado(postear("/api/v1/admin/series/" + serieId + "/cancelar", admin, Map.of("motivo", " ")), 400);
        esperarEstado(postear("/api/v1/admin/series/" + serieId + "/cancelar", cliente.token(), Map.of("motivo", "x")), 403);
        MvcResult cancelada = postear("/api/v1/admin/series/" + serieId + "/cancelar", admin, Map.of("motivo", "El equipo se disolvió"));

        esperarEstado(cancelada, 200);
        assertThat((Integer) leer(cancelada, "$.canceladas")).isEqualTo(3);
        ids.forEach(id -> {
            assertThat(estadoDe(id)).isEqualTo("CANCELADA");
            assertThat(ocupaciones(id)).isZero();
        });
        assertThat(jdbc.queryForObject("SELECT motivo_cancelacion FROM reservas WHERE id = ?", String.class, ids.get(0))).startsWith("Serie cancelada");
        esperarEstado(solicitar(nuevoClienteConSesion().token(), canchaId, fecha, "19:00", "20:00"), 201); // el horario volvió a estar libre
        assertThat((Integer) leer(postear("/api/v1/admin/series/" + serieId + "/cancelar", admin, Map.of("motivo", "otra vez")), "$.canceladas")).as("idempotente").isZero();
    }

    @Test
    void validaLasSeriesAntesDeCrearlas() throws Exception {
        esperarEstado(postear("/api/v1/admin/series/previsualizar", admin, serie(fecha, "19:00", "20:00", 1)), 400);
        esperarEstado(postear("/api/v1/admin/series/previsualizar", admin, serie(fecha, "19:00", "20:00", 27)), 400);
        Map<String, Object> invertida = serie(fecha, "19:00", "20:00", 3);
        invertida.put("fin", a(fecha, "18:00").toString());
        esperarEstado(postear("/api/v1/admin/series/previsualizar", admin, invertida), 400);
        Map<String, Object> cruzaMedianoche = serie(fecha, "23:00", "20:00", 3);
        cruzaMedianoche.put("fin", a(fecha.plusDays(1), "01:00").toString());
        esperarEstado(postear("/api/v1/admin/series/previsualizar", admin, cruzaMedianoche), 400);

        // fuera de horario: todas las fechas, y sin ninguna disponible no hay nada que crear
        MvcResult previa = postear("/api/v1/admin/series/previsualizar", admin, serie(fecha, "03:00", "04:00", 3));
        assertThat(estados(previa)).containsOnly("FUERA_DE_HORARIO");
        Map<String, Object> aceptando = serie(fecha, "03:00", "04:00", 3);
        aceptando.put("omitirOcupadas", true);
        esperarEstado(postear("/api/v1/admin/series", admin, aceptando), 409);

        esperarEstado(postear("/api/v1/admin/series/previsualizar", cliente.token(), serie(fecha, "19:00", "20:00", 3)), 403);
        esperarEstado(postear("/api/v1/admin/series/previsualizar", null, serie(fecha, "19:00", "20:00", 3)), 401);
    }

    // ----- Lista de espera -----

    private MvcResult esperar(String token, LocalDate dia, String desde, String hasta) throws Exception {
        return postear("/api/v1/me/lista-espera", token, Map.of("canchaId", canchaId, "inicio", a(dia, desde).toString(), "fin", a(dia, hasta).toString()));
    }

    private List<String> estadosDeEspera(String token) throws Exception {
        return leer(obtener("/api/v1/me/lista-espera", token), "$[*].estado");
    }

    @Test
    void soloSePuedeEsperarUnHorarioOcupadoSinDuplicarNiPasarseDelLimite() throws Exception {
        Sesion esperando = nuevoClienteConSesion();
        solicitarOk(cliente.token(), canchaId, fecha, "19:00", "20:00");

        MvcResult libre = esperar(esperando.token(), fecha, "21:00", "22:00");
        esperarEstado(libre, 409);
        assertThat((String) leer(libre, "$.detail")).contains("libre");

        esperarEstado(esperar(esperando.token(), fecha, "19:00", "20:00"), 201);
        esperarEstado(esperar(esperando.token(), fecha, "19:00", "20:00"), 409); // duplicada
        esperarEstado(esperar(null, fecha, "19:00", "20:00"), 401);

        // hasta 5 esperas a la vez
        for (int i = 1; i < 5; i++) {
            solicitarOk(nuevoClienteConSesion().token(), canchaId, diaHabil(10 + i), "19:00", "20:00");
            esperarEstado(esperar(esperando.token(), diaHabil(10 + i), "19:00", "20:00"), 201);
        }
        solicitarOk(nuevoClienteConSesion().token(), canchaId, diaHabil(20), "19:00", "20:00");
        esperarEstado(esperar(esperando.token(), diaHabil(20), "19:00", "20:00"), 422);
    }

    @Test
    void alCancelarseLaReservaAvisaATodosLosQueEsperabanYQuienReservaCumpleLaSuya() throws Exception {
        long reservaId = solicitarOk(cliente.token(), canchaId, fecha, "19:00", "20:00");
        Sesion b = nuevoClienteConSesion();
        Sesion c = nuevoClienteConSesion();
        long esperaB = id(esperar(b.token(), fecha, "19:00", "20:00"));
        esperar(c.token(), fecha, "19:00", "20:00");
        assertThat(estadosDeEspera(b.token())).containsExactly("ESPERANDO");

        esperarEstado(postear("/api/v1/reservas/" + reservaId + "/cancelar", cliente.token(), Map.of()), 200);

        assertThat(notificaciones(b.id(), "ESPERA_LIBERADA")).isEqualTo(1);
        assertThat(notificaciones(c.id(), "ESPERA_LIBERADA")).isEqualTo(1);
        assertThat(notificaciones(cliente.id(), "ESPERA_LIBERADA")).isZero();
        assertThat(estadosDeEspera(b.token())).containsExactly("NOTIFICADA");

        // B reserva y su espera se cumple; la de C sigue esperando el aviso de otra cancelación
        esperarEstado(solicitar(b.token(), canchaId, fecha, "19:00", "20:00"), 201);
        assertThat(estadosDeEspera(b.token())).isEmpty();
        assertThat(jdbc.queryForObject("SELECT estado FROM lista_espera WHERE id = ?", String.class, esperaB)).isEqualTo("CUMPLIDA");
        assertThat(estadosDeEspera(c.token())).containsExactly("NOTIFICADA");
    }

    @Test
    void elRechazoTambienAvisaYUnaEsperaCanceladaNoRecibeNada() throws Exception {
        long reservaId = solicitarOk(cliente.token(), canchaId, fecha, "19:00", "20:00");
        Sesion sale = nuevoClienteConSesion();
        Sesion queda = nuevoClienteConSesion();
        long esperaQueSale = id(esperar(sale.token(), fecha, "19:00", "20:00"));
        esperar(queda.token(), fecha, "19:00", "20:00");

        esperarEstado(delete("/api/v1/me/lista-espera/" + esperaQueSale, queda.token()), 404); // ajena: no existe para otro
        esperarEstado(delete("/api/v1/me/lista-espera/" + esperaQueSale, sale.token()), 204);
        assertThat(estadosDeEspera(sale.token())).isEmpty();

        esperarEstado(postear("/api/v1/admin/reservas/" + reservaId + "/rechazar", admin, Map.of("motivo", "No hay árbitro disponible")), 200);

        assertThat(notificaciones(queda.id(), "ESPERA_LIBERADA")).isEqualTo(1);
        assertThat(notificaciones(sale.id(), "ESPERA_LIBERADA")).isZero();
    }

    @Test
    void siElHorarioSigueOcupadoPorOtraReservaNoSeAvisa() throws Exception {
        // dos reservas contiguas: se libera la de las 19:00 pero quien espera de 19:00 a 21:00 sigue bloqueado por la de las 20:00
        long primera = solicitarOk(cliente.token(), canchaId, fecha, "19:00", "20:00");
        solicitarOk(nuevoClienteConSesion().token(), canchaId, fecha, "20:00", "21:00");
        Sesion espera = nuevoClienteConSesion();
        esperarEstado(esperar(espera.token(), fecha, "19:00", "21:00"), 201);

        esperarEstado(postear("/api/v1/reservas/" + primera + "/cancelar", cliente.token(), Map.of()), 200);

        assertThat(notificaciones(espera.id(), "ESPERA_LIBERADA")).isZero();
        assertThat(estadosDeEspera(espera.token())).containsExactly("ESPERANDO");
    }

    private MvcResult delete(String url, String token) throws Exception {
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(url).header("Authorization", "Bearer " + token)).andReturn();
    }
}
