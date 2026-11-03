package co.edu.usco.goltime.reservas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

class FavoritosYCalificacionesIT extends BaseReservasIT {

    private String admin;
    private long canchaId;
    private Sesion cliente;

    @BeforeEach
    void preparar() throws Exception {
        admin = ingresarAdmin().token();
        canchaId = crearCancha(admin, crearSede(admin), "Cancha F");
        cliente = nuevoClienteConSesion();
    }

    private MvcResult poner(String url, String token, Object cuerpo) throws Exception {
        return enviarJson(put(url), token, cuerpo);
    }

    private MvcResult borrar(String url, String token) throws Exception {
        return mvc.perform(delete(url).header("Authorization", "Bearer " + token)).andReturn();
    }

    /** Reserva del cliente creada por el admin, movida al pasado y marcada como completada. */
    private long reservaCompletada(Sesion c, int haceDias, LocalDate futuro) throws Exception {
        Map<String, Object> m = cuerpoReserva(canchaId, a(futuro, "19:00"), a(futuro, "20:00"));
        m.put("clienteId", c.id());
        MvcResult r = postear("/api/v1/admin/reservas", admin, m);
        esperarEstado(r, 201);
        long id = id(r);
        Instant inicio = Instant.now().minus(Duration.ofDays(haceDias)).minus(Duration.ofHours(3));
        jdbc.update("UPDATE reservas SET inicio = ?, fin = ? WHERE id = ?", OffsetDateTime.ofInstant(inicio, ZoneOffset.UTC), OffsetDateTime.ofInstant(inicio.plus(Duration.ofHours(1)), ZoneOffset.UTC), id);
        jdbc.update("UPDATE ocupaciones_cancha SET rango = tstzrange(?, ?, '[)') WHERE reserva_id = ?", OffsetDateTime.ofInstant(inicio, ZoneOffset.UTC),
                OffsetDateTime.ofInstant(inicio.plus(Duration.ofHours(1)), ZoneOffset.UTC), id);
        esperarEstado(parchear("/api/v1/admin/reservas/" + id + "/estado", admin, Map.of("estado", "COMPLETADA", "motivo", "Partido jugado")), 200);
        return id;
    }

    // ----- Favoritos -----

    @Test
    void marcarYQuitarFavoritasEsIdempotenteYCadaClienteTieneLasSuyas() throws Exception {
        Sesion otro = nuevoClienteConSesion();

        esperarEstado(poner("/api/v1/me/favoritos/" + canchaId, cliente.token(), Map.of()), 204);
        esperarEstado(poner("/api/v1/me/favoritos/" + canchaId, cliente.token(), Map.of()), 204);

        MvcResult lista = obtener("/api/v1/me/favoritos", cliente.token());
        assertThat((List<?>) leer(lista, "$")).hasSize(1);
        assertThat((String) leer(lista, "$[0].cancha")).isEqualTo("Cancha F");
        assertThat((Boolean) leer(lista, "$[0].activa")).isTrue();
        assertThat((List<?>) leer(obtener("/api/v1/me/favoritos", otro.token()), "$")).isEmpty();

        esperarEstado(poner("/api/v1/me/favoritos/99999999", cliente.token(), Map.of()), 404);
        esperarEstado(borrar("/api/v1/me/favoritos/" + canchaId, cliente.token()), 204);
        esperarEstado(borrar("/api/v1/me/favoritos/" + canchaId, cliente.token()), 204);
        assertThat((List<?>) leer(obtener("/api/v1/me/favoritos", cliente.token()), "$")).isEmpty();
        esperarEstado(obtener("/api/v1/me/favoritos", null), 401);
    }

    // ----- Calificaciones -----

