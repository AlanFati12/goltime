package co.edu.usco.goltime.auditoria;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import co.edu.usco.goltime.compartido.dominio.ZonaHoraria;
import co.edu.usco.goltime.reservas.BaseReservasIT;

class AuditoriaIT extends BaseReservasIT {

    private Sesion admin;

    @BeforeEach
    void preparar() throws Exception {
        admin = ingresarAdmin();
    }

    private long sedeConNombre(String nombre) throws Exception {
        Map<String, Object> m = new HashMap<>();
        m.put("nombre", nombre);
        m.put("direccion", "Calle 1");
        m.put("ciudad", "Neiva");
        MvcResult r = postear("/api/v1/admin/sedes", admin.token(), m);
        esperarEstado(r, 201);
        return id(r);
    }

    @Test
    void laBitacoraMuestraQuienHizoQueConSuDetalleYSeFiltra() throws Exception {
        long sedeId = sedeConNombre("Sede Auditada " + UUID.randomUUID().toString().substring(0, 6));
        esperarEstado(postear("/api/v1/admin/sedes/" + sedeId + "/estado", admin.token(), Map.of("activa", false, "motivo", "Cierre temporal")), 200);

        MvcResult r = obtener("/api/v1/admin/auditoria?entidad=sedes&entidadId=" + sedeId, admin.token());

        esperarEstado(r, 200);
        assertThat((Integer) leer(r, "$.totalElementos")).isEqualTo(2);
        // de la más reciente a la más antigua
        assertThat((String) leer(r, "$.contenido[0].accion")).isEqualTo("DESACTIVAR_SEDE");
        assertThat((String) leer(r, "$.contenido[0].motivo")).isEqualTo("Cierre temporal");
        assertThat((String) leer(r, "$.contenido[0].actorNombre")).isEqualTo("Administrador Goltime");
        assertThat((String) leer(r, "$.contenido[0].actorRol")).isEqualTo("ADMIN");
        assertThat(((Number) leer(r, "$.contenido[0].actorId")).longValue()).isEqualTo(admin.id());
        assertThat((Boolean) leer(r, "$.contenido[0].cambios.activa.antes")).isTrue();
        assertThat((Boolean) leer(r, "$.contenido[0].cambios.activa.despues")).isFalse();
        assertThat((String) leer(r, "$.contenido[1].accion")).isEqualTo("CREAR_SEDE");
        assertThat((String) leer(r, "$.contenido[0].ip")).isNotBlank();

        assertThat((Integer) leer(obtener("/api/v1/admin/auditoria?entidad=sedes&entidadId=" + sedeId + "&accion=CREAR_SEDE", admin.token()), "$.totalElementos")).isEqualTo(1);
        assertThat((Integer) leer(obtener("/api/v1/admin/auditoria?entidadId=" + sedeId + "&entidad=sedes&actorId=" + admin.id(), admin.token()), "$.totalElementos")).isEqualTo(2);
        assertThat((Integer) leer(obtener("/api/v1/admin/auditoria?entidadId=" + sedeId + "&entidad=sedes&actorId=99999999", admin.token()), "$.totalElementos")).isZero();
    }

    @Test
    void filtraPorRangoDeFechasEnHoraDeBogota() throws Exception {
        long sedeId = sedeConNombre("Sede Fechas " + UUID.randomUUID().toString().substring(0, 6));
        LocalDate hoy = ZonaHoraria.fechaLocal(java.time.Instant.now());

        String base = "/api/v1/admin/auditoria?entidad=sedes&entidadId=" + sedeId;
        assertThat((Integer) leer(obtener(base + "&desde=" + hoy + "&hasta=" + hoy, admin.token()), "$.totalElementos")).isEqualTo(1);
        assertThat((Integer) leer(obtener(base + "&desde=" + hoy.plusDays(1), admin.token()), "$.totalElementos")).isZero();
        assertThat((Integer) leer(obtener(base + "&hasta=" + hoy.minusDays(1), admin.token()), "$.totalElementos")).isZero();
    }

    @Test
    void lasAccionesAutomaticasQuedanConElSistemaComoActor() throws Exception {
        // el administrador inicial se crea al arrancar, sin sesión: actor SISTEMA
        MvcResult r = obtener("/api/v1/admin/auditoria?accion=CREAR_ADMINISTRADOR&entidad=usuarios&tamano=100", admin.token());
        List<Object> roles = leer(r, "$.contenido[*].actorRol");
        assertThat(roles).contains("SISTEMA");
        assertThat((List<Object>) leer(r, "$.contenido[?(@.actorRol=='SISTEMA')].actorNombre")).isEmpty();
    }

    @Test
    void paginaYListaLosFiltrosDisponibles() throws Exception {
        for (int i = 0; i < 3; i++) {
            sedeConNombre("Sede Pagina " + UUID.randomUUID().toString().substring(0, 6));
        }
        MvcResult pagina = obtener("/api/v1/admin/auditoria?entidad=sedes&accion=CREAR_SEDE&tamano=2&pagina=0", admin.token());
        assertThat((List<?>) leer(pagina, "$.contenido")).hasSize(2);
        assertThat((Integer) leer(pagina, "$.totalPaginas")).isGreaterThanOrEqualTo(2);
        assertThat((Integer) leer(pagina, "$.tamano")).isEqualTo(2);

        MvcResult filtros = obtener("/api/v1/admin/auditoria/filtros", admin.token());
        assertThat((List<Object>) leer(filtros, "$.entidades")).contains("sedes", "usuarios");
        assertThat((List<Object>) leer(filtros, "$.acciones")).contains("CREAR_SEDE", "CREAR_ADMINISTRADOR");
    }

    @Test
    void soloElAdminConsultaLaAuditoria() throws Exception {
        Sesion cliente = nuevoClienteConSesion();
        esperarEstado(obtener("/api/v1/admin/auditoria", cliente.token()), 403);
        esperarEstado(obtener("/api/v1/admin/auditoria/filtros", cliente.token()), 403);
        esperarEstado(obtener("/api/v1/admin/auditoria", null), 401);
    }
}
