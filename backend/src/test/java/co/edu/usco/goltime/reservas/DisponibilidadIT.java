package co.edu.usco.goltime.reservas;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

class DisponibilidadIT extends BaseReservasIT {

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
        fecha = diaHabil(5);
    }

    private MvcResult dia(LocalDate f) throws Exception {
        return obtener("/api/v1/publico/disponibilidad?sedeId=" + sedeId + "&fecha=" + f, null);
    }

    private String estadoSlot(MvcResult r, String hora) throws Exception {
        List<Object> estados = leer(r, "$.canchas[0].slots[?(@.inicio=='" + a(fecha, hora) + "')].estado");
        return (String) estados.get(0);
    }

    @Test
    void lasDisponibilidadesSonPublicasYMuestranTodosLosBloquesDelHorario() throws Exception {
        MvcResult r = dia(fecha);
        esperarEstado(r, 200);
        // el horario por defecto es 08:00-23:00 (L-S) o 08:00-20:00 (D), con bloques de 60 min
        List<Object> slots = leer(r, "$.canchas[0].slots");
        int esperados = fecha.getDayOfWeek().getValue() == 7 ? 12 : 15;
        assertThat(slots).hasSize(esperados);
        assertThat(estadoSlot(r, "19:00")).isEqualTo("DISPONIBLE");
        assertThat(((Number) leer(r, "$.canchas[0].slots[0].precio")).longValue()).isEqualTo(80_000);
        assertThat((Boolean) leer(r, "$.canchas[0].cerrado")).isFalse();
    }

    @Test
    void unaReservaOcupaSuBloqueYSeLiberaAlCancelarse() throws Exception {
        long reservaId = solicitarOk(cliente.token(), canchaId, fecha, "19:00", "20:00");
        assertThat(estadoSlot(dia(fecha), "19:00")).isEqualTo("OCUPADO");
        assertThat(estadoSlot(dia(fecha), "20:00")).isEqualTo("DISPONIBLE");
        assertThat(estadoSlot(dia(fecha), "18:00")).isEqualTo("DISPONIBLE");

        esperarEstado(postear("/api/v1/reservas/" + reservaId + "/cancelar", cliente.token(), Map.of()), 200);
        assertThat(estadoSlot(dia(fecha), "19:00")).isEqualTo("DISPONIBLE");
    }

    @Test
    void unaReservaDeDosHorasOcupaDosBloques() throws Exception {
        esperarEstado(solicitar(cliente.token(), canchaId, fecha, "19:00", "21:00"), 201);
        MvcResult r = dia(fecha);
        assertThat(estadoSlot(r, "19:00")).isEqualTo("OCUPADO");
        assertThat(estadoSlot(r, "20:00")).isEqualTo("OCUPADO");
        assertThat(estadoSlot(r, "21:00")).isEqualTo("DISPONIBLE");
    }

    @Test
    void losBloquesDeHoyDentroDeLaAntelacionMinimaNoSonReservables() throws Exception {
        LocalDate hoy = enDias(0);
        MvcResult r = obtener("/api/v1/publico/disponibilidad?sedeId=" + sedeId + "&fecha=" + hoy, null);
        List<Object> estados = leer(r, "$.canchas[0].slots[*].estado");
        // ningún bloque que empiece antes de ahora + 3 h puede figurar como DISPONIBLE
        long ahoraMasTres = System.currentTimeMillis() + 3 * 3600_000L;
        List<Object> inicios = leer(r, "$.canchas[0].slots[*].inicio");
        for (int i = 0; i < inicios.size(); i++) {
            if (java.time.Instant.parse((String) inicios.get(i)).toEpochMilli() < ahoraMasTres) {
                assertThat(estados.get(i)).isNotEqualTo("DISPONIBLE");
            }
        }
    }

    @Test
    void unaFechaMasAllaDeLosTreintaDiasNoEsReservable() throws Exception {
        LocalDate lejos = enDias(40);
        MvcResult r = dia(lejos);
        esperarEstado(r, 200);
        List<Object> estados = leer(r, "$.canchas[0].slots[*].estado");
        assertThat(estados).isNotEmpty().allMatch(e -> e.equals("NO_RESERVABLE"));
    }

    @Test
    void unDiaCerradoNoTieneBloques() throws Exception {
        List<Map<String, Object>> dias = List.of(Map.of("diaSemana", fecha.getDayOfWeek().getValue(), "cerrado", true));
        esperarEstado(modificar("/api/v1/admin/sedes/" + sedeId + "/horario", Map.of("dias", dias)), 200);
        MvcResult r = dia(fecha);
        assertThat((Boolean) leer(r, "$.canchas[0].cerrado")).isTrue();
        assertThat((List<?>) leer(r, "$.canchas[0].slots")).isEmpty();
    }

    @Test
    void unaCanchaCompuestaNoEstaLibreSiUnaComponenteEstaOcupadaYViceversa() throws Exception {
        long a = canchaId;
        long b = crearCancha(admin, sedeId, "Cancha B");
        long f8 = crearCancha(admin, sedeId, "Cancha F8", 8, 160_000, List.of(a, b));
        Sesion otro = nuevoClienteConSesion();

        // Se reserva la F5 A a las 19:00: la F8 queda ocupada a esa hora, la F5 B sigue libre.
        long reservaA = solicitarOk(cliente.token(), a, fecha, "19:00", "20:00");
        MvcResult r = dia(fecha);
        assertThat(estadoDeCancha(r, f8, "19:00")).isEqualTo("OCUPADO");
        assertThat(estadoDeCancha(r, b, "19:00")).isEqualTo("DISPONIBLE");
        // y la F8 no se puede reservar a esa hora
        MvcResult choque = solicitar(otro.token(), f8, fecha, "19:00", "20:00");
        esperarEstado(choque, 409);
        assertThat((String) leer(choque, "$.codigo")).isEqualTo("RESERVA_SOLAPADA");

        // La F5 B sí se puede reservar a esa hora (los hermanos no se bloquean entre sí).
        esperarEstado(solicitar(otro.token(), b, fecha, "19:00", "20:00"), 201);
        esperarEstado(postear("/api/v1/reservas/" + reservaA + "/cancelar", cliente.token(), Map.of()), 200);

        // Reservar la F8 a otra hora ocupa también A y B.
        Sesion tercero = nuevoClienteConSesion();
        long reservaF8 = solicitarOk(tercero.token(), f8, fecha, "21:00", "22:00");
        MvcResult tras = dia(fecha);
        assertThat(estadoDeCancha(tras, a, "21:00")).isEqualTo("OCUPADO");
        assertThat(estadoDeCancha(tras, b, "21:00")).isEqualTo("OCUPADO");
        assertThat(ocupaciones(reservaF8)).as("la F8 ocupa la cancha compuesta y sus dos componentes").isEqualTo(3);
        esperarEstado(solicitar(cliente.token(), a, fecha, "21:00", "22:00"), 409);
    }

    private String estadoDeCancha(MvcResult r, long canchaId, String hora) throws Exception {
        List<Object> estados = leer(r, "$.canchas[?(@.canchaId==" + canchaId + ")].slots[?(@.inicio=='" + a(fecha, hora) + "')].estado");
        return (String) estados.get(0);
    }

    @Test
    void elResumenSemanalCuentaLosHorariosLibresDeLosProximosSieteDias() throws Exception {
        MvcResult r = obtener("/api/v1/publico/disponibilidad/semana?sedeId=" + sedeId, null);
        esperarEstado(r, 200);
        assertThat((List<?>) leer(r, "$")).hasSize(7);
        // un día futuro lejano de la semana tiene todos sus horarios libres
        assertThat((Integer) leer(r, "$[6].horariosLibres")).isGreaterThanOrEqualTo(12);
        assertThat((String) leer(r, "$[0].fecha")).isEqualTo(enDias(0).toString());
    }

    @Test
    void laDisponibilidadSoloListaCanchasReservablesDeSedesActivas() throws Exception {
        long mantenimiento = crearCancha(admin, sedeId, "En mantenimiento");
        esperarEstado(postear("/api/v1/admin/canchas/" + mantenimiento + "/estado", admin, Map.of("estado", "MANTENIMIENTO", "motivo", "Cambio de grama")), 200);
        MvcResult r = dia(fecha);
        assertThat((List<?>) leer(r, "$.canchas")).hasSize(1);
    }

    private MvcResult modificar(String url, Object cuerpo) throws Exception {
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(url)
                .header("Authorization", "Bearer " + admin).contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(cuerpo))).andReturn();
    }
}
