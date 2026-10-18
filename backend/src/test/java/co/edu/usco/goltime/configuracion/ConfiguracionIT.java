package co.edu.usco.goltime.configuracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import co.edu.usco.goltime.reservas.BaseReservasIT;

class ConfiguracionIT extends BaseReservasIT {

    private String admin;

    @BeforeEach
    void preparar() throws Exception {
        admin = ingresarAdmin().token();
    }

    @AfterEach
    void restaurar() {
        jdbc.update("UPDATE configuracion SET valor = '5' WHERE clave = 'max_reservas_activas_cliente'");
        jdbc.update("UPDATE configuracion SET valor = '5,6,7,8,9,11' WHERE clave = 'modalidades_permitidas'");
    }

    private MvcResult poner(String clave, String valor, String token) throws Exception {
        return enviarJson(put("/api/v1/admin/configuracion/" + clave), token, Map.of("valor", valor));
    }

    @Test
    void elAdminListaLosParametrosConSuRangoYSoloLosEditablesSePuedenCambiar() throws Exception {
        MvcResult r = obtener("/api/v1/admin/configuracion", admin);

        esperarEstado(r, 200);
        List<String> claves = leer(r, "$[*].clave");
        assertThat(claves).contains("max_reprogramaciones", "horas_minimas_cancelacion", "contacto_negocio").doesNotContain("programa_premios_activo", "precio_minimo_reserva");
        List<Number> maximos = leer(r, "$[?(@.clave=='max_reprogramaciones')].maximo");
        assertThat(maximos.get(0).intValue()).isEqualTo(10);
        esperarEstado(poner("clave_inventada", "1", admin), 404);
    }

    @Test
    void cambiarUnParametroAplicaDeInmediatoYQuedaAuditado() throws Exception {
        Sesion cliente = nuevoClienteConSesion();
        long sede = crearSede(admin);
        long cancha = crearCancha(admin, sede, "Cancha Cfg");
        esperarEstado(solicitar(cliente.token(), cancha, diaHabil(7), "19:00", "20:00"), 201);

        esperarEstado(poner("max_reservas_activas_cliente", "1", admin), 200);

        MvcResult segunda = solicitar(cliente.token(), cancha, diaHabil(9), "19:00", "20:00");
        esperarEstado(segunda, 422);
        assertThat((String) leer(segunda, "$.codigo")).isEqualTo("LIMITE_DE_RESERVAS");

        esperarEstado(poner("max_reservas_activas_cliente", "5", admin), 200);
        esperarEstado(solicitar(cliente.token(), cancha, diaHabil(9), "19:00", "20:00"), 201);

        MvcResult bitacora = obtener("/api/v1/admin/auditoria?accion=ACTUALIZAR_CONFIGURACION&entidad=configuracion&tamano=5", admin);
        assertThat((String) leer(bitacora, "$.contenido[0].cambios.max_reservas_activas_cliente.despues")).isEqualTo("5");
        assertThat((String) leer(bitacora, "$.contenido[0].cambios.max_reservas_activas_cliente.antes")).isEqualTo("1");
    }

    @Test
    void rechazaValoresInvalidosYNoCambiaNadaSiElValorEsElMismo() throws Exception {
        esperarEstado(poner("max_reprogramaciones", "50", admin), 400);
        esperarEstado(poner("max_reprogramaciones", "muchas", admin), 400);
        esperarEstado(poner("modalidades_permitidas", "5;6", admin), 400);
        Integer antes = jdbc.queryForObject("SELECT count(*) FROM audit_log WHERE accion = 'ACTUALIZAR_CONFIGURACION'", Integer.class);

        esperarEstado(poner("modalidades_permitidas", "5,6,7,8,9,11", admin), 200); // mismo valor

        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_log WHERE accion = 'ACTUALIZAR_CONFIGURACION'", Integer.class)).isEqualTo(antes);
    }

    @Test
    void soloElAdminLaVeYLaEdita() throws Exception {
        Sesion cliente = nuevoClienteConSesion();
        esperarEstado(obtener("/api/v1/admin/configuracion", cliente.token()), 403);
        esperarEstado(poner("max_reprogramaciones", "3", cliente.token()), 403);
        esperarEstado(obtener("/api/v1/admin/configuracion", null), 401);
    }
}
