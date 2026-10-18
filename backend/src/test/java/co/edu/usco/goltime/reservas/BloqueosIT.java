package co.edu.usco.goltime.reservas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class BloqueosIT extends BaseReservasIT {

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
        fecha = diaHabil(6);
    }

    // ----- Ayudantes -----

    private Map<String, Object> puntual(long sede, List<Long> canchas, LocalDate dia, String desde, String hasta, String huella) {
        Map<String, Object> m = new HashMap<>();
        m.put("sedeId", sede);
        m.put("canchaIds", canchas);
        m.put("tipo", "PUNTUAL");
        m.put("inicio", a(dia, desde).toString());
        m.put("fin", a(dia, hasta).toString());
        m.put("motivo", "MANTENIMIENTO");
        m.put("detalle", "cambio de grama");
        if (huella != null) {
            m.put("huellaConfirmada", huella);
        }
        return m;
    }

    private Map<String, Object> recurrente(long sede, List<Long> canchas, List<Integer> dias, String desde, String hasta, LocalDate d1, LocalDate d2) {
        Map<String, Object> m = new HashMap<>();
        m.put("sedeId", sede);
        m.put("canchaIds", canchas);
        m.put("tipo", "RECURRENTE");
        m.put("diasSemana", dias);
        m.put("horaInicio", desde);
        m.put("horaFin", hasta);
        m.put("fechaDesde", d1.toString());
        m.put("fechaHasta", d2.toString());
        m.put("motivo", "EVENTO_PRIVADO");
        return m;
    }

    private String huellaDe(Map<String, Object> cuerpo) throws Exception {
        return leer(postear("/api/v1/admin/bloqueos/previsualizar", admin, cuerpo), "$.huella");
    }

    private long crearBloqueo(Map<String, Object> cuerpo) throws Exception {
        MvcResult r = postear("/api/v1/admin/bloqueos", admin, cuerpo);
        esperarEstado(r, 201);
        return id(r);
    }

    private String estadoSlot(long sede, long cancha, LocalDate dia, String hora) throws Exception {
        MvcResult r = obtener("/api/v1/publico/disponibilidad?sedeId=" + sede + "&fecha=" + dia, null);
        List<Object> estados = leer(r, "$.canchas[?(@.canchaId==" + cancha + ")].slots[?(@.inicio=='" + a(dia, hora) + "')].estado");
        return (String) estados.get(0);
    }

    private int ocupacionesDeBloqueo(long bloqueoId) {
        return jdbc.queryForObject("SELECT count(*) FROM ocupaciones_cancha WHERE bloqueo_id = ?", Integer.class, bloqueoId);
    }

    private MvcResult modificar(String url, Object cuerpo) throws Exception {
        return mvc.perform(put(url).header("Authorization", "Bearer " + admin).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(cuerpo))).andReturn();
    }

    private String motivoCancelacion(long reservaId) {
        return jdbc.queryForObject("SELECT motivo_cancelacion FROM reservas WHERE id = ?", String.class, reservaId);
    }

    // ===================== Sin conflictos =====================

    @Test
    void unBloqueoSinConflictosOcupaLaSedeYBloqueaLaDisponibilidad() throws Exception {
        Map<String, Object> cuerpo = puntual(sedeId, List.of(), fecha, "17:00", "22:00", null);
        MvcResult previa = postear("/api/v1/admin/bloqueos/previsualizar", admin, cuerpo);
        esperarEstado(previa, 200);
        assertThat((List<?>) leer(previa, "$.conflictos")).isEmpty();
        assertThat((Boolean) leer(previa, "$.requiereConfirmacion")).isFalse();
        assertThat((Integer) leer(previa, "$.canchasAfectadas")).isEqualTo(1);

        MvcResult r = postear("/api/v1/admin/bloqueos", admin, cuerpo);
        esperarEstado(r, 201);
        long bloqueoId = id(r);
        assertThat((String) leer(r, "$.motivoTexto")).isEqualTo("Mantenimiento: cambio de grama");
        assertThat((Boolean) leer(r, "$.activo")).isTrue();
        assertThat(ocupacionesDeBloqueo(bloqueoId)).isEqualTo(1);

        assertThat(estadoSlot(sedeId, canchaId, fecha, "19:00")).isEqualTo("BLOQUEADO");
        assertThat(estadoSlot(sedeId, canchaId, fecha, "16:00")).isEqualTo("DISPONIBLE");
        assertThat(estadoSlot(sedeId, canchaId, fecha, "22:00")).isEqualTo("DISPONIBLE");
        MvcResult choque = solicitar(cliente.token(), canchaId, fecha, "19:00", "20:00");
        esperarEstado(choque, 409);
        assertThat((String) leer(choque, "$.codigo")).isEqualTo("RESERVA_SOLAPADA");

        assertThat(jdbc.queryForObject("SELECT motivo FROM audit_log WHERE accion = 'CREAR_BLOQUEO' AND entidad_id = ?", String.class, bloqueoId))
                .isEqualTo("Mantenimiento: cambio de grama");

        // eliminar libera las canchas y queda registrado
        esperarEstado(mvc.perform(delete("/api/v1/admin/bloqueos/" + bloqueoId).header("Authorization", "Bearer " + admin)).andReturn(), 204);
        assertThat(estadoSlot(sedeId, canchaId, fecha, "19:00")).isEqualTo("DISPONIBLE");
        assertThat(ocupacionesDeBloqueo(bloqueoId)).isZero();
        esperarEstado(mvc.perform(delete("/api/v1/admin/bloqueos/" + bloqueoId).header("Authorization", "Bearer " + admin)).andReturn(), 204);
        assertThat((Boolean) leer(obtener("/api/v1/admin/bloqueos/" + bloqueoId, admin), "$.activo")).isFalse();
        assertThat((List<?>) leer(obtener("/api/v1/admin/bloqueos?sedeId=" + sedeId, admin), "$")).isEmpty();
        assertThat((List<?>) leer(obtener("/api/v1/admin/bloqueos?sedeId=" + sedeId + "&soloActivos=false", admin), "$")).hasSize(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_log WHERE accion = 'ELIMINAR_BLOQUEO' AND entidad_id = ?", Integer.class, bloqueoId)).isEqualTo(1);
        esperarEstado(solicitar(cliente.token(), canchaId, fecha, "19:00", "20:00"), 201);
    }

    @Test
    void unBloqueoDeTodoElDiaCubreLosDiasCompletosEnHoraDeBogota() throws Exception {
        Map<String, Object> m = puntual(sedeId, List.of(), fecha, "15:00", "10:00", null);
        m.put("fin", a(fecha.plusDays(1), "09:00").toString());
        m.put("todoElDia", true);
        crearBloqueo(m);

        assertThat(estadoSlot(sedeId, canchaId, fecha, "08:00")).isEqualTo("BLOQUEADO");
        assertThat(estadoSlot(sedeId, canchaId, fecha, "19:00")).isEqualTo("BLOQUEADO");
        assertThat(estadoSlot(sedeId, canchaId, fecha.plusDays(1), "08:00")).isEqualTo("BLOQUEADO");
        assertThat(estadoSlot(sedeId, canchaId, fecha.plusDays(1), "19:00")).isEqualTo("BLOQUEADO");
        assertThat(estadoSlot(sedeId, canchaId, fecha.plusDays(2), "08:00")).isEqualTo("DISPONIBLE");
        assertThat(estadoSlot(sedeId, canchaId, fecha.minusDays(1), "19:00")).isEqualTo("DISPONIBLE");
    }

    @Test
    void sePuedeBloquearSoloAlgunasCanchasDeLaSede() throws Exception {
        long b = crearCancha(admin, sedeId, "Cancha B");
        crearBloqueo(puntual(sedeId, List.of(canchaId), fecha, "17:00", "20:00", null));

        assertThat(estadoSlot(sedeId, canchaId, fecha, "18:00")).isEqualTo("BLOQUEADO");
        assertThat(estadoSlot(sedeId, b, fecha, "18:00")).isEqualTo("DISPONIBLE");
        esperarEstado(solicitar(cliente.token(), b, fecha, "18:00", "19:00"), 201);
    }

    // ===================== Conflictos =====================

    @Test
    void conReservasAfectadasExigeConfirmarLaListaYLasCancelaEnUnaSolaTransaccion() throws Exception {
        Sesion otro = nuevoClienteConSesion();
        long pendiente = solicitarOk(cliente.token(), canchaId, fecha, "18:00", "19:00");
        long pagada = solicitarOk(otro.token(), canchaId, fecha, "20:00", "21:00");
        aprobar(admin, pagada);
        esperarEstado(postear("/api/v1/admin/reservas/" + pagada + "/pagos", admin, Map.of("monto", 80_000, "metodo", "EFECTIVO")), 201);
        long fuera = solicitarOk(cliente.token(), canchaId, fecha, "22:00", "23:00");

        Map<String, Object> cuerpo = puntual(sedeId, List.of(), fecha, "17:00", "21:00", null);
        MvcResult previa = postear("/api/v1/admin/bloqueos/previsualizar", admin, cuerpo);
        esperarEstado(previa, 200);
        assertThat((Boolean) leer(previa, "$.requiereConfirmacion")).isTrue();
        assertThat((List<?>) leer(previa, "$.conflictos")).hasSize(2);
        assertThat(((Number) leer(previa, "$.conflictos[0].reservaId")).longValue()).isEqualTo(pendiente);
        assertThat((String) leer(previa, "$.conflictos[0].cliente")).isEqualTo("Ana Pérez");
        assertThat((String) leer(previa, "$.conflictos[0].cancha")).isEqualTo("Cancha A");
        assertThat((Integer) leer(previa, "$.conflictos[0].precioFinal")).isEqualTo(80_000);
        assertThat((Boolean) leer(previa, "$.conflictos[0].conPago")).isFalse();
        assertThat((Boolean) leer(previa, "$.conflictos[1].conPago")).isTrue();
        String huella = leer(previa, "$.huella");
        assertThat(estadoDe(pendiente)).as("previsualizar no cambia nada").isEqualTo("PENDIENTE");

        // sin confirmar o con una huella inventada: 409 y nada cambia
        MvcResult sinHuella = postear("/api/v1/admin/bloqueos", admin, cuerpo);
        esperarEstado(sinHuella, 409);
        assertThat((String) leer(sinHuella, "$.codigo")).isEqualTo("CONFLICTOS_SIN_CONFIRMAR");
        esperarEstado(postear("/api/v1/admin/bloqueos", admin, puntual(sedeId, List.of(), fecha, "17:00", "21:00", "0000000000000000")), 409);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bloqueos WHERE sede_id = ?", Integer.class, sedeId)).isZero();
        assertThat(estadoDe(pendiente)).isEqualTo("PENDIENTE");
        assertThat(estadoDe(pagada)).isEqualTo("CONFIRMADA");

        // confirmando la lista exacta: se crea el bloqueo y se cancelan las dos reservas
        long bloqueoId = crearBloqueo(puntual(sedeId, List.of(), fecha, "17:00", "21:00", huella));
        String motivo = "Bloqueo de horario: Mantenimiento: cambio de grama";
        assertThat(estadoDe(pendiente)).isEqualTo("CANCELADA");
        assertThat(estadoDe(pagada)).isEqualTo("CANCELADA");
        assertThat(estadoDe(fuera)).as("la que no se cruza queda intacta").isEqualTo("PENDIENTE");
        assertThat(motivoCancelacion(pendiente)).isEqualTo(motivo);
        assertThat(jdbc.queryForObject("SELECT rol_cancelador FROM reservas WHERE id = ?", String.class, pendiente)).isEqualTo("ADMIN");
        assertThat(jdbc.queryForObject("SELECT estado_pago FROM reservas WHERE id = ?", String.class, pagada)).isEqualTo("REEMBOLSO_PENDIENTE");
        assertThat(jdbc.queryForObject("SELECT estado_pago FROM reservas WHERE id = ?", String.class, pendiente)).isEqualTo("PENDIENTE");
        assertThat(ocupaciones(pendiente)).isZero();
        assertThat(ocupacionesDeBloqueo(bloqueoId)).isEqualTo(1);
        assertThat(acciones(pendiente)).containsExactly("CREADA", "CANCELADA");
        assertThat(notificaciones(cliente.id(), "RESERVA_CANCELADA_ADMIN")).isEqualTo(1);
        assertThat(notificaciones(otro.id(), "RESERVA_CANCELADA_ADMIN")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT cambios->>'reservasCanceladas' FROM audit_log WHERE accion = 'CREAR_BLOQUEO' AND entidad_id = ?", String.class, bloqueoId))
                .isEqualTo("2");
        // los horarios quedaron bloqueados
        assertThat(estadoSlot(sedeId, canchaId, fecha, "18:00")).isEqualTo("BLOQUEADO");
    }

    @Test
    void siLasReservasAfectadasCambianDespuesDeRevisarlasSePideConfirmarDeNuevo() throws Exception {
        long primera = solicitarOk(cliente.token(), canchaId, fecha, "18:00", "19:00");
        Map<String, Object> cuerpo = puntual(sedeId, List.of(), fecha, "17:00", "21:00", null);
        String huellaVieja = huellaDe(cuerpo);

        // mientras el admin revisaba la lista, entró una reserva nueva en ese rango
        Sesion otro = nuevoClienteConSesion();
        long nueva = solicitarOk(otro.token(), canchaId, fecha, "19:00", "20:00");

        MvcResult r = postear("/api/v1/admin/bloqueos", admin, puntual(sedeId, List.of(), fecha, "17:00", "21:00", huellaVieja));
        esperarEstado(r, 409);
        assertThat((String) leer(r, "$.codigo")).isEqualTo("CONFLICTOS_SIN_CONFIRMAR");
        assertThat((String) leer(r, "$.detail")).contains("cambiaron");
        assertThat(estadoDe(primera)).isEqualTo("PENDIENTE");
        assertThat(estadoDe(nueva)).as("no se canceló una reserva que el admin no vio").isEqualTo("PENDIENTE");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bloqueos WHERE sede_id = ?", Integer.class, sedeId)).isZero();

        // con la lista actualizada ya se puede confirmar
        MvcResult nuevaPrevia = postear("/api/v1/admin/bloqueos/previsualizar", admin, cuerpo);
        assertThat((List<?>) leer(nuevaPrevia, "$.conflictos")).hasSize(2);
        crearBloqueo(puntual(sedeId, List.of(), fecha, "17:00", "21:00", leer(nuevaPrevia, "$.huella")));
        assertThat(estadoDe(primera)).isEqualTo("CANCELADA");
        assertThat(estadoDe(nueva)).isEqualTo("CANCELADA");
    }

    @Test
    void siElBloqueoChocaConOtroSeRevierteTodoIncluidasLasCancelaciones() throws Exception {
        crearBloqueo(puntual(sedeId, List.of(), fecha, "20:00", "22:00", null));
        long reserva = solicitarOk(cliente.token(), canchaId, fecha, "22:00", "23:00");

        // [21:00, 23:00) toca la reserva de las 22:00 y además se solapa con el bloqueo existente de 20:00-22:00
        Map<String, Object> cuerpo = puntual(sedeId, List.of(), fecha, "21:00", "23:00", null);
        String huella = huellaDe(cuerpo);
        MvcResult r = postear("/api/v1/admin/bloqueos", admin, puntual(sedeId, List.of(), fecha, "21:00", "23:00", huella));

        esperarEstado(r, 409);
        assertThat((String) leer(r, "$.codigo")).isEqualTo("BLOQUEO_SOLAPADO");
        assertThat(estadoDe(reserva)).as("la cancelación se revirtió con todo lo demás").isEqualTo("PENDIENTE");
        assertThat(ocupaciones(reserva)).isEqualTo(1);
        assertThat(acciones(reserva)).containsExactly("CREADA");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bloqueos WHERE sede_id = ?", Integer.class, sedeId)).isEqualTo(1);
        assertThat(notificaciones(cliente.id(), "RESERVA_CANCELADA_ADMIN")).isZero();
    }

    @Test
    void bloquearUnaComponenteAfectaLasReservasDeLaCompuestaYAlReves() throws Exception {
        long b = crearCancha(admin, sedeId, "Cancha B");
        long f8 = crearCancha(admin, sedeId, "Cancha F8", 8, 160_000, List.of(canchaId, b));
        long reservaF8 = solicitarOk(cliente.token(), f8, fecha, "19:00", "20:00");
        long reservaB = solicitarOk(nuevoClienteConSesion().token(), b, fecha, "21:00", "22:00");

        // bloquear la F5 A toca la reserva de la F8 (que ocupa A), no la de B
        MvcResult previaA = postear("/api/v1/admin/bloqueos/previsualizar", admin, puntual(sedeId, List.of(canchaId), fecha, "18:00", "23:00", null));
        assertThat((List<Object>) leer(previaA, "$.conflictos[*].reservaId")).extracting(o -> ((Number) o).longValue()).containsExactly(reservaF8);

        // bloquear la F8 ocupa también A y B, así que toca la reserva de B
        MvcResult previaF8 = postear("/api/v1/admin/bloqueos/previsualizar", admin, puntual(sedeId, List.of(f8), fecha, "20:30", "22:30", null));
        assertThat((List<Object>) leer(previaF8, "$.conflictos[*].reservaId")).extracting(o -> ((Number) o).longValue()).containsExactly(reservaB);
        assertThat((Integer) leer(previaF8, "$.canchasAfectadas")).isEqualTo(3);

        long bloqueoId = crearBloqueo(puntual(sedeId, List.of(f8), fecha, "20:30", "22:30", leer(previaF8, "$.huella")));
        assertThat(estadoDe(reservaB)).isEqualTo("CANCELADA");
        assertThat(estadoDe(reservaF8)).isEqualTo("PENDIENTE");
        assertThat(ocupacionesDeBloqueo(bloqueoId)).isEqualTo(3);
        assertThat(estadoSlot(sedeId, canchaId, fecha, "21:00")).isEqualTo("BLOQUEADO");
        assertThat(estadoSlot(sedeId, b, fecha, "21:00")).isEqualTo("BLOQUEADO");
    }

    // ===================== Recurrentes =====================

    @Test
    void unBloqueoRecurrenteBloqueaCadaDiaQueCoincideDentroDelRango() throws Exception {
        long b = crearCancha(admin, sedeId, "Cancha B");
        int dia = fecha.getDayOfWeek().getValue();
        Map<String, Object> cuerpo = recurrente(sedeId, List.of(), List.of(dia), "18:00", "20:00", fecha, fecha.plusDays(14));

        MvcResult previa = postear("/api/v1/admin/bloqueos/previsualizar", admin, cuerpo);
        assertThat((Integer) leer(previa, "$.intervalos")).isEqualTo(3);
        long bloqueoId = crearBloqueo(cuerpo);

        assertThat(ocupacionesDeBloqueo(bloqueoId)).as("3 días x 2 canchas").isEqualTo(6);
        for (LocalDate d : List.of(fecha, fecha.plusDays(7), fecha.plusDays(14))) {
            assertThat(estadoSlot(sedeId, canchaId, d, "18:00")).isEqualTo("BLOQUEADO");
            assertThat(estadoSlot(sedeId, b, d, "19:00")).isEqualTo("BLOQUEADO");
            assertThat(estadoSlot(sedeId, canchaId, d, "20:00")).isEqualTo("DISPONIBLE");
        }
        assertThat(estadoSlot(sedeId, canchaId, fecha.plusDays(1), "18:00")).isEqualTo("DISPONIBLE");
        assertThat(estadoSlot(sedeId, canchaId, fecha.plusDays(21), "18:00")).isEqualTo("DISPONIBLE");

        MvcResult detalle = obtener("/api/v1/admin/bloqueos/" + bloqueoId, admin);
        assertThat((String) leer(detalle, "$.tipo")).isEqualTo("RECURRENTE");
        assertThat((String) leer(detalle, "$.horaInicio")).isEqualTo("18:00");
        assertThat((List<Object>) leer(detalle, "$.diasSemana")).containsExactly(dia);
    }

    // ===================== Editar =====================

    @Test
    void editarUnBloqueoLiberaSuOcupacionAnteriorYAplicaLaNuevaConElMismoFlujoDeConflictos() throws Exception {
        long bloqueoId = crearBloqueo(puntual(sedeId, List.of(), fecha, "17:00", "19:00", null));
        long reserva = solicitarOk(cliente.token(), canchaId, fecha, "21:00", "22:00");

        // mover el bloqueo a un horario que toca una reserva exige confirmar
        Map<String, Object> editado = puntual(sedeId, List.of(), fecha, "20:00", "22:30", null);
        MvcResult sinConfirmar = modificar("/api/v1/admin/bloqueos/" + bloqueoId, editado);
        esperarEstado(sinConfirmar, 409);
        assertThat(estadoSlot(sedeId, canchaId, fecha, "17:00")).as("sigue el bloqueo original").isEqualTo("BLOQUEADO");

        String huella = huellaDe(editado);
        MvcResult r = modificar("/api/v1/admin/bloqueos/" + bloqueoId, puntual(sedeId, List.of(), fecha, "20:00", "22:30", huella));
        esperarEstado(r, 200);
        assertThat(estadoDe(reserva)).isEqualTo("CANCELADA");
        assertThat(estadoSlot(sedeId, canchaId, fecha, "17:00")).as("el horario viejo quedó libre").isEqualTo("DISPONIBLE");
        assertThat(estadoSlot(sedeId, canchaId, fecha, "21:00")).isEqualTo("BLOQUEADO");
        assertThat(ocupacionesDeBloqueo(bloqueoId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_log WHERE accion = 'ACTUALIZAR_BLOQUEO' AND entidad_id = ?", Integer.class, bloqueoId)).isEqualTo(1);
    }

    @Test
    void noSeEditaUnBloqueoEliminadoInexistenteNiSeCambiaDeSede() throws Exception {
        long bloqueoId = crearBloqueo(puntual(sedeId, List.of(), fecha, "17:00", "19:00", null));
        long otraSede = crearSede(admin);
        crearCancha(admin, otraSede, "Cancha X");

        esperarEstado(modificar("/api/v1/admin/bloqueos/" + bloqueoId, puntual(otraSede, List.of(), fecha, "17:00", "19:00", null)), 400);
        esperarEstado(modificar("/api/v1/admin/bloqueos/987654321", puntual(sedeId, List.of(), fecha, "17:00", "19:00", null)), 404);
        esperarEstado(mvc.perform(delete("/api/v1/admin/bloqueos/" + bloqueoId).header("Authorization", "Bearer " + admin)).andReturn(), 204);
        esperarEstado(modificar("/api/v1/admin/bloqueos/" + bloqueoId, puntual(sedeId, List.of(), fecha, "17:00", "19:00", null)), 409);
        esperarEstado(obtener("/api/v1/admin/bloqueos/987654321", admin), 404);
    }

    // ===================== Validaciones y permisos =====================

    @Test
    void validaLosDatosDelBloqueo() throws Exception {
        Map<String, Object> sinMotivo = puntual(sedeId, List.of(), fecha, "17:00", "19:00", null);
        sinMotivo.remove("motivo");
        esperarEstado(postear("/api/v1/admin/bloqueos", admin, sinMotivo), 400);

        esperarEstado(postear("/api/v1/admin/bloqueos", admin, puntual(sedeId, List.of(), fecha, "19:00", "17:00", null)), 400);
        esperarEstado(postear("/api/v1/admin/bloqueos", admin, puntual(sedeId, List.of(), enDias(-3), "17:00", "19:00", null)), 400);

        long otraSede = crearSede(admin);
        long ajena = crearCancha(admin, otraSede, "Cancha ajena");
        esperarEstado(postear("/api/v1/admin/bloqueos", admin, puntual(sedeId, List.of(ajena), fecha, "17:00", "19:00", null)), 400);
        esperarEstado(postear("/api/v1/admin/bloqueos", admin, puntual(987654321L, List.of(), fecha, "17:00", "19:00", null)), 400);

        esperarEstado(postear("/api/v1/admin/bloqueos", admin, recurrente(sedeId, List.of(), List.of(), "18:00", "20:00", fecha, fecha.plusDays(7))), 400);
        esperarEstado(postear("/api/v1/admin/bloqueos", admin, recurrente(sedeId, List.of(), List.of(2), "18:00", "20:00", fecha, fecha.plusDays(400))), 400);
        esperarEstado(postear("/api/v1/admin/bloqueos", admin, recurrente(sedeId, List.of(), List.of(2), "20:00", "18:00", fecha, fecha.plusDays(7))), 400);
        esperarEstado(postear("/api/v1/admin/bloqueos", admin, recurrente(sedeId, List.of(), List.of(9), "18:00", "20:00", fecha, fecha.plusDays(7))), 400);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bloqueos WHERE sede_id = ?", Integer.class, sedeId)).isZero();
    }

    @Test
    void soloElAdminGestionaBloqueos() throws Exception {
        Map<String, Object> cuerpo = puntual(sedeId, List.of(), fecha, "17:00", "19:00", null);
        esperarEstado(postear("/api/v1/admin/bloqueos", cliente.token(), cuerpo), 403);
        esperarEstado(postear("/api/v1/admin/bloqueos/previsualizar", cliente.token(), cuerpo), 403);
        esperarEstado(obtener("/api/v1/admin/bloqueos", cliente.token()), 403);
        esperarEstado(postear("/api/v1/admin/bloqueos", null, cuerpo), 401);
        long id = crearBloqueo(cuerpo);
        esperarEstado(mvc.perform(delete("/api/v1/admin/bloqueos/" + id).header("Authorization", "Bearer " + cliente.token())).andReturn(), 403);
        assertThat((Boolean) leer(obtener("/api/v1/admin/bloqueos/" + id, admin), "$.activo")).isTrue();
        assertThat(Instant.now()).isNotNull();
    }
}
