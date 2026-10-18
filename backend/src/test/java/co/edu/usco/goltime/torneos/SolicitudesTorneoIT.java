package co.edu.usco.goltime.torneos;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

class SolicitudesTorneoIT extends BaseTorneosIT {

    private Sesion cliente;
    private long torneoId;

    @BeforeEach
    void preparar() throws Exception {
        cliente = nuevoClienteConSesion();
        habilitar(cliente, true);
        Map<String, Object> cuerpo = cuerpoTorneo("LIGA");
        cuerpo.put("cupoEquipos", 3);
        torneoId = crearTorneo(cuerpo);
        esperarEstado(estado(torneoId, "INSCRIPCIONES", null), 200);
    }

    private void habilitar(Sesion c, boolean valor) {
        jdbc.update("UPDATE usuarios SET puede_inscribir_equipos = ? WHERE id = ?", valor, c.id());
    }

    private Map<String, Object> solicitud(String equipo, int jugadores) {
        List<Map<String, Object>> lista = new ArrayList<>();
        for (int i = 1; i <= jugadores; i++) {
            Map<String, Object> j = new HashMap<>();
            j.put("nombres", "Jugador" + i);
            j.put("apellidos", equipo.replace(' ', '_'));
            j.put("documento", documentoUnico());
            j.put("dorsal", i);
            lista.add(j);
        }
        Map<String, Object> m = new HashMap<>();
        m.put("nombreEquipo", equipo);
        m.put("color", "#445566");
        m.put("jugadores", lista);
        return m;
    }

    private MvcResult solicitar(Sesion c, String equipo, int jugadores) throws Exception {
        return postear("/api/v1/torneos/" + torneoId + "/solicitudes", c.token(), solicitud(equipo, jugadores));
    }

    private long idDe(MvcResult r) throws Exception {
        return ((Number) leer(r, "$.inscripcionId")).longValue();
    }

    @Test
    void soloUnClienteHabilitadoPuedeSolicitarYQuedaPendiente() throws Exception {
        Sesion sinPermiso = nuevoClienteConSesion();
        MvcResult info = obtener("/api/v1/me/solicitudes-torneo", sinPermiso.token());
        assertThat((Boolean) leer(info, "$.habilitado")).isFalse();
        esperarEstado(solicitar(sinPermiso, nombre("Sin permiso"), 2), 403);
        esperarEstado(postear("/api/v1/torneos/" + torneoId + "/solicitudes", null, solicitud("Anónimo", 2)), 401);

        MvcResult r = solicitar(cliente, nombre("Los Aspirantes"), 2);

        esperarEstado(r, 201);
        assertThat((String) leer(r, "$.estado")).isEqualTo("SOLICITADA");
        long inscripcionId = idDe(r);
        assertThat((Boolean) leer(obtener("/api/v1/me/solicitudes-torneo", cliente.token()), "$.habilitado")).isTrue();
        assertThat((List<?>) leer(obtener("/api/v1/me/solicitudes-torneo", cliente.token()), "$.solicitudes")).hasSize(1);
        // el equipo queda a cargo del cliente y el torneo no lo cuenta aún
        assertThat(jdbc.queryForObject("SELECT usuario_responsable_id FROM equipos e JOIN inscripciones_torneo i ON i.equipo_id = e.id WHERE i.id = ?", Long.class, inscripcionId))
                .isEqualTo(cliente.id());
        MvcResult admin = obtener("/api/v1/admin/torneos/" + torneoId, this.admin);
        assertThat((List<?>) leer(admin, "$.inscripciones")).isEmpty();
        assertThat((List<?>) leer(admin, "$.solicitudes")).hasSize(1);
        assertThat((List<?>) leer(obtener("/api/v1/publico/torneos/" + torneoId, null), "$.solicitudes")).isEmpty();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notificaciones WHERE tipo = 'INSCRIPCION_SOLICITADA' AND entidad_id = ?", Integer.class, torneoId)).isPositive();
    }

    @Test
    void validaLaPlantillaElNombreYLosDocumentosYLaFaseDelTorneo() throws Exception {
        esperarEstado(solicitar(cliente, nombre("Incompleto"), 1), 400); // el torneo pide 2 jugadores

        String nombreEquipo = nombre("Repetido");
        esperarEstado(solicitar(cliente, nombreEquipo, 2), 201);
        esperarEstado(solicitar(cliente, nombreEquipo, 2), 409); // mismo nombre de equipo

        Map<String, Object> conDocumentoRepetido = solicitud(nombre("Doc repetido"), 2);
        String documento = jdbc.queryForObject("SELECT documento FROM jugadores ORDER BY id DESC LIMIT 1", String.class);
        ((List<Map<String, Object>>) conDocumentoRepetido.get("jugadores")).get(0).put("documento", documento);
        MvcResult dup = postear("/api/v1/torneos/" + torneoId + "/solicitudes", cliente.token(), conDocumentoRepetido);
        esperarEstado(dup, 409);
        // la solicitud fallida no dejó equipos a medias
        assertThat(jdbc.queryForObject("SELECT count(*) FROM equipos WHERE lower(nombre) LIKE '%doc repetido%'", Integer.class)).isZero();

        // un torneo en borrador no existe para el cliente y uno con fixture ya no recibe solicitudes
        long borrador = crearTorneo(cuerpoTorneo("LIGA"));
        esperarEstado(postear("/api/v1/torneos/" + borrador + "/solicitudes", cliente.token(), solicitud(nombre("Borrador"), 2)), 404);
        for (int i = 0; i < 2; i++) {
            inscribir(torneoId, crearEquipo(2), i + 1);
        }
        generarFixture(torneoId);
        esperarEstado(solicitar(cliente, nombre("Tarde"), 2), 409);
    }

