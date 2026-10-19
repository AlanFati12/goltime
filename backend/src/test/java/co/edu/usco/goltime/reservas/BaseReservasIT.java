package co.edu.usco.goltime.reservas;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

import co.edu.usco.goltime.BaseIntegracion;
import co.edu.usco.goltime.compartido.dominio.ZonaHoraria;

/** Ayudantes comunes de las pruebas de reservas: crear sedes y canchas, fechas futuras y llamadas a la API. */
public abstract class BaseReservasIT extends BaseIntegracion {

    protected static String nombre(String base) {
        return base + " " + UUID.randomUUID().toString().substring(0, 6);
    }

    /** Fecha a {@code dias} días de hoy en Bogotá (la antelación mínima es de 3 h y la máxima de 30 días). */
    protected static LocalDate enDias(int dias) {
        return ZonaHoraria.fechaLocal(Instant.now()).plusDays(dias);
    }

    /**
     * El día hábil número {@code dias} contando desde hoy (negativo hacia atrás), sin contar domingos. Los domingos
     * la sede cierra a las 20:00 (L-S cierra a las 23:00) y las pruebas reservan hasta las 23:00. Se cuentan días
     * hábiles en vez de mover el domingo al lunes porque así cada {@code dias} da una fecha distinta: con el
     * corrimiento, {@code diaHabil(n)} y {@code diaHabil(n + 1)} caían el mismo lunes según el día en que corrieran
     * las pruebas, y la segunda reserva chocaba con la primera.
     */
    protected static LocalDate diaHabil(int dias) {
        LocalDate fecha = enDias(0);
        int paso = dias < 0 ? -1 : 1;
        for (int contados = 0; contados < Math.abs(dias); ) {
            fecha = fecha.plusDays(paso);
            if (fecha.getDayOfWeek() != java.time.DayOfWeek.SUNDAY) {
                contados++;
            }
        }
        return fecha.getDayOfWeek() == java.time.DayOfWeek.SUNDAY ? fecha.plusDays(1) : fecha;
    }

    protected static Instant a(LocalDate fecha, String hora) {
        return ZonaHoraria.aInstante(fecha, LocalTime.parse(hora));
    }

    protected static <T> T leer(MvcResult r, String ruta) throws Exception {
        return JsonPath.read(r.getResponse().getContentAsString(), ruta);
    }

    protected static long id(MvcResult r) throws Exception {
        return ((Number) leer(r, "$.id")).longValue();
    }

    protected MvcResult enviarJson(MockHttpServletRequestBuilder peticion, String token, Object cuerpo) throws Exception {
        if (token != null) {
            peticion.header("Authorization", "Bearer " + token);
        }
        return mvc.perform(peticion.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(cuerpo))).andReturn();
    }

    protected MvcResult postear(String url, String token, Object cuerpo) throws Exception {
        return enviarJson(post(url), token, cuerpo);
    }

    protected MvcResult parchear(String url, String token, Object cuerpo) throws Exception {
        return enviarJson(patch(url), token, cuerpo);
    }

    protected MvcResult obtener(String url, String token) throws Exception {
        MockHttpServletRequestBuilder peticion = get(url);
        if (token != null) {
            peticion.header("Authorization", "Bearer " + token);
        }
        return mvc.perform(peticion).andReturn();
    }

    // ----- Catálogo -----

    protected long crearSede(String admin) throws Exception {
        Map<String, Object> m = new HashMap<>();
        m.put("nombre", nombre("Sede"));
        m.put("direccion", "Calle 1 # 2-3");
        m.put("ciudad", "Neiva");
        MvcResult r = postear("/api/v1/admin/sedes", admin, m);
        esperarEstado(r, 201);
        return id(r);
    }

    protected long crearCancha(String admin, long sedeId, String nombre, int jugadores, int precio, List<Long> componentes) throws Exception {
        Map<String, Object> m = new HashMap<>();
        m.put("nombre", nombre);
        m.put("jugadoresPorEquipo", jugadores);
        m.put("precioBaseHora", precio);
        if (componentes != null) {
            m.put("componentes", componentes);
        }
        MvcResult r = postear("/api/v1/admin/sedes/" + sedeId + "/canchas", admin, m);
        esperarEstado(r, 201);
        return id(r);
    }

    protected long crearCancha(String admin, long sedeId, String nombre) throws Exception {
        return crearCancha(admin, sedeId, nombre, 5, 80_000, null);
    }

    // ----- Reservas -----

    protected Map<String, Object> cuerpoReserva(long canchaId, Instant inicio, Instant fin) {
        Map<String, Object> m = new HashMap<>();
        m.put("canchaId", canchaId);
        m.put("inicio", inicio.toString());
        m.put("fin", fin.toString());
        m.put("nombreEquipo", "Los Cracks");
        return m;
    }

    /** Solicita una reserva de una hora y devuelve la respuesta cruda. */
    protected MvcResult solicitar(String token, long canchaId, LocalDate fecha, String horaInicio, String horaFin) throws Exception {
        return postear("/api/v1/reservas", token, cuerpoReserva(canchaId, a(fecha, horaInicio), a(fecha, horaFin)));
    }

    protected long solicitarOk(String token, long canchaId, LocalDate fecha, String horaInicio, String horaFin) throws Exception {
        MvcResult r = solicitar(token, canchaId, fecha, horaInicio, horaFin);
        esperarEstado(r, 201);
        return id(r);
    }

    protected void aprobar(String admin, long reservaId) throws Exception {
        esperarEstado(postear("/api/v1/admin/reservas/" + reservaId + "/aprobar", admin, Map.of()), 200);
    }

    protected String estadoDe(long reservaId) {
        return jdbc.queryForObject("SELECT estado FROM reservas WHERE id = ?", String.class, reservaId);
    }

    protected int ocupaciones(long reservaId) {
        return jdbc.queryForObject("SELECT count(*) FROM ocupaciones_cancha WHERE reserva_id = ?", Integer.class, reservaId);
    }

    protected List<String> acciones(long reservaId) {
        return jdbc.queryForList("SELECT accion FROM reserva_historial WHERE reserva_id = ? ORDER BY id", String.class, reservaId);
    }

    protected int notificaciones(long usuarioId, String tipo) {
        return jdbc.queryForObject("SELECT count(*) FROM notificaciones WHERE usuario_id = ? AND tipo = ?", Integer.class, usuarioId, tipo);
    }
}
