package co.edu.usco.goltime.promociones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import co.edu.usco.goltime.reservas.BaseReservasIT;

class PromocionesYPremiosIT extends BaseReservasIT {

    private String admin;
    private long sedeId;
    private long canchaId;
    private LocalDate fecha;
    private Long reglaActiva;

    @BeforeEach
    void preparar() throws Exception {
        admin = ingresarAdmin().token();
        sedeId = crearSede(admin);
        canchaId = crearCancha(admin, sedeId, "Cancha P"); // 80.000 la hora
        fecha = diaHabil(9);
    }

    @AfterEach
    void apagarReglas() throws Exception {
        // las reglas de premio son globales: se apagan para no afectar a las demás pruebas
        if (reglaActiva != null) {
            jdbc.update("UPDATE reglas_premio SET activa = FALSE WHERE id = ?", reglaActiva);
        }
    }

    // ----- Ayudantes -----

    private Map<String, Object> promo(String codigo, String tipo, int valor, Long cancha) {
        Map<String, Object> m = new HashMap<>();
        m.put("nombre", nombre("Promo"));
        m.put("codigo", codigo);
        m.put("tipo", tipo);
        m.put("valor", valor);
        m.put("fechaDesde", enDias(-1).toString());
        m.put("fechaHasta", enDias(60).toString());
        m.put("canchaId", cancha);
        return m;
    }

    private long crearPromo(Map<String, Object> cuerpo) throws Exception {
        MvcResult r = postear("/api/v1/admin/promociones", admin, cuerpo);
        esperarEstado(r, 201);
        return id(r);
    }

