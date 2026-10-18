package co.edu.usco.goltime.reservas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class SolicitudReservasIT extends BaseReservasIT {

    private String admin;
    private long sedeId;
    private long canchaId;
    private Sesion cliente;
    private LocalDate fecha;

    @BeforeEach
    void preparar() throws Exception {
        admin = ingresarAdmin().token();
        sedeId = crearSede(admin);
        canchaId = crearCancha(admin, sedeId, "Cancha A");
        cliente = nuevoClienteConSesion();
        fecha = enDias(5);
    }

    private MvcResult solicitarConClave(String token, String clave, long cancha, LocalDate f, String desde, String hasta) throws Exception {
        return mvc.perform(post("/api/v1/reservas").header("Authorization", "Bearer " + token).header("Idempotency-Key", clave)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(cuerpoReserva(cancha, a(f, desde), a(f, hasta))))).andReturn();
    }

    // ----- Creación -----

    @Test
    void unaSolicitudValidaQuedaPendienteConPrecioCodigoYHistorial() throws Exception {
        Instant antes = Instant.now();
        MvcResult r = solicitar(cliente.token(), canchaId, fecha, "19:00", "20:00");

        esperarEstado(r, 201);
        long reservaId = id(r);
        assertThat((String) leer(r, "$.estado")).isEqualTo("PENDIENTE");
        assertThat((String) leer(r, "$.origen")).isEqualTo("WEB");
        assertThat((String) leer(r, "$.codigo")).matches("GT-\\d{4}-\\d{6}");
        assertThat((Integer) leer(r, "$.precioFinal")).isEqualTo(80_000);
        assertThat((Integer) leer(r, "$.descuento")).isZero();
        assertThat((String) leer(r, "$.estadoPago")).isEqualTo("PENDIENTE");
        assertThat((String) leer(r, "$.nombreEquipo")).isEqualTo("Los Cracks");
        assertThat((String) leer(r, "$.cancha.nombre")).isEqualTo("Cancha A");
        assertThat((Boolean) leer(r, "$.puedeCancelar")).isTrue();
        assertThat((Boolean) leer(r, "$.puedeReprogramar")).isTrue();

        // expira a las 12 h (el inicio está a 5 días)
        Instant expira = Instant.parse(leer(r, "$.expiraEn"));
        assertThat(Duration.between(antes, expira).toHours()).isBetween(11L, 12L);

        assertThat(estadoDe(reservaId)).isEqualTo("PENDIENTE");
        assertThat(ocupaciones(reservaId)).isEqualTo(1);
        assertThat(acciones(reservaId)).containsExactly("CREADA");
        Long adminId = jdbc.queryForObject("SELECT id FROM usuarios WHERE correo = ?", Long.class, ADMIN_CORREO);
        assertThat(notificaciones(adminId, "RESERVA_SOLICITADA")).isGreaterThanOrEqualTo(1);
    }

    @Test
    void elPrecioSeCalculaEnElServidorConLasTarifasDeLaCancha() throws Exception {
        Map<String, Object> franja = new HashMap<>();
        franja.put("diasSemana", List.of(1, 2, 3, 4, 5, 6, 7));
        franja.put("horaInicio", "17:00");
        franja.put("horaFin", "23:00");
        franja.put("precioHora", 100_000);
        esperarEstado(postear("/api/v1/admin/canchas/" + canchaId + "/tarifas", admin, franja), 201);

        MvcResult r = solicitar(cliente.token(), canchaId, fecha, "19:00", "21:00");
        esperarEstado(r, 201);
        assertThat((Integer) leer(r, "$.precioFinal")).isEqualTo(200_000);

        // el precio queda congelado aunque luego cambie la tarifa
        long tarifaId = ((Number) leer(obtener("/api/v1/publico/canchas/" + canchaId, null), "$.tarifas[0].id")).longValue();
        franja.put("precioHora", 150_000);
        esperarEstado(mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/admin/tarifas/" + tarifaId)
                .header("Authorization", "Bearer " + admin).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(franja))).andReturn(), 200);
        assertThat((Integer) leer(obtener("/api/v1/reservas/" + id(r), cliente.token()), "$.precioFinal")).isEqualTo(200_000);
    }

    @Test
    void rechazaHorariosFueraDeLasReglasDeLaCancha() throws Exception {
        // fuera del horario de operación (abre a las 08:00)
        MvcResult temprano = solicitar(cliente.token(), canchaId, fecha, "06:00", "07:00");
        esperarEstado(temprano, 422);
        assertThat((String) leer(temprano, "$.codigo")).isEqualTo("FUERA_DE_HORARIO");
        // no alineado al bloque de 60 min
        esperarEstado(solicitar(cliente.token(), canchaId, fecha, "19:30", "20:30"), 400);
        // duración mayor al máximo (2 h)
        esperarEstado(solicitar(cliente.token(), canchaId, fecha, "17:00", "20:00"), 400);
        // fin antes del inicio
        esperarEstado(solicitar(cliente.token(), canchaId, fecha, "20:00", "19:00"), 400);
        // en el pasado y demasiado lejos
        MvcResult pasado = solicitar(cliente.token(), canchaId, enDias(-1), "19:00", "20:00");
        esperarEstado(pasado, 422);
        assertThat((String) leer(pasado, "$.codigo")).isEqualTo("FUERA_DE_VENTANA");
        esperarEstado(solicitar(cliente.token(), canchaId, enDias(40), "19:00", "20:00"), 422);
        // ninguna dejó rastro
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reservas WHERE cliente_id = ?", Integer.class, cliente.id())).isZero();
    }

    @Test
    void noSeReservaEnCanchasInexistentesInactivasNiEnSedesDesactivadas() throws Exception {
        esperarEstado(solicitar(cliente.token(), 987654321L, fecha, "19:00", "20:00"), 404);

        long mantenimiento = crearCancha(admin, sedeId, "Cancha M");
        esperarEstado(postear("/api/v1/admin/canchas/" + mantenimiento + "/estado", admin, Map.of("estado", "MANTENIMIENTO", "motivo", "Cambio de grama")), 200);
        esperarEstado(solicitar(cliente.token(), mantenimiento, fecha, "19:00", "20:00"), 404);

        esperarEstado(postear("/api/v1/admin/sedes/" + sedeId + "/estado", admin, Map.of("activa", false, "motivo", "Cierre temporal")), 200);
        esperarEstado(solicitar(cliente.token(), canchaId, fecha, "19:00", "20:00"), 404);
    }

    @Test
    void sinSesionNoSePuedeReservar() throws Exception {
        esperarEstado(solicitar(null, canchaId, fecha, "19:00", "20:00"), 401);
        esperarEstado(obtener("/api/v1/me/reservas", null), 401);
    }

    @Test
    void unClienteTieneUnMaximoDeReservasActivas() throws Exception {
        for (int i = 1; i <= 5; i++) {
            esperarEstado(solicitar(cliente.token(), canchaId, enDias(i + 1), "19:00", "20:00"), 201);
        }
        MvcResult sexta = solicitar(cliente.token(), canchaId, enDias(8), "19:00", "20:00");
        esperarEstado(sexta, 422);
        assertThat((String) leer(sexta, "$.codigo")).isEqualTo("LIMITE_DE_RESERVAS");
        // otro cliente no se ve afectado
        esperarEstado(solicitar(nuevoClienteConSesion().token(), canchaId, enDias(8), "19:00", "20:00"), 201);
    }

    // ----- Anti-solapamiento -----

    @Test
    void dosReservasDelMismoHorarioSeRechazanYLasQueSeSolapanParcialmenteTambien() throws Exception {
        long primera = solicitarOk(cliente.token(), canchaId, fecha, "19:00", "21:00");
        Sesion otro = nuevoClienteConSesion();

        MvcResult igual = solicitar(otro.token(), canchaId, fecha, "19:00", "20:00");
        esperarEstado(igual, 409);
        assertThat((String) leer(igual, "$.codigo")).isEqualTo("RESERVA_SOLAPADA");
        esperarEstado(solicitar(otro.token(), canchaId, fecha, "20:00", "21:00"), 409);
        // pegadas por los lados sí se pueden
        esperarEstado(solicitar(otro.token(), canchaId, fecha, "18:00", "19:00"), 201);
        esperarEstado(solicitar(otro.token(), canchaId, fecha, "21:00", "22:00"), 201);

        // la rechazada no dejó filas a medias
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reservas WHERE cancha_id = ?", Integer.class, canchaId)).isEqualTo(3);
        assertThat(ocupaciones(primera)).isEqualTo(1);
    }

    @Test
    void cuandoOchoClientesPidenElMismoHorarioALaVezSoloUnoLoConsigue() throws Exception {
        int clientes = 8;
        List<Sesion> sesiones = new ArrayList<>();
        for (int i = 0; i < clientes; i++) {
            sesiones.add(nuevoClienteConSesion());
        }
        ExecutorService pool = Executors.newFixedThreadPool(clientes);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<MvcResult>> resultados = new ArrayList<>();
        for (Sesion s : sesiones) {
            resultados.add(pool.submit(() -> {
                salida.await();
                return solicitar(s.token(), canchaId, fecha, "20:00", "21:00");
            }));
        }
        salida.countDown();
        int creadas = 0;
        int solapadas = 0;
        for (Future<MvcResult> f : resultados) {
            MvcResult r = f.get();
            if (r.getResponse().getStatus() == 201) {
                creadas++;
            } else {
                assertThat(r.getResponse().getStatus()).isEqualTo(409);
                assertThat((String) leer(r, "$.codigo")).isEqualTo("RESERVA_SOLAPADA");
                solapadas++;
            }
        }
        pool.shutdown();

        assertThat(creadas).as("solo una reserva gana el horario").isEqualTo(1);
        assertThat(solapadas).isEqualTo(clientes - 1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reservas WHERE cancha_id = ?", Integer.class, canchaId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ocupaciones_cancha WHERE cancha_id = ?", Integer.class, canchaId)).isEqualTo(1);
    }

    @Test
    void laBaseDeDatosMismaRechazaOcupacionesSolapadas() throws Exception {
        long reservaId = solicitarOk(cliente.token(), canchaId, fecha, "19:00", "20:00");
        // Inserción directa que se salta toda la lógica de la aplicación: la restricción EXCLUDE debe frenarla.
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO ocupaciones_cancha (cancha_id, rango, tipo, reserva_id) VALUES (?, tstzrange(?::timestamptz, ?::timestamptz, '[)'), 'RESERVA', ?)",
                canchaId, a(fecha, "19:30").toString(), a(fecha, "20:30").toString(), reservaId))
                .hasMessageContaining("ex_ocupaciones_sin_solape");
    }

    // ----- Idempotencia -----

    @Test
    void repetirLaPeticionConLaMismaClaveDevuelveLaMismaReserva() throws Exception {
        String clave = UUID.randomUUID().toString();
        MvcResult primera = solicitarConClave(cliente.token(), clave, canchaId, fecha, "19:00", "20:00");
        MvcResult segunda = solicitarConClave(cliente.token(), clave, canchaId, fecha, "19:00", "20:00");

        esperarEstado(primera, 201);
        esperarEstado(segunda, 200);
        assertThat(id(segunda)).isEqualTo(id(primera));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reservas WHERE cliente_id = ?", Integer.class, cliente.id())).isEqualTo(1);
        // la misma clave de otro cliente es independiente
        Sesion otro = nuevoClienteConSesion();
        esperarEstado(solicitarConClave(otro.token(), clave, canchaId, fecha, "21:00", "22:00"), 201);
    }

    @Test
    void elDobleClicSimultaneoConLaMismaClaveCreaUnaSolaReserva() throws Exception {
        String clave = UUID.randomUUID().toString();
        int intentos = 6;
        ExecutorService pool = Executors.newFixedThreadPool(intentos);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<MvcResult>> resultados = new ArrayList<>();
        for (int i = 0; i < intentos; i++) {
            resultados.add(pool.submit(() -> {
                salida.await();
                return solicitarConClave(cliente.token(), clave, canchaId, fecha, "19:00", "20:00");
            }));
        }
        salida.countDown();
        int creadas = 0;
        int repetidas = 0;
        long idReserva = -1;
        for (Future<MvcResult> f : resultados) {
            MvcResult r = f.get();
            if (r.getResponse().getStatus() == 201) {
                creadas++;
            } else {
                assertThat(r.getResponse().getStatus()).isEqualTo(200);
                repetidas++;
            }
            long actual = id(r);
            idReserva = idReserva == -1 ? actual : idReserva;
            assertThat(actual).isEqualTo(idReserva);
        }
        pool.shutdown();
        assertThat(creadas).isEqualTo(1);
        assertThat(repetidas).isEqualTo(intentos - 1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reservas WHERE cliente_id = ?", Integer.class, cliente.id())).isEqualTo(1);
    }
}
