package co.edu.usco.goltime.whatsapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import co.edu.usco.goltime.autenticacion.aplicacion.ServicioVerificacionTelefono;
import co.edu.usco.goltime.notificaciones.aplicacion.EnviadorCorreo;
import co.edu.usco.goltime.notificaciones.aplicacion.EnviadorCorreo.MensajeCorreo;
import co.edu.usco.goltime.torneos.BaseTorneosIT;
import co.edu.usco.goltime.whatsapp.aplicacion.EnviadorWhatsapp;

/** El bot de WhatsApp (ADR 0018): seguridad de la integración, identidad por número, reservas y consultas. */
class WhatsappIT extends BaseTorneosIT {

    private static final String CLAVE = "clave-de-integracion-solo-para-pruebas-0123456789";
    private static final String BASE = "/api/v1/integracion/whatsapp";

    @MockitoBean
    EnviadorCorreo correo;

    @MockitoBean
    EnviadorWhatsapp enviador;

    @Autowired
    ServicioVerificacionTelefono verificacion;

    @AfterEach
    void restaurarConfiguracion() {
        jdbc.update("UPDATE configuracion SET valor = 'true' WHERE clave IN ('whatsapp_bot_activo', 'whatsapp_reservas_sin_cuenta')");
    }

    private static String celular() {
        return "+573" + ThreadLocalRandom.current().nextLong(100_000_000L, 999_999_999L);
    }