    private static String codigoUnico() {
        return "PRUEBA" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private MvcResult reservarConCodigo(String token, LocalDate dia, String desde, String hasta, String codigo, Long premioId) throws Exception {
        Map<String, Object> m = cuerpoReserva(canchaId, a(dia, desde), a(dia, hasta));
        m.put("codigoPromocion", codigo);
        m.put("premioId", premioId);
        return postear("/api/v1/reservas", token, m);
    }

    private int usosActivos(long promoId) throws Exception {
        return (Integer) leer(obtener("/api/v1/admin/promociones/" + promoId, admin), "$.usosActivos");
    }

    private long reservaAdminParaCliente(long clienteId, LocalDate dia) throws Exception {
        Map<String, Object> m = cuerpoReserva(canchaId, a(dia, "19:00"), a(dia, "20:00"));
        m.put("clienteId", clienteId);
        MvcResult r = postear("/api/v1/admin/reservas", admin, m);
        esperarEstado(r, 201);
        return id(r);
    }

    /** Mueve la reserva al pasado (en días distintos para no chocar) y la marca COMPLETADA. */
    private void completar(long reservaId, int haceDias) throws Exception {
        Instant inicio = Instant.now().minus(Duration.ofDays(haceDias)).minus(Duration.ofHours(3));
        Instant fin = inicio.plus(Duration.ofHours(1));
        jdbc.update("UPDATE reservas SET inicio = ?, fin = ? WHERE id = ?", OffsetDateTime.ofInstant(inicio, ZoneOffset.UTC), OffsetDateTime.ofInstant(fin, ZoneOffset.UTC),
                reservaId);
        jdbc.update("UPDATE ocupaciones_cancha SET rango = tstzrange(?, ?, '[)') WHERE reserva_id = ?", OffsetDateTime.ofInstant(inicio, ZoneOffset.UTC),
                OffsetDateTime.ofInstant(fin, ZoneOffset.UTC), reservaId);
        esperarEstado(parchear("/api/v1/admin/reservas/" + reservaId + "/estado", admin, Map.of("estado", "COMPLETADA", "motivo", "Partido jugado")), 200);
    }

    private long reglaCada(int reservas, String tipo, int valor) throws Exception {
        Map<String, Object> m = new HashMap<>();
        m.put("nombre", nombre("Premio"));
        m.put("reservasRequeridas", reservas);
        m.put("tipo", tipo);
        m.put("valor", valor);
        m.put("vigenciaDias", 30);
        MvcResult r = postear("/api/v1/admin/premios/reglas", admin, m);
        esperarEstado(r, 201);
        reglaActiva = id(r);
        return reglaActiva;
    }

    /** Campo del progreso de la regla de esta prueba. */
    private int progreso(MvcResult r, String campo) throws Exception {
        List<Number> valores = leer(r, "$.progreso[?(@.regla.id==" + reglaActiva + ")]." + campo);
        return valores.get(0).intValue();
    }

    private MvcResult misPremios(String token) throws Exception {
        MvcResult r = obtener("/api/v1/me/premios", token);
        esperarEstado(r, 200);
        return r;
    }

    // ----- Promociones -----

    @Test
    void elCodigoDescuentaSeLimitaPorCupoYSeLiberaAlCancelar() throws Exception {
        String codigo = codigoUnico();
        Map<String, Object> cuerpo = promo(codigo, "PORCENTAJE", 20, canchaId);
        cuerpo.put("usosMaximos", 1);
        long promoId = crearPromo(cuerpo);
        Sesion a = nuevoClienteConSesion();
        Sesion b = nuevoClienteConSesion();

        MvcResult r = reservarConCodigo(a.token(), fecha, "19:00", "20:00", codigo.toLowerCase(), null);

        esperarEstado(r, 201);
        assertThat(((Number) leer(r, "$.precioBase")).intValue()).isEqualTo(80_000);
        assertThat(((Number) leer(r, "$.descuento")).intValue()).isEqualTo(16_000);
        assertThat(((Number) leer(r, "$.precioFinal")).intValue()).isEqualTo(64_000);
        assertThat(usosActivos(promoId)).isEqualTo(1);

        // el cupo era de uno: el segundo cliente no puede
        MvcResult agotada = reservarConCodigo(b.token(), fecha, "21:00", "22:00", codigo, null);
        esperarEstado(agotada, 422);
        assertThat((String) leer(agotada, "$.codigo")).isEqualTo("PROMOCION_INVALIDA");
        assertThat((String) leer(agotada, "$.detail")).contains("agotó");

        // al cancelar la reserva el uso se libera y el cupo vuelve
        esperarEstado(postear("/api/v1/reservas/" + id(r) + "/cancelar", a.token(), Map.of()), 200);
        assertThat(usosActivos(promoId)).isZero();
        esperarEstado(reservarConCodigo(b.token(), fecha, "21:00", "22:00", codigo, null), 201);
        assertThat(usosActivos(promoId)).isEqualTo(1);
    }

    @Test
    void unCodigoInvalidoOQueNoAplicaSeExplicaYNoCreaLaReserva() throws Exception {
        Sesion cliente = nuevoClienteConSesion();
        String vencida = codigoUnico();
        Map<String, Object> pasada = promo(vencida, "PORCENTAJE", 10, canchaId);
        pasada.put("fechaDesde", enDias(-30).toString());
        pasada.put("fechaHasta", enDias(-2).toString());
        crearPromo(pasada);
        String deOtraCancha = codigoUnico();
        crearPromo(promo(deOtraCancha, "PORCENTAJE", 10, crearCancha(admin, sedeId, "Otra cancha")));
        String primera = codigoUnico();
        Map<String, Object> soloPrimera = promo(primera, "MONTO_FIJO", 5_000, canchaId);
        soloPrimera.put("soloPrimeraReserva", true);
        crearPromo(soloPrimera);

        esperarEstado(reservarConCodigo(cliente.token(), fecha, "19:00", "20:00", "NOEXISTE1", null), 422);
        assertThat((String) leer(reservarConCodigo(cliente.token(), fecha, "19:00", "20:00", vencida, null), "$.detail")).contains("vigente");
        assertThat((String) leer(reservarConCodigo(cliente.token(), fecha, "19:00", "20:00", deOtraCancha, null), "$.detail")).contains("cancha");
        // ninguna de esas solicitudes dejó una reserva
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reservas WHERE cliente_id = ?", Integer.class, cliente.id())).isZero();

        // la de "primera reserva" funciona la primera vez y no la segunda
        esperarEstado(reservarConCodigo(cliente.token(), fecha, "19:00", "20:00", primera, null), 201);
        MvcResult segunda = reservarConCodigo(cliente.token(), fecha, "21:00", "22:00", primera, null);
        esperarEstado(segunda, 422);
        assertThat((String) leer(segunda, "$.detail")).contains("primera");
    }

    @Test
    void laPromocionAutomaticaSeAplicaSolaYGanaElMayorDescuento() throws Exception {
        crearPromo(promo(null, "MONTO_FIJO", 5_000, canchaId)); // automática, solo en esta cancha
        Sesion cliente = nuevoClienteConSesion();

        MvcResult sinCodigo = reservarConCodigo(cliente.token(), fecha, "19:00", "20:00", null, null);
        esperarEstado(sinCodigo, 201);
        assertThat(((Number) leer(sinCodigo, "$.descuento")).intValue()).isEqualTo(5_000);

        // con un código del 20% (16.000) gana el código y la automática no se consume
        String codigo = codigoUnico();
        long promoCodigo = crearPromo(promo(codigo, "PORCENTAJE", 20, canchaId));
        MvcResult conCodigo = reservarConCodigo(cliente.token(), fecha, "21:00", "22:00", codigo, null);
        esperarEstado(conCodigo, 201);
        assertThat(((Number) leer(conCodigo, "$.descuento")).intValue()).isEqualTo(16_000);
        assertThat(usosActivos(promoCodigo)).isEqualTo(1);
    }

    @Test
    void laPrevisualizacionMuestraElPrecioFinalYRechazaCodigosInvalidos() throws Exception {
        String codigo = codigoUnico();
        crearPromo(promo(codigo, "PORCENTAJE", 25, canchaId));
        Sesion cliente = nuevoClienteConSesion();
        String base = "/api/v1/me/descuentos/previsualizar?canchaId=" + canchaId + "&inicio=" + a(fecha, "19:00") + "&fin=" + a(fecha, "20:00");

        MvcResult r = obtener(base + "&codigo=" + codigo, cliente.token());

        esperarEstado(r, 200);
        assertThat(((Number) leer(r, "$.descuento")).intValue()).isEqualTo(20_000);
        assertThat(((Number) leer(r, "$.precioFinal")).intValue()).isEqualTo(60_000);
        assertThat((String) leer(r, "$.origen")).isEqualTo("PROMOCION");
        MvcResult sin = obtener(base, cliente.token());
        assertThat(((Number) leer(sin, "$.descuento")).intValue()).isZero();
        esperarEstado(obtener(base + "&codigo=NOEXISTE", cliente.token()), 422);
        esperarEstado(obtener(base, null), 401);
        // previsualizar no consume nada
        assertThat(jdbc.queryForObject("SELECT count(*) FROM usos_promocion WHERE cliente_id = ?", Integer.class, cliente.id())).isZero();
    }

    @Test
    void alReprogramarSeConservaElDescuento() throws Exception {
        String codigo = codigoUnico();
        crearPromo(promo(codigo, "PORCENTAJE", 20, canchaId));
        Sesion cliente = nuevoClienteConSesion();
        long reservaId = id(reservarConCodigo(cliente.token(), fecha, "19:00", "20:00", codigo, null));

        MvcResult r = parchear("/api/v1/reservas/" + reservaId + "/reprogramar", cliente.token(),
                Map.of("inicio", a(fecha, "21:00").toString(), "fin", a(fecha, "22:00").toString()));

        esperarEstado(r, 200);
        assertThat(((Number) leer(r, "$.descuento")).intValue()).isEqualTo(16_000);
        assertThat(((Number) leer(r, "$.precioFinal")).intValue()).isEqualTo(64_000);
    }

    @Test
    void adminAdministraPromocionesYElPublicoSoloVeLasPublicasVigentes() throws Exception {
        String codigoPublico = codigoUnico();
        long publica = crearPromo(promo(codigoPublico, "PORCENTAJE", 15, canchaId));
        Map<String, Object> privada = promo(codigoUnico(), "PORCENTAJE", 50, canchaId);
        privada.put("publica", false);
        long idPrivada = crearPromo(privada);

        esperarEstado(postear("/api/v1/admin/promociones", admin, promo(codigoPublico, "PORCENTAJE", 5, canchaId)), 409); // código repetido
        Map<String, Object> invalida = promo(codigoUnico(), "PORCENTAJE", 150, canchaId);
        esperarEstado(postear("/api/v1/admin/promociones", admin, invalida), 400);

        List<Number> ids = leer(obtener("/api/v1/publico/promociones", null), "$[*].id");
        assertThat(ids.stream().map(Number::longValue)).contains(publica).doesNotContain(idPrivada);

        esperarEstado(postear("/api/v1/admin/promociones/" + publica + "/estado", admin, Map.of("activa", false)), 200);
        List<Number> despues = leer(obtener("/api/v1/publico/promociones", null), "$[*].id");
        assertThat(despues.stream().map(Number::longValue)).doesNotContain(publica);
        Sesion cliente = nuevoClienteConSesion();
        esperarEstado(reservarConCodigo(cliente.token(), fecha, "19:00", "20:00", codigoPublico, null), 422); // desactivada
        esperarEstado(obtener("/api/v1/admin/promociones", cliente.token()), 403);
        esperarEstado(postear("/api/v1/admin/promociones", cliente.token(), promo(codigoUnico(), "PORCENTAJE", 5, canchaId)), 403);

        MvcResult edit = poner("/api/v1/admin/promociones/" + publica, promo(codigoPublico, "PORCENTAJE", 30, canchaId));
        esperarEstado(edit, 200);
        assertThat((Integer) leer(edit, "$.valor")).isEqualTo(30);
        assertThat((Boolean) leer(edit, "$.activa")).as("editar no reactiva").isFalse();
    }

    private MvcResult poner(String url, Object cuerpo) throws Exception {
        return enviarJson(put(url), admin, cuerpo);
    }

    // ----- Premios -----

    @Test
    void cadaNReservasCompletadasGanaUnPremioQueSeUsaYSeDevuelveAlCancelar() throws Exception {
        reglaCada(2, "PORCENTAJE", 10);
        Sesion cliente = nuevoClienteConSesion();

        completar(reservaAdminParaCliente(cliente.id(), diaHabil(20)), 3);
        MvcResult primero = misPremios(cliente.token());
        assertThat((List<?>) leer(primero, "$.premios")).isEmpty();
        assertThat(progreso(primero, "enCiclo")).isEqualTo(1);
        assertThat(progreso(primero, "faltan")).isEqualTo(1);

        completar(reservaAdminParaCliente(cliente.id(), diaHabil(21)), 4);
        MvcResult segundo = misPremios(cliente.token());
        assertThat((List<?>) leer(segundo, "$.premios")).hasSize(1);
        assertThat((String) leer(segundo, "$.premios[0].estado")).isEqualTo("DISPONIBLE");
        assertThat((String) leer(segundo, "$.premios[0].beneficio")).isEqualTo("10%");
        long premioId = ((Number) leer(segundo, "$.premios[0].id")).longValue();
        assertThat(notificaciones(cliente.id(), "PREMIO_GANADO")).isEqualTo(1);
        // completar otra más (la tercera) no otorga otro hasta la cuarta
        completar(reservaAdminParaCliente(cliente.id(), diaHabil(22)), 5);
        assertThat((List<?>) leer(misPremios(cliente.token()), "$.premios")).hasSize(1);

        // usar el premio: 10% de 80.000
        MvcResult reserva = reservarConCodigo(cliente.token(), fecha, "19:00", "20:00", null, premioId);
        esperarEstado(reserva, 201);
        assertThat(((Number) leer(reserva, "$.descuento")).intValue()).isEqualTo(8_000);
        assertThat((String) leer(misPremios(cliente.token()), "$.premios[0].estado")).isEqualTo("USADO");
        // un premio usado no se puede reutilizar
        MvcResult otra = reservarConCodigo(cliente.token(), fecha, "21:00", "22:00", null, premioId);
        esperarEstado(otra, 422);
        assertThat((String) leer(otra, "$.codigo")).isEqualTo("PROMOCION_INVALIDA");

        // si la reserva se cancela, el premio vuelve a estar disponible
        esperarEstado(postear("/api/v1/reservas/" + id(reserva) + "/cancelar", cliente.token(), Map.of()), 200);
        assertThat((String) leer(misPremios(cliente.token()), "$.premios[0].estado")).isEqualTo("DISPONIBLE");
    }

    @Test
    void unPremioAjenoONoExistenteNoSePuedeUsar() throws Exception {
        reglaCada(1, "MONTO_FIJO", 10_000);
        Sesion duenio = nuevoClienteConSesion();
        Sesion otro = nuevoClienteConSesion();
        completar(reservaAdminParaCliente(duenio.id(), diaHabil(20)), 3);
        long premioId = ((Number) leer(misPremios(duenio.token()), "$.premios[0].id")).longValue();

        esperarEstado(reservarConCodigo(otro.token(), fecha, "19:00", "20:00", null, premioId), 422); // ajeno
        esperarEstado(reservarConCodigo(otro.token(), fecha, "19:00", "20:00", null, 999_999L), 422);
        MvcResult mio = reservarConCodigo(duenio.token(), fecha, "19:00", "20:00", null, premioId);
        esperarEstado(mio, 201);
        assertThat(((Number) leer(mio, "$.descuento")).intValue()).isEqualTo(10_000);
    }

    @Test
    void corregirUnaCompletadaANoAsistioRevocaElPremioSiNoSeHaUsado() throws Exception {
        reglaCada(2, "PORCENTAJE", 10);
        Sesion cliente = nuevoClienteConSesion();
        long r1 = reservaAdminParaCliente(cliente.id(), diaHabil(20));
        long r2 = reservaAdminParaCliente(cliente.id(), diaHabil(21));
        completar(r1, 3);
        completar(r2, 4);
        assertThat((List<?>) leer(misPremios(cliente.token()), "$.premios")).hasSize(1);

        esperarEstado(parchear("/api/v1/admin/reservas/" + r2 + "/estado", admin, Map.of("estado", "NO_ASISTIO", "motivo", "No se presentaron")), 200);

        assertThat((List<?>) leer(misPremios(cliente.token()), "$.premios")).isEmpty();
        List<String> estados = leer(obtener("/api/v1/admin/premios/otorgados?clienteId=" + cliente.id(), admin), "$[*].estado");
        assertThat(estados).containsExactly("REVOCADO");
        assertThat(progreso(misPremios(cliente.token()), "enCiclo")).isEqualTo(1);
    }

    @Test
    void elAdminMarcaCompletadaUnaReservaFuturaYCuentaParaPremiosHastaQueLaDevuelveAPendiente() throws Exception {
        reglaCada(1, "MONTO_FIJO", 10_000);
        Sesion cliente = nuevoClienteConSesion();
        long futura = reservaAdminParaCliente(cliente.id(), diaHabil(20));
        String url = "/api/v1/admin/reservas/" + futura + "/estado";

        esperarEstado(parchear(url, admin, Map.of("estado", "COMPLETADA", "motivo", "Pagó y se da por jugada")), 200);
        MvcResult conPremio = misPremios(cliente.token());
        assertThat((List<?>) leer(conPremio, "$.premios")).hasSize(1);
        assertThat((String) leer(conPremio, "$.premios[0].estado")).isEqualTo("DISPONIBLE");

        esperarEstado(parchear(url, admin, Map.of("estado", "PENDIENTE", "motivo", "Se marcó por error")), 200);
        assertThat((List<?>) leer(misPremios(cliente.token()), "$.premios")).isEmpty();
        List<String> estados = leer(obtener("/api/v1/admin/premios/otorgados?clienteId=" + cliente.id(), admin), "$[*].estado");
        assertThat(estados).containsExactly("REVOCADO");
    }

    @Test
    void unPremioYaUsadoNoSeRevocaYElAdminPuedeRevocarUnoDisponible() throws Exception {
        reglaCada(1, "PORCENTAJE", 50);
        Sesion cliente = nuevoClienteConSesion();
        long r1 = reservaAdminParaCliente(cliente.id(), diaHabil(20));
        completar(r1, 3);
        long premioId = ((Number) leer(misPremios(cliente.token()), "$.premios[0].id")).longValue();
        esperarEstado(reservarConCodigo(cliente.token(), fecha, "19:00", "20:00", null, premioId), 201);

        esperarEstado(parchear("/api/v1/admin/reservas/" + r1 + "/estado", admin, Map.of("estado", "NO_ASISTIO", "motivo", "No se presentaron")), 200);
        assertThat((String) leer(misPremios(cliente.token()), "$.premios[0].estado")).as("ya se usó: se conserva").isEqualTo("USADO");
        esperarEstado(postear("/api/v1/admin/premios/otorgados/" + premioId + "/revocar", admin, Map.of("motivo", "Error de captura")), 409); // no está disponible

        Sesion otro = nuevoClienteConSesion();
        completar(reservaAdminParaCliente(otro.id(), diaHabil(22)), 5);
        long disponible = ((Number) leer(misPremios(otro.token()), "$.premios[0].id")).longValue();
        esperarEstado(postear("/api/v1/admin/premios/otorgados/" + disponible + "/revocar", admin, Map.of("motivo", " ")), 400);
        esperarEstado(postear("/api/v1/admin/premios/otorgados/" + disponible + "/revocar", cliente.token(), Map.of("motivo", "x")), 403);
        esperarEstado(postear("/api/v1/admin/premios/otorgados/" + disponible + "/revocar", admin, Map.of("motivo", "Fraude detectado")), 200);
        assertThat((List<?>) leer(misPremios(otro.token()), "$.premios")).isEmpty();
    }

    @Test
    void losPartidosDeTorneoSinClienteNoCuentanParaPremios() throws Exception {
        reglaCada(1, "PORCENTAJE", 10);
        Sesion cliente = nuevoClienteConSesion();
        // una reserva de torneo completada (sin cliente) no rompe ni otorga nada
        long torneo = jdbc.queryForObject("""
                INSERT INTO reservas (codigo, cancha_id, inicio, fin, estado, origen, precio_base, precio_final) VALUES (?, ?, now() - interval '5 days', now() - interval '5 days' + interval '1 hour', 'COMPLETADA', 'TORNEO', 0, 0)
                RETURNING id""", Long.class, "T-" + UUID.randomUUID().toString().substring(0, 10), canchaId);
        assertThat(torneo).isPositive();
        assertThat((List<?>) leer(misPremios(cliente.token()), "$.premios")).isEmpty();
        assertThat(progreso(misPremios(cliente.token()), "completadas")).isZero();
    }
}