    @Test
    void elAdminApruebaYElEquipoEntraAlTorneoRespetandoElCupo() throws Exception {
        long primera = idDe(solicitar(cliente, nombre("Aprobada"), 2));
        Sesion otro = nuevoClienteConSesion();
        habilitar(otro, true);
        long segunda = idDe(solicitar(otro, nombre("Cupo lleno"), 2));
        inscribir(torneoId, crearEquipo(2), 1);
        inscribir(torneoId, crearEquipo(2), 2); // ya hay 2 de 3

        MvcResult ok = postear("/api/v1/admin/torneos/" + torneoId + "/solicitudes/" + primera + "/aprobar", admin, Map.of());
        esperarEstado(ok, 200);
        assertThat(notificaciones(cliente.id(), "INSCRIPCION_APROBADA")).isEqualTo(1);
        esperarEstado(postear("/api/v1/admin/torneos/" + torneoId + "/solicitudes/" + primera + "/aprobar", admin, Map.of()), 409); // ya resuelta
        MvcResult lleno = postear("/api/v1/admin/torneos/" + torneoId + "/solicitudes/" + segunda + "/aprobar", admin, Map.of());
        esperarEstado(lleno, 409);
        assertThat((String) leer(lleno, "$.detail")).contains("cupos");

        MvcResult detalle = obtener("/api/v1/admin/torneos/" + torneoId, admin);
        assertThat((List<?>) leer(detalle, "$.inscripciones")).hasSize(3);
        assertThat((List<?>) leer(detalle, "$.solicitudes")).hasSize(1);
        esperarEstado(postear("/api/v1/admin/torneos/" + torneoId + "/solicitudes/" + primera + "/aprobar", cliente.token(), Map.of()), 403);
    }

    @Test
    void rechazarLiberaALosJugadoresYElClienteVeElMotivoYPuedeRetirarLaSuya() throws Exception {
        long rechazada = idDe(solicitar(cliente, nombre("Rechazada"), 2));
        Sesion otro = nuevoClienteConSesion();
        habilitar(otro, true);
        long retirable = idDe(solicitar(otro, nombre("Retirada"), 2));

        esperarEstado(postear("/api/v1/admin/torneos/" + torneoId + "/solicitudes/" + rechazada + "/rechazar", admin, Map.of("motivo", " ")), 400);
        esperarEstado(postear("/api/v1/admin/torneos/" + torneoId + "/solicitudes/" + rechazada + "/rechazar", admin, Map.of("motivo", "Faltan documentos")), 204);

        assertThat(notificaciones(cliente.id(), "INSCRIPCION_RECHAZADA")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM plantillas_torneo WHERE inscripcion_id = ?", Integer.class, rechazada)).isZero();
        List<String> estados = leer(obtener("/api/v1/me/solicitudes-torneo", cliente.token()), "$.solicitudes[*].estado");
        assertThat(estados).containsExactly("RECHAZADA");

        esperarEstado(delete("/api/v1/me/solicitudes-torneo/" + retirable, cliente.token()), 404); // ajena
        esperarEstado(delete("/api/v1/me/solicitudes-torneo/" + retirable, otro.token()), 204);
        assertThat(jdbc.queryForObject("SELECT estado FROM inscripciones_torneo WHERE id = ?", String.class, retirable)).isEqualTo("RETIRADA");
        esperarEstado(delete("/api/v1/me/solicitudes-torneo/" + retirable, otro.token()), 409); // ya no está pendiente
    }

    @Test
    void elAdminVeElEquipoSuResponsableYSuPlantillaAntesDeResolver() throws Exception {
        String equipo = nombre("Los Revisados");
        long inscripcionId = idDe(solicitar(cliente, equipo, 3));

        MvcResult r = obtener("/api/v1/admin/torneos/" + torneoId + "/solicitudes/" + inscripcionId, admin);

        esperarEstado(r, 200);
        assertThat((String) leer(r, "$.estado")).isEqualTo("SOLICITADA");
        assertThat((String) leer(r, "$.equipo.nombre")).isEqualTo(equipo);
        assertThat(((Number) leer(r, "$.responsable.id")).longValue()).isEqualTo(cliente.id());
        assertThat((String) leer(r, "$.responsable.correo")).contains("@");
        assertThat((List<?>) leer(r, "$.jugadores")).hasSize(3);
        // un cliente no puede verla por la ruta de administración
        esperarEstado(obtener("/api/v1/admin/torneos/" + torneoId + "/solicitudes/" + inscripcionId, cliente.token()), 403);
    }

    private MvcResult delete(String url, String token) throws Exception {
        return borrar(url, token);
    }
}
