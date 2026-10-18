package co.edu.usco.goltime.sedes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

import co.edu.usco.goltime.BaseIntegracion;

class SedesCanchasIT extends BaseIntegracion {

    private String admin;

    @BeforeEach
    void iniciarSesionAdmin() throws Exception {
        admin = ingresarAdmin().token();
    }

    // ----- Ayudantes -----

    private MvcResult post(String url, Object cuerpo) throws Exception {
        return enviarConToken(url, cuerpo, admin);
    }

    private MvcResult modificar(String url, Object cuerpo) throws Exception {
        return mvc.perform(put(url).header("Authorization", "Bearer " + admin).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(cuerpo))).andReturn();
    }

    private MvcResult borrar(String url) throws Exception {
        return mvc.perform(delete(url).header("Authorization", "Bearer " + admin)).andReturn();
    }

    private static String nombre(String base) {
        return base + " " + UUID.randomUUID().toString().substring(0, 6);
    }

    private static List<Object> lista(MvcResult r, String ruta) throws Exception {
        return leer(r, ruta);
    }

    private static <T> T leer(MvcResult r, String ruta) throws Exception {
        return JsonPath.read(r.getResponse().getContentAsString(), ruta);
    }

    private Map<String, Object> datosSede(String nombre) {
        Map<String, Object> m = new HashMap<>();
        m.put("nombre", nombre);
        m.put("direccion", "Calle 10 # 5-20");
        m.put("ciudad", "Neiva");
        m.put("telefono", "8712345");
        m.put("descripcion", "Sede de prueba");
        m.put("servicios", List.of("Parqueadero", "Camerinos", " Parqueadero "));
        return m;
    }

    private long crearSede() throws Exception {
        MvcResult r = post("/api/v1/admin/sedes", datosSede(nombre("Sede")));
        esperarEstado(r, 201);
        return ((Number) leer(r, "$.id")).longValue();
    }

    private Map<String, Object> datosCancha(String nombre, int jugadores, int precio) {
        Map<String, Object> m = new HashMap<>();
        m.put("nombre", nombre);
        m.put("jugadoresPorEquipo", jugadores);
        m.put("precioBaseHora", precio);
        return m;
    }

    private long crearCancha(long sedeId, String nombre, int jugadores, int precio) throws Exception {
        MvcResult r = post("/api/v1/admin/sedes/" + sedeId + "/canchas", datosCancha(nombre, jugadores, precio));
        esperarEstado(r, 201);
        return ((Number) leer(r, "$.id")).longValue();
    }

    private Map<String, Object> tarifa(List<Integer> dias, String desde, String hasta, int precio, boolean festivo) {
        Map<String, Object> m = new HashMap<>();
        m.put("diasSemana", dias);
        m.put("horaInicio", desde);
        m.put("horaFin", hasta);
        m.put("aplicaFestivo", festivo);
        m.put("precioHora", precio);
        return m;
    }

    // ----- Sedes -----

    @Test
    void creaUnaSedeConHorarioPorDefectoServiciosLimpiosYAuditoria() throws Exception {
        String nombreSede = nombre("Sede Central");
        MvcResult r = post("/api/v1/admin/sedes", datosSede(nombreSede));

        esperarEstado(r, 201);
        assertThat((String) leer(r, "$.nombre")).isEqualTo(nombreSede);
        assertThat((Boolean) leer(r, "$.activa")).isTrue();
        assertThat(lista(r, "$.servicios")).containsExactly("Parqueadero", "Camerinos");
        assertThat(lista(r, "$.horario")).hasSize(7);
        assertThat((String) leer(r, "$.horario[5].cierre")).isEqualTo("23:00");
        assertThat((String) leer(r, "$.horario[6].cierre")).isEqualTo("20:00");
        Integer auditorias = jdbc.queryForObject("SELECT count(*) FROM audit_log WHERE accion='CREAR_SEDE' AND entidad_id = ?",
                Integer.class, ((Number) leer(r, "$.id")).longValue());
        assertThat(auditorias).isEqualTo(1);
    }

    @Test
    void rechazaSedesRepetidasOInvalidas() throws Exception {
        String nombreSede = nombre("Sede Unica");
        esperarEstado(post("/api/v1/admin/sedes", datosSede(nombreSede)), 201);
        esperarEstado(post("/api/v1/admin/sedes", datosSede(nombreSede.toUpperCase())), 400);

        Map<String, Object> sinNombre = datosSede("x");
        sinNombre.put("nombre", " ");
        esperarEstado(post("/api/v1/admin/sedes", sinNombre), 400);

        Map<String, Object> latitudMala = datosSede(nombre("Sede"));
        latitudMala.put("latitud", 120);
        esperarEstado(post("/api/v1/admin/sedes", latitudMala), 400);
    }

    @Test
    void soloElAdminPuedeGestionarSedes() throws Exception {
        Sesion cliente = nuevoClienteConSesion();
        esperarEstado(enviarConToken("/api/v1/admin/sedes", datosSede(nombre("Sede")), cliente.token()), 403);
        esperarEstado(consultar("/api/v1/admin/sedes", cliente.token()), 403);
        esperarEstado(consultar("/api/v1/admin/sedes", null), 401);
    }

    @Test
    void editaElHorarioDeLaSedeYValidaLosRangos() throws Exception {
        long sedeId = crearSede();
        List<Map<String, Object>> dias = List.of(
                Map.of("diaSemana", 1, "cerrado", false, "apertura", "10:00", "cierre", "24:00"),
                Map.of("diaSemana", 2, "cerrado", true));
        MvcResult r = modificar("/api/v1/admin/sedes/" + sedeId + "/horario", Map.of("dias", dias));
        esperarEstado(r, 200);
        assertThat((String) leer(r, "$.horario[0].cierre")).isEqualTo("24:00");
        assertThat((Boolean) leer(r, "$.horario[1].cerrado")).isTrue();

        List<Map<String, Object>> invertido = List.of(Map.of("diaSemana", 1, "cerrado", false, "apertura", "20:00", "cierre", "08:00"));
        esperarEstado(modificar("/api/v1/admin/sedes/" + sedeId + "/horario", Map.of("dias", invertido)), 400);
        List<Map<String, Object>> repetido = List.of(Map.of("diaSemana", 1, "cerrado", true), Map.of("diaSemana", 1, "cerrado", true));
        esperarEstado(modificar("/api/v1/admin/sedes/" + sedeId + "/horario", Map.of("dias", repetido)), 400);
    }

    @Test
    void desactivarUnaSedeExigeMotivoYLaOcultaDelCatalogoPublico() throws Exception {
        long sedeId = crearSede();
        crearCancha(sedeId, "Cancha A", 5, 80_000);
        esperarEstado(consultar("/api/v1/publico/sedes/" + sedeId, null), 200);

        esperarEstado(post("/api/v1/admin/sedes/" + sedeId + "/estado", Map.of("activa", false)), 400);
        MvcResult previa = post("/api/v1/admin/sedes/" + sedeId + "/estado/previsualizar", Map.of("activa", false));
        assertThat((Integer) leer(previa, "$.reservasFuturasActivas")).isZero();

        esperarEstado(post("/api/v1/admin/sedes/" + sedeId + "/estado", Map.of("activa", false, "motivo", "Cierre por remodelación")), 200);
        esperarEstado(consultar("/api/v1/publico/sedes/" + sedeId, null), 404);
        MvcResult publicas = consultar("/api/v1/publico/sedes", null);
        assertThat(lista(publicas, "$[?(@.id==" + sedeId + ")]")).isEmpty();
        // el admin sí la sigue viendo
        MvcResult admin = consultar("/api/v1/admin/sedes/" + sedeId, this.admin);
        assertThat((Boolean) leer(admin, "$.activa")).isFalse();

        esperarEstado(post("/api/v1/admin/sedes/" + sedeId + "/estado", Map.of("activa", true)), 200);
        esperarEstado(consultar("/api/v1/publico/sedes/" + sedeId, null), 200);
    }

    @Test
    void elCatalogoPublicoMuestraSoloCanchasReservables() throws Exception {
        long sedeId = crearSede();
        long activa = crearCancha(sedeId, "Activa", 5, 80_000);
        long mantenimiento = crearCancha(sedeId, "En mantenimiento", 7, 120_000);
        esperarEstado(post("/api/v1/admin/canchas/" + mantenimiento + "/estado", Map.of("estado", "MANTENIMIENTO", "motivo", "Cambio de grama")), 200);

        MvcResult sede = consultar("/api/v1/publico/sedes/" + sedeId, null);
        assertThat(lista(sede, "$.canchas")).hasSize(1);
        assertThat(((Number) leer(sede, "$.canchas[0].id")).longValue()).isEqualTo(activa);
        esperarEstado(consultar("/api/v1/publico/canchas/" + mantenimiento, null), 404);

        MvcResult filtradas = consultar("/api/v1/publico/canchas?sedeId=" + sedeId + "&modalidad=f5", null);
        assertThat(lista(filtradas, "$")).hasSize(1);
        MvcResult sinResultados = consultar("/api/v1/publico/canchas?sedeId=" + sedeId + "&modalidad=F7", null);
        assertThat(lista(sinResultados, "$")).isEmpty();
    }

    // ----- Canchas -----

    @Test
    void creaUnaCanchaConValoresPorDefectoYValidaLaModalidad() throws Exception {
        long sedeId = crearSede();
        MvcResult r = post("/api/v1/admin/sedes/" + sedeId + "/canchas", datosCancha(nombre("Cancha"), 5, 80_000));
        esperarEstado(r, 201);
        assertThat((String) leer(r, "$.modalidad")).isEqualTo("F5");
        assertThat((Integer) leer(r, "$.capacidadTotal")).isEqualTo(12);
        assertThat((Integer) leer(r, "$.duracionBloqueMin")).isEqualTo(60);
        assertThat((Integer) leer(r, "$.duracionMaxMin")).isEqualTo(120);
        assertThat((String) leer(r, "$.superficie")).isEqualTo("Sintética");
        assertThat((Boolean) leer(r, "$.iluminacion")).isTrue();
        assertThat((String) leer(r, "$.estado")).isEqualTo("ACTIVA");

        MvcResult noPermitida = post("/api/v1/admin/sedes/" + sedeId + "/canchas", datosCancha(nombre("Cancha"), 4, 80_000));
        esperarEstado(noPermitida, 400);
        assertThat((String) leer(noPermitida, "$.detail")).contains("F4");
    }

    @Test
    void validaDuracionesPreciosYNombresRepetidos() throws Exception {
        long sedeId = crearSede();
        crearCancha(sedeId, "Repetida", 5, 80_000);
        esperarEstado(post("/api/v1/admin/sedes/" + sedeId + "/canchas", datosCancha("REPETIDA", 5, 80_000)), 400);

        Map<String, Object> mal = datosCancha(nombre("C"), 5, 80_000);
        mal.put("duracionBloqueMin", 60);
        mal.put("duracionMinMin", 90);
        esperarEstado(post("/api/v1/admin/sedes/" + sedeId + "/canchas", mal), 400);

        esperarEstado(post("/api/v1/admin/sedes/" + sedeId + "/canchas", datosCancha(nombre("C"), 5, -1)), 400);
        esperarEstado(post("/api/v1/admin/sedes/99999999/canchas", datosCancha(nombre("C"), 5, 1000)), 404);
    }

    @Test
    void canchasCompuestasRespetanSusReglas() throws Exception {
        long sedeId = crearSede();
        long a = crearCancha(sedeId, "F5 A", 5, 80_000);
        long b = crearCancha(sedeId, "F5 B", 5, 80_000);
        Map<String, Object> datosF8 = datosCancha("F8 Techada", 8, 160_000);
        datosF8.put("componentes", List.of(a, b));
        MvcResult f8 = post("/api/v1/admin/sedes/" + sedeId + "/canchas", datosF8);
        esperarEstado(f8, 201);
        long f8Id = ((Number) leer(f8, "$.id")).longValue();
        assertThat((Boolean) leer(f8, "$.esCompuesta")).isTrue();
        assertThat(lista(f8, "$.componentes")).hasSize(2);

        // una componente no puede volverse compuesta
        Map<String, Object> aDependiente = datosCancha("F5 A", 5, 80_000);
        aDependiente.put("componentes", List.of(b));
        esperarEstado(modificar("/api/v1/admin/canchas/" + a, aDependiente), 400);

        // una compuesta no puede ser componente de otra
        Map<String, Object> otraCompuesta = datosCancha("F11", 11, 300_000);
        otraCompuesta.put("componentes", List.of(f8Id));
        esperarEstado(post("/api/v1/admin/sedes/" + sedeId + "/canchas", otraCompuesta), 400);

        // no puede depender de sí misma
        Map<String, Object> consigoMisma = datosCancha("F8 Techada", 8, 160_000);
        consigoMisma.put("componentes", List.of(f8Id));
        esperarEstado(modificar("/api/v1/admin/canchas/" + f8Id, consigoMisma), 400);

        // las componentes deben existir y ser de la misma sede
        long otraSede = crearSede();
        long ajena = crearCancha(otraSede, "Ajena", 5, 80_000);
        Map<String, Object> deOtraSede = datosCancha("F8 Mixta", 8, 160_000);
        deOtraSede.put("componentes", List.of(a, ajena));
        esperarEstado(post("/api/v1/admin/sedes/" + sedeId + "/canchas", deOtraSede), 400);
        Map<String, Object> inexistente = datosCancha("F8 Fantasma", 8, 160_000);
        inexistente.put("componentes", List.of(987654321L));
        esperarEstado(post("/api/v1/admin/sedes/" + sedeId + "/canchas", inexistente), 400);

        // se puede quitar la dependencia
        MvcResult sinDependencias = modificar("/api/v1/admin/canchas/" + f8Id, datosCancha("F8 Techada", 8, 160_000));
        esperarEstado(sinDependencias, 200);
        assertThat((Boolean) leer(sinDependencias, "$.esCompuesta")).isFalse();
    }

    @Test
    void elHorarioPropioDeLaCanchaReemplazaAlDeLaSedeYSePuedeQuitar() throws Exception {
        long sedeId = crearSede();
        long canchaId = crearCancha(sedeId, "Con horario", 5, 80_000);

        MvcResult inicial = consultar("/api/v1/publico/canchas/" + canchaId, null);
        assertThat((String) leer(inicial, "$.horarioEfectivo[0].cierre")).isEqualTo("23:00");
        assertThat(lista(inicial, "$.horarioPropio")).isEmpty();

        List<Map<String, Object>> propio = List.of(Map.of("diaSemana", 1, "cerrado", false, "apertura", "14:00", "cierre", "22:00"));
        esperarEstado(modificar("/api/v1/admin/canchas/" + canchaId + "/horario", Map.of("dias", propio)), 200);
        MvcResult conPropio = consultar("/api/v1/publico/canchas/" + canchaId, null);
        assertThat((String) leer(conPropio, "$.horarioEfectivo[0].apertura")).isEqualTo("14:00");
        assertThat((String) leer(conPropio, "$.horarioEfectivo[1].apertura")).as("otros días usan el de la sede").isEqualTo("08:00");

        esperarEstado(borrar("/api/v1/admin/canchas/" + canchaId + "/horario"), 200);
        MvcResult sinPropio = consultar("/api/v1/publico/canchas/" + canchaId, null);
        assertThat((String) leer(sinPropio, "$.horarioEfectivo[0].apertura")).isEqualTo("08:00");
    }

    @Test
    void sacarUnaCanchaDeServicioExigeMotivoYAvisaDeConflictos() throws Exception {
        long sedeId = crearSede();
        long canchaId = crearCancha(sedeId, "Mantenimiento", 5, 80_000);
        String base = "/api/v1/admin/canchas/" + canchaId;

        MvcResult previa = post(base + "/estado/previsualizar", Map.of("estado", "INACTIVA"));
        assertThat((Boolean) leer(previa, "$.requiereConfirmacion")).isFalse();
        esperarEstado(post(base + "/estado", Map.of("estado", "INACTIVA")), 400);

        MvcResult r = post(base + "/estado", Map.of("estado", "INACTIVA", "motivo", "Se retira la cancha"));
        esperarEstado(r, 200);
        assertThat((String) leer(r, "$.estado")).isEqualTo("INACTIVA");
        esperarEstado(post(base + "/estado", Map.of("estado", "ACTIVA")), 200);
        Integer auditorias = jdbc.queryForObject(
                "SELECT count(*) FROM audit_log WHERE accion='CAMBIAR_ESTADO_CANCHA' AND entidad_id = ?", Integer.class, canchaId);
        assertThat(auditorias).isEqualTo(2);
    }

    // ----- Tarifas y cotización -----

    @Test
    void lasTarifasSeGuardanSeValidanYSeAplicanEnLaCotizacion() throws Exception {
        long sedeId = crearSede();
        long canchaId = crearCancha(sedeId, "Con tarifas", 5, 80_000);
        String url = "/api/v1/admin/canchas/" + canchaId + "/tarifas";

        MvcResult manana = post(url, tarifa(List.of(1, 2, 3, 4, 5), "06:00", "17:00", 70_000, false));
        esperarEstado(manana, 201);
        assertThat(lista(manana, "$.diasSemana")).containsExactly(1, 2, 3, 4, 5);
        esperarEstado(post(url, tarifa(List.of(1, 2, 3, 4, 5), "17:00", "23:00", 100_000, false)), 201);
        esperarEstado(post(url, tarifa(List.of(), "06:00", "23:00", 130_000, true)), 201);

        // solapada con la de la mañana
        esperarEstado(post(url, tarifa(List.of(2), "10:00", "12:00", 60_000, false)), 400);
        // sin días y sin ser de festivo
        esperarEstado(post(url, tarifa(List.of(), "06:00", "07:00", 60_000, false)), 400);
        // horas invertidas o inválidas
        esperarEstado(post(url, tarifa(List.of(6), "12:00", "10:00", 60_000, false)), 400);
        esperarEstado(post(url, tarifa(List.of(6), "25:00", "26:00", 60_000, false)), 400);

        // martes 17/nov/2026 19:00-20:00 hora Bogotá = 2026-11-18T00:00Z a 01:00Z -> 100.000
        MvcResult martes = consultar("/api/v1/publico/canchas/" + canchaId + "/cotizacion?inicio=2026-11-18T00:00:00Z&fin=2026-11-18T01:00:00Z", null);
        esperarEstado(martes, 200);
        assertThat(((Number) leer(martes, "$.total")).longValue()).isEqualTo(100_000);
        assertThat((String) leer(martes, "$.tramos[0].origen")).isEqualTo("TARIFA");

        // lunes 16/nov/2026 es festivo (Independencia de Cartagena): 19:00-20:00 Bogotá -> 130.000
        MvcResult festivo = consultar("/api/v1/publico/canchas/" + canchaId + "/cotizacion?inicio=2026-11-17T00:00:00Z&fin=2026-11-17T01:00:00Z", null);
        assertThat(((Number) leer(festivo, "$.total")).longValue()).isEqualTo(130_000);
        assertThat((String) leer(festivo, "$.tramos[0].origen")).isEqualTo("FESTIVO");

        // 16:00-18:00 Bogotá un martes cruza dos franjas: 70.000 + 100.000
        MvcResult cruce = consultar("/api/v1/publico/canchas/" + canchaId + "/cotizacion?inicio=2026-11-17T21:00:00Z&fin=2026-11-17T23:00:00Z", null);
        assertThat(((Number) leer(cruce, "$.total")).longValue()).isEqualTo(170_000);
        assertThat(lista(cruce, "$.tramos")).hasSize(2);
    }

    @Test
    void laCotizacionValidaLaDuracionYLaDisponibilidadDeLaCancha() throws Exception {
        long sedeId = crearSede();
        long canchaId = crearCancha(sedeId, "Cotizable", 5, 80_000);
        String base = "/api/v1/publico/canchas/" + canchaId + "/cotizacion";

        esperarEstado(consultar(base + "?inicio=2026-11-18T00:00:00Z&fin=2026-11-18T00:45:00Z", null), 400);
        esperarEstado(consultar(base + "?inicio=2026-11-18T00:00:00Z&fin=2026-11-18T04:00:00Z", null), 400);
        esperarEstado(consultar(base + "?inicio=2026-11-18T02:00:00Z&fin=2026-11-18T01:00:00Z", null), 400);
        assertThat(((Number) leer(consultar(base + "?inicio=2026-11-18T00:00:00Z&fin=2026-11-18T02:00:00Z", null), "$.total")).longValue())
                .isEqualTo(160_000);

        post("/api/v1/admin/canchas/" + canchaId + "/estado", Map.of("estado", "INACTIVA", "motivo", "Fuera de servicio"));
        esperarEstado(consultar(base + "?inicio=2026-11-18T00:00:00Z&fin=2026-11-18T01:00:00Z", null), 404);
    }

    @Test
    void editarYEliminarTarifas() throws Exception {
        long canchaId = crearCancha(crearSede(), "Editable", 5, 80_000);
        MvcResult creada = post("/api/v1/admin/canchas/" + canchaId + "/tarifas", tarifa(List.of(6, 7), "08:00", "20:00", 110_000, false));
        long tarifaId = ((Number) leer(creada, "$.id")).longValue();

        MvcResult editada = modificar("/api/v1/admin/tarifas/" + tarifaId, tarifa(List.of(6, 7), "08:00", "20:00", 120_000, false));
        esperarEstado(editada, 200);
        assertThat((Integer) leer(editada, "$.precioHora")).isEqualTo(120_000);

        esperarEstado(borrar("/api/v1/admin/tarifas/" + tarifaId), 204);
        esperarEstado(borrar("/api/v1/admin/tarifas/" + tarifaId), 404);
        MvcResult cancha = consultar("/api/v1/publico/canchas/" + canchaId, null);
        assertThat(lista(cancha, "$.tarifas")).isEmpty();
    }

    // ----- Festivos -----

    @Test
    void losFestivosDeColombia2026y2027EstanPrecargadosYSonAdministrables() throws Exception {
        MvcResult f2026 = consultar("/api/v1/publico/festivos?anio=2026", null);
        assertThat(lista(f2026, "$")).hasSize(18);
        assertThat(lista(f2026, "$[?(@.fecha=='2026-12-25')].nombre")).containsExactly("Navidad");
        assertThat(lista(consultar("/api/v1/publico/festivos?anio=2027", null), "$")).hasSize(18);

        esperarEstado(post("/api/v1/admin/festivos", Map.of("fecha", "2026-12-25", "nombre", "Duplicado")), 400);

        MvcResult nuevo = post("/api/v1/admin/festivos", Map.of("fecha", "2028-01-01", "nombre", "Año Nuevo 2028"));
        esperarEstado(nuevo, 201);
        long id = ((Number) leer(nuevo, "$.id")).longValue();
        esperarEstado(borrar("/api/v1/admin/festivos/" + id), 204);
        assertThat(lista(consultar("/api/v1/publico/festivos?anio=2028", null), "$")).isEmpty();

        Sesion cliente = nuevoClienteConSesion();
        esperarEstado(enviarConToken("/api/v1/admin/festivos", Map.of("fecha", "2028-02-02", "nombre", "X"), cliente.token()), 403);
    }

    // ----- Fotos -----

    private static byte[] png() {
        byte[] b = new byte[64];
        byte[] firma = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        System.arraycopy(firma, 0, b, 0, firma.length);
        return b;
    }

    private MvcResult subir(String url, byte[] contenido, String nombreArchivo, String tipo) throws Exception {
        return mvc.perform(multipart(url).file(new MockMultipartFile("archivo", nombreArchivo, tipo, contenido))
                .header("Authorization", "Bearer " + admin)).andReturn();
    }

    @Test
    void subeSirveYBorraFotosValidandoLaFirmaDelArchivo() throws Exception {
        long sedeId = crearSede();
        String url = "/api/v1/admin/sedes/" + sedeId + "/fotos";

        MvcResult subida = subir(url, png(), "cancha.png", "image/png");
        esperarEstado(subida, 201);
        String ruta = leer(subida, "$.url");
        long fotoId = ((Number) leer(subida, "$.id")).longValue();
        assertThat(ruta).startsWith("/api/v1/publico/archivos/").endsWith(".png");

        MvcResult descarga = consultar(ruta, null);
        esperarEstado(descarga, 200);
        assertThat(descarga.getResponse().getContentType()).isEqualTo("image/png");
        assertThat(descarga.getResponse().getContentAsByteArray()).hasSize(64);
        assertThat(descarga.getResponse().getHeader("Cache-Control")).contains("immutable");

        MvcResult sede = consultar("/api/v1/publico/sedes/" + sedeId, null);
        assertThat(lista(sede, "$.fotos")).hasSize(1);

        esperarEstado(borrar(url + "/" + fotoId), 204);
        esperarEstado(consultar(ruta, null), 404);
        esperarEstado(borrar(url + "/" + fotoId), 404);
    }

    @Test
    void rechazaArchivosQueNoSonImagenesAunqueDigan_serlo() throws Exception {
        long canchaId = crearCancha(crearSede(), "Fotos", 5, 80_000);
        String url = "/api/v1/admin/canchas/" + canchaId + "/fotos";

        byte[] texto = "<?php echo 'hola'; ?> esto no es una imagen".getBytes();
        MvcResult falso = subir(url, texto, "foto.png", "image/png");
        esperarEstado(falso, 400);
        assertThat((String) leer(falso, "$.detail")).contains("JPG, PNG o WebP");

        esperarEstado(subir(url, new byte[0], "vacia.png", "image/png"), 400);
        byte[] enorme = new byte[5 * 1024 * 1024 + 1];
        System.arraycopy(png(), 0, enorme, 0, 8);
        esperarEstado(subir(url, enorme, "enorme.png", "image/png"), 400);
    }

    @Test
    void limitaLaCantidadDeFotos() throws Exception {
        long canchaId = crearCancha(crearSede(), "Muchas fotos", 5, 80_000);
        String url = "/api/v1/admin/canchas/" + canchaId + "/fotos";
        for (int i = 0; i < 10; i++) {
            esperarEstado(subir(url, png(), "f" + i + ".png", "image/png"), 201);
        }
        esperarEstado(subir(url, png(), "extra.png", "image/png"), 400);
    }

    @Test
    void noSePuedeLeerFueraDelDirectorioDeArchivos() throws Exception {
        // el firewall de Spring Security rechaza las barras codificadas (400) antes de llegar al controlador
        assertThat(consultar("/api/v1/publico/archivos/..%2F..%2Fetc%2Fpasswd", null).getResponse().getStatus()).isIn(400, 404);
        esperarEstado(consultar("/api/v1/publico/archivos/no-existe.png", null), 404);
        esperarEstado(consultar("/api/v1/publico/archivos/" + UUID.randomUUID() + ".png", null), 404);
        esperarEstado(consultar("/api/v1/publico/archivos/" + UUID.randomUUID() + ".exe", null), 404);
    }

    @Test
    void sinSesionNoSePuedeSubirFotos() throws Exception {
        long sedeId = crearSede();
        MvcResult r = mvc.perform(multipart("/api/v1/admin/sedes/" + sedeId + "/fotos")
                .file(new MockMultipartFile("archivo", "a.png", "image/png", png()))).andReturn();
        esperarEstado(r, 401);
        assertThat(Set.of(401)).contains(r.getResponse().getStatus());
        mvc.perform(get("/api/v1/publico/estado")).andReturn();
    }
}