    private MvcResult bot(MockHttpServletRequestBuilder peticion, String numero, Object cuerpo) throws Exception {
        peticion.header("X-Clave-Integracion", CLAVE);
        if (numero != null) {
            peticion.header("X-WhatsApp-Numero", numero);
        }
        if (cuerpo != null) {
            peticion.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(cuerpo));
        }
        return mvc.perform(peticion).andReturn();
    }

    private Map<String, Object> reserva(LocalDate fecha, String desde, String hasta, String nombre) {
        Map<String, Object> m = new HashMap<>();
        m.put("canchaId", canchaId);
        m.put("fecha", fecha.toString());
        m.put("desde", desde);
        m.put("hasta", hasta);
        if (nombre != null) {
            m.put("nombre", nombre);
        }
        return m;
    }

    private MvcResult reservar(String numero, LocalDate fecha, String desde, String hasta, String nombre, String clave) throws Exception {
        MockHttpServletRequestBuilder peticion = post(BASE + "/reservas");
        if (clave != null) {
            peticion.header("Idempotency-Key", clave);
        }
        return bot(peticion, numero, reserva(fecha, desde, hasta, nombre));
    }

    private String codigoDe(MvcResult r) throws Exception {
        return leer(r, "$.codigo");
    }

    /** Registra un cliente con ese número y lo vincula con el código que llega al correo. */
    private long clienteVinculado(String numero) throws Exception {
        String correoNuevo = correoUnico();
        Map<String, Object> datos = new HashMap<>(datosRegistro(correoNuevo, CLAVE_VALIDA));
        datos.put("telefono", numero);
        esperarEstado(enviar("/api/v1/autenticacion/registrar", datos), 201);
        esperarEstado(bot(post(BASE + "/vinculacion/codigo"), numero, null), 200);
        ArgumentCaptor<MensajeCorreo> captor = ArgumentCaptor.forClass(MensajeCorreo.class);
        verify(correo, atLeastOnce()).enviar(captor.capture());
        String codigo = captor.getAllValues().stream().filter(m -> m.destinatario().equals(correoNuevo))
                .reduce((a, b) -> b).map(m -> (String) m.variables().get("codigo")).orElseThrow();
        MvcResult vinculado = bot(post(BASE + "/vinculacion/confirmar"), numero, Map.of("codigo", codigo));
        esperarEstado(vinculado, 200);
        return jdbc.queryForObject("SELECT id FROM usuarios WHERE correo = ?", Long.class, correoNuevo);
    }

    // ---------- Seguridad ----------

    @Test
    void soloEntraQuienTraeLaClaveDeIntegracionYLaClaveNoSirveEnOtrasRutas() throws Exception {
        String numero = celular();
        esperarEstado(mvc.perform(get(BASE + "/contexto").header("X-WhatsApp-Numero", numero)).andReturn(), 401);
        esperarEstado(mvc.perform(get(BASE + "/contexto").header("X-WhatsApp-Numero", numero).header("X-Clave-Integracion", "otra")).andReturn(), 401);
        // un JWT de cliente o de administrador no sirve en la integración
        esperarEstado(mvc.perform(get(BASE + "/contexto").header("X-WhatsApp-Numero", numero)
                .header("Authorization", "Bearer " + nuevoClienteConSesion().token())).andReturn(), 401);
        esperarEstado(mvc.perform(get(BASE + "/contexto").header("X-WhatsApp-Numero", numero)
                .header("Authorization", "Bearer " + admin)).andReturn(), 401);
        // y la clave de integración no abre las demás rutas
        esperarEstado(mvc.perform(get("/api/v1/me").header("X-Clave-Integracion", CLAVE)).andReturn(), 401);
        esperarEstado(mvc.perform(get("/api/v1/admin/clientes").header("X-Clave-Integracion", CLAVE)).andReturn(), 401);

        MvcResult ok = bot(get(BASE + "/contexto"), numero, null);
        esperarEstado(ok, 200);
        assertThat((String) leer(ok, "$.situacion")).isEqualTo("SIN_CUENTA");
        assertThat((Boolean) leer(ok, "$.botActivo")).isTrue();
        assertThat((Integer) leer(ok, "$.politicas.horasMinimasCancelacion")).isNotNull();
        // sin el número del remitente no hay contexto
        esperarEstado(bot(get(BASE + "/contexto"), null, null), 400);
    }

    // ---------- Reservas sin cuenta ----------

    @Test
    void quienNoTieneCuentaReservaConSuNombreYSoloSuNumeroPuedeVerOCancelarla() throws Exception {
        String numero = celular();
        LocalDate fecha = diaHabil(3);

        esperarEstado(reservar(numero, fecha, "19:00", "20:00", null, null), 400); // la primera vez hace falta el nombre
        MvcResult creada = reservar(numero, fecha, "19:00", "20:00", "Pedro Pérez", "wamid.1");
        esperarEstado(creada, 201);
        String codigo = codigoDe(creada);
        assertThat((String) leer(creada, "$.estado")).isEqualTo("Pendiente de aprobación");
        assertThat((String) leer(creada, "$.desde")).isEqualTo("19:00");
        assertThat((Integer) leer(creada, "$.precio")).isEqualTo(80_000);
        assertThat(jdbc.queryForMap("SELECT r.origen, r.cliente_id, m.nombre, m.telefono FROM reservas r "
                + "JOIN clientes_mostrador m ON m.id = r.cliente_mostrador_id WHERE r.codigo = ?", codigo))
                .containsEntry("origen", "WHATSAPP").containsEntry("nombre", "Pedro Pérez").containsEntry("telefono", numero);

        // el mismo mensaje reintentado por n8n no crea otra reserva
        MvcResult repetida = reservar(numero, fecha, "19:00", "20:00", "Pedro Pérez", "wamid.1");
        esperarEstado(repetida, 200);
        assertThat(codigoDe(repetida)).isEqualTo(codigo);

        // el contexto ya lo saluda por su nombre y sus reservas aparecen
        assertThat((String) leer(bot(get(BASE + "/contexto"), numero, null), "$.nombre")).isEqualTo("Pedro Pérez");
        assertThat((List<String>) leer(bot(get(BASE + "/reservas"), numero, null), "$[*].codigo")).containsExactly(codigo);

        // otro número no la ve ni la puede cancelar
        String otro = celular();
        assertThat((List<?>) leer(bot(get(BASE + "/reservas"), otro, null), "$")).isEmpty();
        esperarEstado(bot(post(BASE + "/reservas/" + codigo + "/cancelar"), otro, Map.of("motivo", "no")), 404);

        MvcResult cancelada = bot(post(BASE + "/reservas/" + codigo + "/cancelar"), numero, Map.of("motivo", "Se cruzó con otro plan"));
        esperarEstado(cancelada, 200);
        assertThat((String) leer(cancelada, "$.estado")).isEqualTo("Cancelada");
        long id = jdbc.queryForObject("SELECT id FROM reservas WHERE codigo = ?", Long.class, codigo);
        assertThat(ocupaciones(id)).isZero();
        assertThat(jdbc.queryForObject("SELECT datos_nuevos ->> 'canal' FROM reserva_historial WHERE reserva_id = ? AND accion = 'CANCELADA'",
                String.class, id)).isEqualTo("WHATSAPP");
    }

    @Test
    void sinCuentaHayUnLimiteDeReservasActivasYElAdminPuedeApagarlas() throws Exception {
        String numero = celular();
        esperarEstado(reservar(numero, diaHabil(4), "08:00", "09:00", "Ana Gómez", null), 201);
        esperarEstado(reservar(numero, diaHabil(4), "09:00", "10:00", null, null), 201);
        MvcResult tercera = reservar(numero, diaHabil(4), "10:00", "11:00", null, null);
        esperarEstado(tercera, 422);
        assertThat((String) leer(tercera, "$.codigo")).isEqualTo("LIMITE_DE_RESERVAS");

        jdbc.update("UPDATE configuracion SET valor = 'false' WHERE clave = 'whatsapp_reservas_sin_cuenta'");
        MvcResult apagado = reservar(celular(), diaHabil(4), "11:00", "12:00", "Luis Díaz", null);
        esperarEstado(apagado, 403);
        assertThat((Boolean) leer(bot(get(BASE + "/contexto"), celular(), null), "$.puedeReservarSinCuenta")).isFalse();
    }

    // ---------- Clientes registrados ----------

    @Test
    void conElNumeroVinculadoReservaComoSuCuentaYLaVeEnLaWeb() throws Exception {
        String numero = celular();
        long clienteId = clienteVinculado(numero);
        assertThat((String) leer(bot(get(BASE + "/contexto"), numero, null), "$.situacion")).isEqualTo("CLIENTE");

        MvcResult creada = reservar(numero, diaHabil(5), "18:00", "19:00", null, "wamid.web");
        esperarEstado(creada, 201);
        assertThat(jdbc.queryForObject("SELECT cliente_id FROM reservas WHERE codigo = ?", Long.class, codigoDe(creada))).isEqualTo(clienteId);
        // también aparece en «Mis reservas» de la web
        Sesion web = ingresar(jdbc.queryForObject("SELECT correo FROM usuarios WHERE id = ?", String.class, clienteId), CLAVE_VALIDA, false);
        assertThat((List<String>) leer(obtener("/api/v1/me/reservas", web.token()), "$.contenido[*].codigo")).contains(codigoDe(creada));

        // reprogramar: primero se previsualiza, luego se mueve
        Map<String, Object> nueva = new HashMap<>(Map.of("fecha", diaHabil(5).toString(), "desde", "20:00", "hasta", "22:00", "motivo", "Llegamos tarde"));
        MvcResult previa = bot(post(BASE + "/reservas/" + codigoDe(creada) + "/reprogramar/previsualizar"), numero, nueva);
        esperarEstado(previa, 200);
        assertThat((Boolean) leer(previa, "$.disponible")).isTrue();
        MvcResult movida = bot(post(BASE + "/reservas/" + codigoDe(creada) + "/reprogramar"), numero, nueva);
        esperarEstado(movida, 200);
        assertThat((String) leer(movida, "$.desde")).isEqualTo("20:00");
        assertThat((String) leer(movida, "$.hasta")).isEqualTo("22:00");
    }

    @Test
    void unaCuentaSinVincularNoReservaHastaVincularYUnaDesactivadaNoUsaElBot() throws Exception {
        String numero = celular();
        Map<String, Object> datos = new HashMap<>(datosRegistro(correoUnico(), CLAVE_VALIDA));
        datos.put("telefono", numero);
        esperarEstado(enviar("/api/v1/autenticacion/registrar", datos), 201);

        assertThat((String) leer(bot(get(BASE + "/contexto"), numero, null), "$.situacion")).isEqualTo("SIN_VERIFICAR");
        MvcResult r = reservar(numero, diaHabil(6), "19:00", "20:00", "Ana", null);
        esperarEstado(r, 409);
        assertThat((String) leer(r, "$.codigo")).isEqualTo("WHATSAPP_SIN_VINCULAR");

        String otro = celular();
        long clienteId = clienteVinculado(otro);
        esperarEstado(postear("/api/v1/admin/clientes/" + clienteId + "/desactivar", admin, Map.of("motivo", "Prueba de desactivación")), 200);
        assertThat((String) leer(bot(get(BASE + "/contexto"), otro, null), "$.situacion")).isEqualTo("DESACTIVADO");
        esperarEstado(reservar(otro, diaHabil(6), "20:00", "21:00", null, null), 403);
    }

    // ---------- Consultas ----------

    @Test
    void laDisponibilidadSoloTraeFranjasLibresYElPrecioLoCalculaElServidor() throws Exception {
        LocalDate fecha = diaHabil(7);
        esperarEstado(reservar(celular(), fecha, "18:00", "19:00", "Carlos Ruiz", null), 201);

        MvcResult dia = bot(get(BASE + "/disponibilidad").param("fecha", fecha.toString()).param("sedeId", String.valueOf(sedeId)), null, null);
        esperarEstado(dia, 200);
        List<String> hastas = leer(dia, "$.canchas[0].franjasLibres[*].hasta");
        List<String> desdes = leer(dia, "$.canchas[0].franjasLibres[*].desde");
        assertThat(hastas).contains("18:00");
        assertThat(desdes).contains("19:00");

        MvcResult precio = bot(get(BASE + "/cotizacion").param("canchaId", String.valueOf(canchaId)).param("fecha", fecha.toString())
                .param("desde", "20:00").param("hasta", "22:00"), null, null);
        esperarEstado(precio, 200);
        assertThat((Integer) leer(precio, "$.total")).isEqualTo(160_000);
        assertThat((String) leer(precio, "$.totalTexto")).isEqualTo("$ 160.000");
    }

    @Test
    void elResumenDeUnPartidoTraeMarcadorYGolesPeroNoLosGoleadoresDelTorneo() throws Exception {
        List<Long> ids = torneoConEquipos(cuerpoTorneo("LIGA"), 2);
        long torneoId = ids.get(0);
        generarFixture(torneoId);
        iniciar(torneoId);
        Map<String, Object> p = partidos(torneoId).get(0);
        long partido = idPartido(p);
        long local = inscripcionLocal(p);
        List<Long> deLocal = jugadoresDe(torneoId, local);
        esperarEstado(postear("/api/v1/admin/partidos/" + partido + "/eventos", admin,
                Map.of("inscripcionId", local, "jugadorId", deLocal.get(0), "tipo", "GOL", "minuto", 12)), 201);
        resultado(partido, 1, 0);

        assertThat((List<Integer>) leer(bot(get(BASE + "/torneos"), null, null), "$[*].id")).contains((int) torneoId);
        MvcResult resumen = bot(get(BASE + "/partidos/" + partido + "/resumen"), null, null);
        esperarEstado(resumen, 200);
        assertThat((String) leer(resumen, "$.partido.marcador")).isEqualTo("1-0");
        assertThat((Integer) leer(resumen, "$.goles[0].minuto")).isEqualTo(12);
        assertThat((String) leer(resumen, "$.resumen")).contains("1-0").contains("12'");
        assertThat((List<?>) leer(bot(get(BASE + "/torneos/" + torneoId + "/partidos").param("jugados", "true"), null, null), "$")).hasSize(1);
        assertThat((List<?>) leer(bot(get(BASE + "/torneos/" + torneoId + "/tabla"), null, null), "$[0].filas")).hasSize(2);
        // no hay ruta de goleadores, tarjetas ni jugadores en el bot
        esperarEstado(bot(get(BASE + "/torneos/" + torneoId + "/estadisticas"), null, null), 404);
    }

    @Test
    void conElBotApagadoSoloRespondeElContexto() throws Exception {
        jdbc.update("UPDATE configuracion SET valor = 'false' WHERE clave = 'whatsapp_bot_activo'");
        String numero = celular();
        MvcResult contexto = bot(get(BASE + "/contexto"), numero, null);
        esperarEstado(contexto, 200);
        assertThat((Boolean) leer(contexto, "$.botActivo")).isFalse();
        MvcResult r = reservar(numero, diaHabil(3), "07:00", "08:00", "Ana", null);
        esperarEstado(r, 503);
        assertThat((String) leer(r, "$.codigo")).isEqualTo("SERVICIO_NO_DISPONIBLE");
        esperarEstado(bot(get(BASE + "/torneos"), null, null), 503);
    }

    // ---------- Avisos salientes ----------

    @Test
    void quienReservoPorWhatsappRecibeLaConfirmacionYUnNumeroSinVincularNuncaRecibeNada() throws Exception {
        String numero = celular();
        MvcResult creada = reservar(numero, diaHabil(8), "19:00", "20:00", "Marta Silva", null);
        esperarEstado(creada, 201);
        String codigo = codigoDe(creada);
        long id = jdbc.queryForObject("SELECT id FROM reservas WHERE codigo = ?", Long.class, codigo);
        aprobar(admin, id);
        verify(enviador, timeout(3000)).enviar(argThat(a -> a.numero().equals(numero) && a.tipo().equals("RESERVA_CONFIRMADA")
                && a.mensaje().contains(codigo) && a.mensaje().contains("confirmada")));

        // un cliente de la web con el número sin vincular: el aviso queda en la web, no va por WhatsApp
        String sinVincular = celular();
        Map<String, Object> datos = new HashMap<>(datosRegistro(correoUnico(), CLAVE_VALIDA));
        datos.put("telefono", sinVincular);
        esperarEstado(enviar("/api/v1/autenticacion/registrar", datos), 201);
        Sesion web = ingresar((String) datos.get("correo"), CLAVE_VALIDA, false);
        long deLaWeb = solicitarOk(web.token(), canchaId, diaHabil(8), "20:00", "21:00");
        aprobar(admin, deLaWeb);
        verify(enviador, after(1500).never()).enviar(argThat(a -> a.numero().equals(sinVincular)));
    }
}

