package co.edu.usco.goltime.reservas;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import co.edu.usco.goltime.reservas.aplicacion.TareasReservas;

class CicloReservasIT extends BaseReservasIT {

    @Autowired
    TareasReservas tareas;

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
        fecha = enDias(5);
    }

    /** Mueve una reserva (y su ocupación) a otro instante, para simular el paso del tiempo sin esperar. */
    private void mover(long reservaId, Instant inicio, Instant fin) {
        OffsetDateTime i = OffsetDateTime.ofInstant(inicio, ZoneOffset.UTC);
        OffsetDateTime f = OffsetDateTime.ofInstant(fin, ZoneOffset.UTC);
        jdbc.update("UPDATE reservas SET inicio = ?, fin = ? WHERE id = ?", i, f, reservaId);
        jdbc.update("UPDATE ocupaciones_cancha SET rango = tstzrange(?, ?, '[)') WHERE reserva_id = ?", i, f, reservaId);
    }

    private MvcResult cancelar(String token, long reservaId, String motivo) throws Exception {
        return postear("/api/v1/reservas/" + reservaId + "/cancelar", token, motivo == null ? Map.of() : Map.of("motivo", motivo));
    }

    private Map<String, Object> cuerpoReprogramacion(Long cancha, LocalDate f, String desde, String hasta) {
        Map<String, Object> m = new HashMap<>();
        if (cancha != null) {
            m.put("canchaId", cancha);
        }
        m.put("inicio", a(f, desde).toString());
        m.put("fin", a(f, hasta).toString());
        return m;
    }

    private void agregarFranjaNoche(long cancha, int precio) throws Exception {
        Map<String, Object> franja = new HashMap<>();
        franja.put("diasSemana", List.of(1, 2, 3, 4, 5, 6, 7));
        franja.put("horaInicio", "17:00");
        franja.put("horaFin", "23:00");
        franja.put("precioHora", precio);
        esperarEstado(postear("/api/v1/admin/canchas/" + cancha + "/tarifas", admin, franja), 201);
    }

    // ===================== Cancelación =====================

    @Test
    void elClienteCancelaUnaPendienteSinPenalidadYSeLiberaElHorario() throws Exception {
        long reservaId = solicitarOk(cliente.token(), canchaId, fecha, "19:00", "20:00");

        MvcResult r = cancelar(cliente.token(), reservaId, "Se canceló el partido");

        esperarEstado(r, 200);
        assertThat((String) leer(r, "$.estado")).isEqualTo("CANCELADA");
        assertThat((Boolean) leer(r, "$.cancelacionTardia")).isFalse();
        assertThat((String) leer(r, "$.canceladaPorRol")).isEqualTo("CLIENTE");
        assertThat((String) leer(r, "$.motivoCancelacion")).isEqualTo("Se canceló el partido");
        assertThat((Boolean) leer(r, "$.puedeCancelar")).isFalse();
        assertThat(ocupaciones(reservaId)).isZero();
        assertThat(acciones(reservaId)).containsExactly("CREADA", "CANCELADA");
        // el horario vuelve a estar disponible para otro cliente
        esperarEstado(solicitar(nuevoClienteConSesion().token(), canchaId, fecha, "19:00", "20:00"), 201);
        // no se cancela dos veces
        MvcResult otraVez = cancelar(cliente.token(), reservaId, null);
        esperarEstado(otraVez, 409);
        assertThat((String) leer(otraVez, "$.codigo")).isEqualTo("ESTADO_INVALIDO");
    }

    @Test
    void cancelarUnaConfirmadaConAntelacionNoEsTardiaYAvisaAlAdmin() throws Exception {
        long reservaId = solicitarOk(cliente.token(), canchaId, fecha, "19:00", "20:00");
        aprobar(admin, reservaId);
        int avisosAntes = notificaciones(adminId, "RESERVA_CANCELADA");

        MvcResult r = cancelar(cliente.token(), reservaId, null);

        assertThat((Boolean) leer(r, "$.cancelacionTardia")).isFalse();
        assertThat(notificaciones(adminId, "RESERVA_CANCELADA")).isEqualTo(avisosAntes + 1);
    }

    @Test
    void cancelarUnaConfirmadaFaltandoMenosDeLasHorasMinimasQuedaComoTardia() throws Exception {
        long reservaId = solicitarOk(cliente.token(), canchaId, fecha, "19:00", "20:00");
        aprobar(admin, reservaId);
        mover(reservaId, Instant.now().plus(Duration.ofHours(2)), Instant.now().plus(Duration.ofHours(3)));

        MvcResult detalle = obtener("/api/v1/reservas/" + reservaId, cliente.token());
        assertThat((Boolean) leer(detalle, "$.cancelacionSeriaTardia")).isTrue();

        MvcResult r = cancelar(cliente.token(), reservaId, "Me enfermé");
        esperarEstado(r, 200);
        assertThat((String) leer(r, "$.estado")).isEqualTo("CANCELADA");
        assertThat((Boolean) leer(r, "$.cancelacionTardia")).isTrue();
        assertThat(jdbc.queryForObject("SELECT cancelacion_tardia FROM reservas WHERE id = ?", Boolean.class, reservaId)).isTrue();
    }

    @Test
    void unClienteNoVeNiCancelaNiReprogramaLasReservasDeOtro() throws Exception {
        long reservaId = solicitarOk(cliente.token(), canchaId, fecha, "19:00", "20:00");
        Sesion intruso = nuevoClienteConSesion();

        esperarEstado(obtener("/api/v1/reservas/" + reservaId, intruso.token()), 404);
        esperarEstado(cancelar(intruso.token(), reservaId, null), 404);
        esperarEstado(parchear("/api/v1/reservas/" + reservaId + "/reprogramar", intruso.token(), cuerpoReprogramacion(null, fecha, "20:00", "21:00")), 404);
        esperarEstado(postear("/api/v1/reservas/" + reservaId + "/reprogramar/previsualizar", intruso.token(), cuerpoReprogramacion(null, fecha, "20:00", "21:00")), 404);
        // sigue intacta
        assertThat(estadoDe(reservaId)).isEqualTo("PENDIENTE");
        // y no aparece en el listado del intruso
        assertThat((List<?>) leer(obtener("/api/v1/me/reservas", intruso.token()), "$.contenido")).isEmpty();
        // las rutas de administración están vedadas
        esperarEstado(postear("/api/v1/admin/reservas/" + reservaId + "/aprobar", intruso.token(), Map.of()), 403);
        esperarEstado(obtener("/api/v1/admin/reservas", intruso.token()), 403);
    }

    // ===================== Reprogramación =====================

    @Test
    void reprogramarCambiaHorarioYPrecioMueveLaOcupacionYDejaHistorial() throws Exception {
        agregarFranjaNoche(canchaId, 100_000);
        long reservaId = solicitarOk(cliente.token(), canchaId, fecha, "19:00", "20:00");

        MvcResult previa = postear("/api/v1/reservas/" + reservaId + "/reprogramar/previsualizar", cliente.token(), cuerpoReprogramacion(null, fecha, "10:00", "11:00"));
        esperarEstado(previa, 200);
        assertThat((Boolean) leer(previa, "$.disponible")).isTrue();
        assertThat((Integer) leer(previa, "$.precioActual")).isEqualTo(100_000);
        assertThat((Integer) leer(previa, "$.precioNuevo")).isEqualTo(80_000);
        assertThat((Integer) leer(previa, "$.diferencia")).isEqualTo(-20_000);
        assertThat(estadoDe(reservaId)).as("previsualizar no cambia nada").isEqualTo("PENDIENTE");

        MvcResult r = parchear("/api/v1/reservas/" + reservaId + "/reprogramar", cliente.token(), cuerpoReprogramacion(null, fecha, "10:00", "11:00"));
        esperarEstado(r, 200);
        assertThat(Instant.parse(leer(r, "$.inicio"))).isEqualTo(a(fecha, "10:00"));
        assertThat((Integer) leer(r, "$.precioFinal")).isEqualTo(80_000);
        assertThat((Integer) leer(r, "$.vecesReprogramada")).isEqualTo(1);
        assertThat(ocupaciones(reservaId)).isEqualTo(1);

        // el horario viejo quedó libre y el nuevo ocupado
        esperarEstado(solicitar(nuevoClienteConSesion().token(), canchaId, fecha, "19:00", "20:00"), 201);
        esperarEstado(solicitar(nuevoClienteConSesion().token(), canchaId, fecha, "10:00", "11:00"), 409);

        // el historial dice quién, cuándo y qué cambió
        MvcResult detalle = obtener("/api/v1/reservas/" + reservaId, cliente.token());
        assertThat(acciones(reservaId)).containsExactly("CREADA", "REPROGRAMADA");
        assertThat((String) leer(detalle, "$.historial[1].accion")).isEqualTo("REPROGRAMADA");
        assertThat((String) leer(detalle, "$.historial[1].actorNombre")).isEqualTo("Ana Pérez");
        assertThat((String) leer(detalle, "$.historial[1].actorRol")).isEqualTo("CLIENTE");
        assertThat((Integer) leer(detalle, "$.historial[1].datosAnteriores.precioFinal")).isEqualTo(100_000);
        assertThat((Integer) leer(detalle, "$.historial[1].datosNuevos.precioFinal")).isEqualTo(80_000);
    }

    @Test
    void reprogramarAUnHorarioOcupadoFallaYDejaTodoComoEstaba() throws Exception {
        long reservaId = solicitarOk(cliente.token(), canchaId, fecha, "19:00", "20:00");
        solicitarOk(nuevoClienteConSesion().token(), canchaId, fecha, "21:00", "22:00");

        MvcResult previa = postear("/api/v1/reservas/" + reservaId + "/reprogramar/previsualizar", cliente.token(), cuerpoReprogramacion(null, fecha, "21:00", "22:00"));
        assertThat((Boolean) leer(previa, "$.disponible")).isFalse();

        MvcResult r = parchear("/api/v1/reservas/" + reservaId + "/reprogramar", cliente.token(), cuerpoReprogramacion(null, fecha, "21:00", "22:00"));
        esperarEstado(r, 409);
        assertThat((String) leer(r, "$.codigo")).isEqualTo("RESERVA_SOLAPADA");
        // la reserva conserva su horario y su ocupación original (la transacción se revirtió completa)
        MvcResult detalle = obtener("/api/v1/reservas/" + reservaId, cliente.token());
        assertThat(Instant.parse(leer(detalle, "$.inicio"))).isEqualTo(a(fecha, "19:00"));
        assertThat((Integer) leer(detalle, "$.vecesReprogramada")).isZero();
        assertThat(ocupaciones(reservaId)).isEqualTo(1);
        assertThat(acciones(reservaId)).containsExactly("CREADA");
        esperarEstado(solicitar(nuevoClienteConSesion().token(), canchaId, fecha, "19:00", "20:00"), 409);
    }

    @Test
    void sePuedeReprogramarAOtraCancha() throws Exception {
        long otra = crearCancha(admin, sedeId, "Cancha B", 5, 90_000, null);
        long reservaId = solicitarOk(cliente.token(), canchaId, fecha, "19:00", "20:00");

        MvcResult r = parchear("/api/v1/reservas/" + reservaId + "/reprogramar", cliente.token(), cuerpoReprogramacion(otra, fecha, "19:00", "20:00"));

        esperarEstado(r, 200);
        assertThat((String) leer(r, "$.cancha.nombre")).isEqualTo("Cancha B");
        assertThat((Integer) leer(r, "$.precioFinal")).isEqualTo(90_000);
        esperarEstado(solicitar(nuevoClienteConSesion().token(), canchaId, fecha, "19:00", "20:00"), 201);
    }

    @Test
    void elClienteSoloPuedeReprogramarElMaximoDeVecesPermitido() throws Exception {
        long reservaId = solicitarOk(cliente.token(), canchaId, fecha, "19:00", "20:00");
        esperarEstado(parchear("/api/v1/reservas/" + reservaId + "/reprogramar", cliente.token(), cuerpoReprogramacion(null, fecha, "18:00", "19:00")), 200);
        esperarEstado(parchear("/api/v1/reservas/" + reservaId + "/reprogramar", cliente.token(), cuerpoReprogramacion(null, fecha, "17:00", "18:00")), 200);

        MvcResult tercera = parchear("/api/v1/reservas/" + reservaId + "/reprogramar", cliente.token(), cuerpoReprogramacion(null, fecha, "16:00", "17:00"));
        esperarEstado(tercera, 422);
        assertThat((String) leer(tercera, "$.codigo")).isEqualTo("REPROGRAMACION_NO_PERMITIDA");
        MvcResult detalle = obtener("/api/v1/reservas/" + reservaId, cliente.token());
        assertThat((Boolean) leer(detalle, "$.puedeReprogramar")).isFalse();
    }

    @Test
    void noSeReprogramaConMenosDeLasHorasMinimasDeAntelacion() throws Exception {
        long reservaId = solicitarOk(cliente.token(), canchaId, fecha, "19:00", "20:00");
        aprobar(admin, reservaId);
        mover(reservaId, Instant.now().plus(Duration.ofHours(3)), Instant.now().plus(Duration.ofHours(4)));

        MvcResult r = parchear("/api/v1/reservas/" + reservaId + "/reprogramar", cliente.token(), cuerpoReprogramacion(null, fecha, "20:00", "21:00"));
        esperarEstado(r, 422);
        assertThat((String) leer(r, "$.codigo")).isEqualTo("REPROGRAMACION_NO_PERMITIDA");
    }

    // ===================== Aprobación del administrador =====================

    @Test
    void elAdminApruebaYElClienteRecibeLaNotificacion() throws Exception {
        long reservaId = solicitarOk(cliente.token(), canchaId, fecha, "19:00", "20:00");

        MvcResult r = postear("/api/v1/admin/reservas/" + reservaId + "/aprobar", admin, Map.of());

        esperarEstado(r, 200);
        assertThat((String) leer(r, "$.estado")).isEqualTo("CONFIRMADA");
        assertThat(jdbc.queryForObject("SELECT aprobada_por FROM reservas WHERE id = ?", Long.class, reservaId)).isEqualTo(adminId);
        assertThat(jdbc.queryForObject("SELECT expira_en IS NULL FROM reservas WHERE id = ?", Boolean.class, reservaId)).isTrue();
        assertThat(notificaciones(cliente.id(), "RESERVA_CONFIRMADA")).isEqualTo(1);
        assertThat(acciones(reservaId)).containsExactly("CREADA", "APROBADA");
        // no se aprueba dos veces
        esperarEstado(postear("/api/v1/admin/reservas/" + reservaId + "/aprobar", admin, Map.of()), 409);
    }

    @Test
    void rechazarExigeMotivoLiberaElHorarioYAvisaAlCliente() throws Exception {
        long reservaId = solicitarOk(cliente.token(), canchaId, fecha, "19:00", "20:00");
        String url = "/api/v1/admin/reservas/" + reservaId + "/rechazar";

        esperarEstado(postear(url, admin, Map.of("motivo", " ")), 400);
        assertThat(estadoDe(reservaId)).isEqualTo("PENDIENTE");

        MvcResult r = postear(url, admin, Map.of("motivo", "Esa hora la reservamos para un evento"));
        esperarEstado(r, 200);
        assertThat((String) leer(r, "$.estado")).isEqualTo("RECHAZADA");
        assertThat((String) leer(r, "$.motivoRechazo")).isEqualTo("Esa hora la reservamos para un evento");
        assertThat(ocupaciones(reservaId)).isZero();
        assertThat(notificaciones(cliente.id(), "RESERVA_RECHAZADA")).isEqualTo(1);
        esperarEstado(solicitar(nuevoClienteConSesion().token(), canchaId, fecha, "19:00", "20:00"), 201);
        // una rechazada aparece en la pestaña de canceladas del cliente
        assertThat((List<?>) leer(obtener("/api/v1/me/reservas?pestana=CANCELADAS", cliente.token()), "$.contenido")).hasSize(1);
    }

    @Test
    void elAdminCancelaConMotivoYElClienteEsNotificado() throws Exception {
        long reservaId = solicitarOk(cliente.token(), canchaId, fecha, "19:00", "20:00");
        aprobar(admin, reservaId);
        String url = "/api/v1/admin/reservas/" + reservaId + "/cancelar";

        esperarEstado(postear(url, admin, Map.of("motivo", "")), 400);
        MvcResult r = postear(url, admin, Map.of("motivo", "Daño en la cancha"));
        esperarEstado(r, 200);
        assertThat((String) leer(r, "$.estado")).isEqualTo("CANCELADA");
        assertThat((String) leer(r, "$.canceladaPorRol")).isEqualTo("ADMIN");
        assertThat((Boolean) leer(r, "$.cancelacionTardia")).isFalse();
        assertThat(notificaciones(cliente.id(), "RESERVA_CANCELADA_ADMIN")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT cancelada_por FROM reservas WHERE id = ?", Long.class, reservaId)).isEqualTo(adminId);
    }

    @Test
    void elListadoDeAdminPoneLasPendientesPrimeroYFiltra() throws Exception {
        Sesion otro = nuevoClienteConSesion();
        long confirmada = solicitarOk(cliente.token(), canchaId, fecha, "19:00", "20:00");
        aprobar(admin, confirmada);
        long pendiente = solicitarOk(otro.token(), canchaId, fecha, "20:00", "21:00");

        MvcResult todas = obtener("/api/v1/admin/reservas?sedeId=" + sedeId, admin);
        esperarEstado(todas, 200);
        assertThat(((Number) leer(todas, "$.contenido[0].id")).longValue()).as("la pendiente va primero").isEqualTo(pendiente);
        assertThat((Integer) leer(todas, "$.totalElementos")).isEqualTo(2);

        MvcResult soloPendientes = obtener("/api/v1/admin/reservas?sedeId=" + sedeId + "&estado=PENDIENTE", admin);
        assertThat((List<?>) leer(soloPendientes, "$.contenido")).hasSize(1);

        String codigo = leer(obtener("/api/v1/admin/reservas/" + confirmada, admin), "$.codigo");
        MvcResult porCodigo = obtener("/api/v1/admin/reservas?busqueda=" + codigo.toLowerCase(), admin);
        assertThat((List<?>) leer(porCodigo, "$.contenido")).hasSize(1);
        MvcResult porNombre = obtener("/api/v1/admin/reservas?sedeId=" + sedeId + "&busqueda=ana", admin);
        assertThat((Integer) leer(porNombre, "$.totalElementos")).isEqualTo(2);
        assertThat((String) leer(porNombre, "$.contenido[0].cliente.correo")).contains("@prueba.test");
    }

    // ===================== Mis reservas =====================

    @Test
    void misReservasSeSeparanEnProximasHistorialYCanceladas() throws Exception {
        long proxima = solicitarOk(cliente.token(), canchaId, fecha, "19:00", "20:00");
        long cancelada = solicitarOk(cliente.token(), canchaId, fecha, "20:00", "21:00");
        cancelar(cliente.token(), cancelada, null);
        long jugada = solicitarOk(cliente.token(), canchaId, fecha, "21:00", "22:00");
        aprobar(admin, jugada);
        mover(jugada, Instant.now().minus(Duration.ofHours(3)), Instant.now().minus(Duration.ofHours(2)));
        tareas.completarTerminadas();

        assertThat(ids(obtener("/api/v1/me/reservas?pestana=PROXIMAS", cliente.token()))).containsExactly(proxima);
        assertThat(ids(obtener("/api/v1/me/reservas?pestana=CANCELADAS", cliente.token()))).containsExactly(cancelada);
        assertThat(ids(obtener("/api/v1/me/reservas?pestana=HISTORIAL", cliente.token()))).containsExactly(jugada);
        assertThat(ids(obtener("/api/v1/me/reservas", cliente.token()))).as("por defecto, las próximas").containsExactly(proxima);
    }

    private List<Long> ids(MvcResult r) throws Exception {
        List<Object> ids = leer(r, "$.contenido[*].id");
        return ids.stream().map(o -> ((Number) o).longValue()).toList();
    }

    // ===================== Tareas automáticas =====================

    @Test
    void unaSolicitudSinRespuestaExpiraYLiberaElHorario() throws Exception {
        long reservaId = solicitarOk(cliente.token(), canchaId, fecha, "19:00", "20:00");
        long vigente = solicitarOk(nuevoClienteConSesion().token(), canchaId, fecha, "21:00", "22:00");
        jdbc.update("UPDATE reservas SET expira_en = now() - interval '1 minute' WHERE id = ?", reservaId);

        int expiradas = tareas.expirarSolicitudes();

        assertThat(expiradas).isGreaterThanOrEqualTo(1);
        assertThat(estadoDe(reservaId)).isEqualTo("EXPIRADA");
        assertThat(estadoDe(vigente)).as("las demás no se tocan").isEqualTo("PENDIENTE");
        assertThat(ocupaciones(reservaId)).isZero();
        assertThat(acciones(reservaId)).containsExactly("CREADA", "EXPIRADA");
        assertThat(jdbc.queryForObject("SELECT actor_rol FROM reserva_historial WHERE reserva_id = ? AND accion = 'EXPIRADA'", String.class, reservaId))
                .isEqualTo("SISTEMA");
        assertThat(notificaciones(cliente.id(), "RESERVA_EXPIRADA")).isEqualTo(1);
        esperarEstado(solicitar(nuevoClienteConSesion().token(), canchaId, fecha, "19:00", "20:00"), 201);
        assertThat(tareas.expirarSolicitudes()).as("es idempotente").isZero();
    }

    @Test
    void laReservaConfirmadaYJugadaSePasaAComplatadaUnaSolaVez() throws Exception {
        long reservaId = solicitarOk(cliente.token(), canchaId, fecha, "19:00", "20:00");
        long pendiente = solicitarOk(nuevoClienteConSesion().token(), canchaId, fecha, "21:00", "22:00");
        aprobar(admin, reservaId);
        mover(reservaId, Instant.now().minus(Duration.ofHours(3)), Instant.now().minus(Duration.ofHours(2)));

        assertThat(tareas.completarTerminadas()).isGreaterThanOrEqualTo(1);
        assertThat(estadoDe(reservaId)).isEqualTo("COMPLETADA");
        assertThat(estadoDe(pendiente)).isEqualTo("PENDIENTE");
        assertThat(acciones(reservaId)).containsExactly("CREADA", "APROBADA", "COMPLETADA");
        assertThat(tareas.completarTerminadas()).isZero();
    }

    @Test
    void elRecordatorioSeEnviaUnaSolaVezAlAcercarseLaReserva() throws Exception {
        long reservaId = solicitarOk(cliente.token(), canchaId, fecha, "19:00", "20:00");
        aprobar(admin, reservaId);
        assertThat(tareas.enviarRecordatorios()).as("falta mucho todavía").isZero();

        mover(reservaId, Instant.now().plus(Duration.ofMinutes(90)), Instant.now().plus(Duration.ofMinutes(150)));
        assertThat(tareas.enviarRecordatorios()).isGreaterThanOrEqualTo(1);
        assertThat(notificaciones(cliente.id(), "RESERVA_RECORDATORIO")).isEqualTo(1);
        tareas.enviarRecordatorios();
        assertThat(notificaciones(cliente.id(), "RESERVA_RECORDATORIO")).as("no se repite").isEqualTo(1);
    }

    // ===================== Efecto de otras acciones del admin =====================

    @Test
    void sacarUnaCanchaDeServicioConReservasFuturasExigeConfirmarYLasCancela() throws Exception {
        long reservaId = solicitarOk(cliente.token(), canchaId, fecha, "19:00", "20:00");
        aprobar(admin, reservaId);
        String base = "/api/v1/admin/canchas/" + canchaId + "/estado";

        MvcResult previa = postear(base + "/previsualizar", admin, Map.of("estado", "INACTIVA"));
        assertThat((Integer) leer(previa, "$.reservasFuturasActivas")).isEqualTo(1);
        assertThat((Boolean) leer(previa, "$.requiereConfirmacion")).isTrue();

        MvcResult sinConfirmar = postear(base, admin, Map.of("estado", "INACTIVA", "motivo", "Se retira la cancha"));
        esperarEstado(sinConfirmar, 409);
        assertThat((String) leer(sinConfirmar, "$.codigo")).isEqualTo("CONFLICTOS_SIN_CONFIRMAR");
        assertThat(estadoDe(reservaId)).isEqualTo("CONFIRMADA");

        esperarEstado(postear(base, admin, Map.of("estado", "INACTIVA", "motivo", "Se retira la cancha", "confirmarConflictos", true)), 200);
        assertThat(estadoDe(reservaId)).isEqualTo("CANCELADA");
        assertThat(ocupaciones(reservaId)).isZero();
        assertThat(jdbc.queryForObject("SELECT motivo_cancelacion FROM reservas WHERE id = ?", String.class, reservaId)).isEqualTo("Cancha en inactiva: Se retira la cancha");
        assertThat(jdbc.queryForObject("SELECT rol_cancelador FROM reservas WHERE id = ?", String.class, reservaId)).isEqualTo("ADMIN");
        assertThat(notificaciones(cliente.id(), "RESERVA_CANCELADA_ADMIN")).isEqualTo(1);
    }

    @Test
    void sacarDeServicioUnaComponenteAfectaLasReservasDeLaCanchaCompuesta() throws Exception {
        long b = crearCancha(admin, sedeId, "Cancha B");
        long f8 = crearCancha(admin, sedeId, "Cancha F8", 8, 160_000, List.of(canchaId, b));
        long reservaF8 = solicitarOk(cliente.token(), f8, fecha, "19:00", "20:00");

        MvcResult previa = postear("/api/v1/admin/canchas/" + canchaId + "/estado/previsualizar", admin, Map.of("estado", "MANTENIMIENTO"));
        assertThat((Integer) leer(previa, "$.reservasFuturasActivas")).as("la F8 usa la cancha A").isEqualTo(1);

        esperarEstado(postear("/api/v1/admin/canchas/" + canchaId + "/estado", admin,
                Map.of("estado", "MANTENIMIENTO", "motivo", "Cambio de grama", "confirmarConflictos", true)), 200);
        assertThat(estadoDe(reservaF8)).isEqualTo("CANCELADA");
        assertThat(ocupaciones(reservaF8)).isZero();
    }

    @Test
    void desactivarUnaSedeCancelaTodasSusReservasFuturasPreviaConfirmacion() throws Exception {
        long otra = crearCancha(admin, sedeId, "Cancha B");
        long r1 = solicitarOk(cliente.token(), canchaId, fecha, "19:00", "20:00");
        long r2 = solicitarOk(nuevoClienteConSesion().token(), otra, fecha, "19:00", "20:00");
        String base = "/api/v1/admin/sedes/" + sedeId + "/estado";

        assertThat((Integer) leer(postear(base + "/previsualizar", admin, Map.of("activa", false)), "$.reservasFuturasActivas")).isEqualTo(2);
        esperarEstado(postear(base, admin, Map.of("activa", false, "motivo", "Cierre por remodelación")), 409);
        esperarEstado(postear(base, admin, Map.of("activa", false, "motivo", "Cierre por remodelación", "confirmarConflictos", true)), 200);

        assertThat(estadoDe(r1)).isEqualTo("CANCELADA");
        assertThat(estadoDe(r2)).isEqualTo("CANCELADA");
    }

    @Test
    void desactivarUnClienteConReservasFuturasLasCancelaPreviaConfirmacion() throws Exception {
        long r1 = solicitarOk(cliente.token(), canchaId, fecha, "19:00", "20:00");
        String base = "/api/v1/admin/clientes/" + cliente.id();

        assertThat((Integer) leer(postear(base + "/desactivar/previsualizar", admin, Map.of()), "$.reservasFuturasActivas")).isEqualTo(1);
        esperarEstado(postear(base + "/desactivar", admin, Map.of("motivo", "Conducta indebida")), 409);
        assertThat(estadoDe(r1)).isEqualTo("PENDIENTE");

        esperarEstado(postear(base + "/desactivar", admin, Map.of("motivo", "Conducta indebida", "confirmarConflictos", true)), 200);
        assertThat(estadoDe(r1)).isEqualTo("CANCELADA");
        assertThat(ocupaciones(r1)).isZero();
        assertThat(jdbc.queryForObject("SELECT motivo_cancelacion FROM reservas WHERE id = ?", String.class, r1)).isEqualTo("Cuenta desactivada: Conducta indebida");
        esperarEstado(solicitar(nuevoClienteConSesion().token(), canchaId, fecha, "19:00", "20:00"), 201);
    }

    // ===================== Notificaciones =====================

    @Test
    void lasNotificacionesSeListanSeMarcanLeidasYSonPrivadas() throws Exception {
        long reservaId = solicitarOk(cliente.token(), canchaId, fecha, "19:00", "20:00");
        aprobar(admin, reservaId);
        Sesion otro = nuevoClienteConSesion();

        MvcResult contador = obtener("/api/v1/me/notificaciones/contador", cliente.token());
        assertThat(((Number) leer(contador, "$.noLeidas")).longValue()).isEqualTo(1);

        MvcResult lista = obtener("/api/v1/me/notificaciones", cliente.token());
        assertThat((String) leer(lista, "$.contenido[0].tipo")).isEqualTo("RESERVA_CONFIRMADA");
        assertThat((Boolean) leer(lista, "$.contenido[0].leida")).isFalse();
        assertThat((String) leer(lista, "$.contenido[0].entidad")).isEqualTo("reserva");
        assertThat(((Number) leer(lista, "$.contenido[0].entidadId")).longValue()).isEqualTo(reservaId);
        long notificacionId = ((Number) leer(lista, "$.contenido[0].id")).longValue();

        // otro usuario no puede leer ni marcar la de este cliente
        esperarEstado(postear("/api/v1/me/notificaciones/" + notificacionId + "/leer", otro.token(), Map.of()), 404);
        assertThat((List<?>) leer(obtener("/api/v1/me/notificaciones", otro.token()), "$.contenido")).isEmpty();

        esperarEstado(postear("/api/v1/me/notificaciones/" + notificacionId + "/leer", cliente.token(), Map.of()), 204);
        assertThat(((Number) leer(obtener("/api/v1/me/notificaciones/contador", cliente.token()), "$.noLeidas")).longValue()).isZero();
        assertThat((List<?>) leer(obtener("/api/v1/me/notificaciones?soloNoLeidas=true", cliente.token()), "$.contenido")).isEmpty();

        cancelar(cliente.token(), reservaId, null);
        esperarEstado(postear("/api/v1/me/notificaciones/leer-todas", admin, Map.of()), 204);
        assertThat(((Number) leer(obtener("/api/v1/me/notificaciones/contador", admin), "$.noLeidas")).longValue()).isZero();
    }
}