    @Test
    void soloSeCalificaUnaReservaPropiaYaJugadaYSePuedeCorregir() throws Exception {
        long pendiente = solicitarOk(cliente.token(), canchaId, diaHabil(7), "19:00", "20:00");
        MvcResult antes = obtener("/api/v1/reservas/" + pendiente + "/calificacion", cliente.token());
        assertThat((Boolean) leer(antes, "$.puedeCalificar")).isFalse();
        esperarEstado(poner("/api/v1/reservas/" + pendiente + "/calificacion", cliente.token(), Map.of("estrellas", 5)), 409);

        long jugada = reservaCompletada(cliente, 3, diaHabil(16));
        assertThat((Boolean) leer(obtener("/api/v1/reservas/" + jugada + "/calificacion", cliente.token()), "$.puedeCalificar")).isTrue();

        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("estrellas", 5);
        cuerpo.put("comentario", "Césped en excelente estado");
        MvcResult ok = poner("/api/v1/reservas/" + jugada + "/calificacion", cliente.token(), cuerpo);
        esperarEstado(ok, 200);
        assertThat((Integer) leer(ok, "$.estrellas")).isEqualTo(5);

        MvcResult publico = obtener("/api/v1/publico/canchas/" + canchaId + "/calificaciones", null);
        assertThat(((Number) leer(publico, "$.promedio")).doubleValue()).isEqualTo(5.0);
        assertThat((Integer) leer(publico, "$.total")).isEqualTo(1);
        assertThat((String) leer(publico, "$.ultimas[0].autor")).isEqualTo("Ana P.");
        assertThat(publico.getResponse().getContentAsString()).doesNotContain("correo").doesNotContain("clienteId");

        // corregir: sigue siendo una sola calificación
        esperarEstado(poner("/api/v1/reservas/" + jugada + "/calificacion", cliente.token(), Map.of("estrellas", 3)), 200);
        MvcResult corregida = obtener("/api/v1/publico/canchas/" + canchaId + "/calificaciones", null);
        assertThat((Integer) leer(corregida, "$.total")).isEqualTo(1);
        assertThat(((Number) leer(corregida, "$.promedio")).doubleValue()).isEqualTo(3.0);

        esperarEstado(poner("/api/v1/reservas/" + jugada + "/calificacion", cliente.token(), Map.of("estrellas", 6)), 400);
        esperarEstado(poner("/api/v1/reservas/" + jugada + "/calificacion", cliente.token(), Map.of("estrellas", 0)), 400);
        Sesion otro = nuevoClienteConSesion();
        esperarEstado(poner("/api/v1/reservas/" + jugada + "/calificacion", otro.token(), Map.of("estrellas", 1)), 404);
        esperarEstado(obtener("/api/v1/reservas/" + jugada + "/calificacion", otro.token()), 404);
        esperarEstado(poner("/api/v1/reservas/" + jugada + "/calificacion", null, Map.of("estrellas", 1)), 401);
    }

    @Test
    void elPromedioReunaVariosClientesYNoCuentaReservasQueDejaronDeEstarCompletadas() throws Exception {
        Sesion otro = nuevoClienteConSesion();
        long r1 = reservaCompletada(cliente, 3, diaHabil(16));
        long r2 = reservaCompletada(otro, 4, diaHabil(18));
        poner("/api/v1/reservas/" + r1 + "/calificacion", cliente.token(), Map.of("estrellas", 5));
        poner("/api/v1/reservas/" + r2 + "/calificacion", otro.token(), Map.of("estrellas", 2, "comentario", "Faltaba iluminación"));

        MvcResult r = obtener("/api/v1/publico/canchas/" + canchaId + "/calificaciones", null);

        assertThat(((Number) leer(r, "$.promedio")).doubleValue()).isEqualTo(3.5);
        assertThat((Integer) leer(r, "$.total")).isEqualTo(2);
        List<Integer> distribucion = leer(r, "$.distribucion");
        assertThat(distribucion).containsExactly(0, 1, 0, 0, 1);
        assertThat((List<?>) leer(r, "$.ultimas")).hasSize(1); // solo la que trae comentario

        // el admin corrige una a "no asistió": su calificación deja de contar
        esperarEstado(parchear("/api/v1/admin/reservas/" + r1 + "/estado", admin, Map.of("estado", "NO_ASISTIO", "motivo", "No se presentaron")), 200);
        MvcResult despues = obtener("/api/v1/publico/canchas/" + canchaId + "/calificaciones", null);
        assertThat((Integer) leer(despues, "$.total")).isEqualTo(1);
        assertThat(((Number) leer(despues, "$.promedio")).doubleValue()).isEqualTo(2.0);
        assertThat(((Number) leer(obtener("/api/v1/publico/canchas/99999999/calificaciones", null), "$.promedio")).doubleValue()).isZero();
    }
}
