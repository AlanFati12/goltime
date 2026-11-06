package co.edu.usco.goltime.usuarios;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.jayway.jsonpath.JsonPath;

import co.edu.usco.goltime.BaseIntegracion;

class GestionClientesIT extends BaseIntegracion {

    private MvcResult patch(String url, Object cuerpo, String token) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.patch(url).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(cuerpo))).andReturn();
    }

    private String codigo(MvcResult r) throws Exception {
        return JsonPath.read(r.getResponse().getContentAsString(), "$.codigo");
    }

    @Test
    void elAdministradorInicialSeCreaDesdeLaConfiguracion() throws Exception {
        Sesion admin = ingresarAdmin();
        MvcResult me = consultar("/api/v1/me", admin.token());
        assertThat((String) JsonPath.read(me.getResponse().getContentAsString(), "$.rol")).isEqualTo("ADMIN");
    }

    @Test
    void listaClientesConBusquedaYFiltroDeEstado() throws Exception {
        Sesion admin = ingresarAdmin();
        String correo = correoUnico();
        entrar(correo, false);

        MvcResult porCorreo = consultar("/api/v1/admin/clientes?busqueda=" + correo.substring(0, 16), admin.token());
        esperarEstado(porCorreo, 200);
        String cuerpo = porCorreo.getResponse().getContentAsString();
        assertThat((Integer) JsonPath.read(cuerpo, "$.totalElementos")).isEqualTo(1);
        assertThat((String) JsonPath.read(cuerpo, "$.contenido[0].correo")).isEqualTo(correo);
        assertThat(cuerpo).doesNotContain("claveHash");

        MvcResult inactivos = consultar("/api/v1/admin/clientes?activo=false&busqueda=" + correo, admin.token());
        assertThat((Integer) JsonPath.read(inactivos.getResponse().getContentAsString(), "$.totalElementos")).isZero();
    }

    @Test
    void desactivarExigeMotivoYCierraLaSesionDeInmediato() throws Exception {
        Sesion admin = ingresarAdmin();
        String correo = correoUnico();
        entrar(correo, false);
        Sesion cliente = entrar(correo, true);
        String url = "/api/v1/admin/clientes/" + cliente.id();

        esperarEstado(enviarConToken(url + "/desactivar", Map.of("motivo", "  "), admin.token()), 400);

        MvcResult r = enviarConToken(url + "/desactivar", Map.of("motivo", "Agresión a otro jugador"), admin.token());
        esperarEstado(r, 200);
        assertThat((Boolean) JsonPath.read(r.getResponse().getContentAsString(), "$.activo")).isFalse();

        // su access token deja de servir de inmediato, no solo al vencer
        esperarEstado(consultar("/api/v1/me", cliente.token()), 401);
        // su refresh token ya no renueva
        esperarEstado(enviar("/api/v1/autenticacion/refrescar", Map.of(), cookie(cliente.refresco())), 401);
        // no puede iniciar sesión y recibe un mensaje claro
        MvcResult login = ingresoGoogle(credencialGoogle(correo, "Ana", "Pérez"), false);
        esperarEstado(login, 403);
        assertThat(codigo(login)).isEqualTo("USUARIO_DESACTIVADO");
        // un token de Google que no es válido ni siquiera llega a revelar el estado de la cuenta
        esperarEstado(ingresoGoogle("inventado", false), 401);

        // quedó en la bitácora con el admin como actor
        Map<String, Object> fila = jdbc.queryForMap(
                "SELECT actor_id, actor_rol, motivo FROM audit_log WHERE accion='DESACTIVAR_CLIENTE' AND entidad_id = ?",
                cliente.id());
        assertThat(((Number) fila.get("actor_id")).longValue()).isEqualTo(admin.id());
        assertThat(fila.get("actor_rol")).isEqualTo("ADMIN");
        assertThat(fila.get("motivo")).isEqualTo("Agresión a otro jugador");

        // reactivar devuelve el acceso
        esperarEstado(enviarConToken(url + "/activar", Map.of(), admin.token()), 200);
        esperarEstado(ingresoGoogle(credencialGoogle(correo, "Ana", "Pérez"), false), 200);
    }

    @Test
    void sinReservasFuturasLaPrevisualizacionNoRequiereConfirmacion() throws Exception {
        Sesion admin = ingresarAdmin();
        Sesion cliente = nuevoClienteConSesion();
        MvcResult r = enviarConToken("/api/v1/admin/clientes/" + cliente.id() + "/desactivar/previsualizar", Map.of(), admin.token());
        esperarEstado(r, 200);
        assertThat((Integer) JsonPath.read(r.getResponse().getContentAsString(), "$.reservasFuturasActivas")).isZero();
        assertThat((Boolean) JsonPath.read(r.getResponse().getContentAsString(), "$.requiereConfirmacion")).isFalse();
    }

    @Test
    void elAdminHabilitaLaInscripcionDeEquiposYQuedaAuditado() throws Exception {
        Sesion admin = ingresarAdmin();
        Sesion cliente = nuevoClienteConSesion();
        MvcResult r = patch("/api/v1/admin/clientes/" + cliente.id() + "/inscripcion-equipos", Map.of("habilitado", true), admin.token());
        esperarEstado(r, 200);
        assertThat((Boolean) JsonPath.read(r.getResponse().getContentAsString(), "$.puedeInscribirEquipos")).isTrue();
        Integer auditorias = jdbc.queryForObject(
                "SELECT count(*) FROM audit_log WHERE accion='HABILITAR_INSCRIPCION_EQUIPOS' AND entidad_id = ?", Integer.class, cliente.id());
        assertThat(auditorias).isEqualTo(1);
    }

    @Test
    void unClienteNoPuedeGestionarOtrosClientes() throws Exception {
        Sesion a = nuevoClienteConSesion();
        Sesion b = nuevoClienteConSesion();
        esperarEstado(enviarConToken("/api/v1/admin/clientes/" + b.id() + "/desactivar", Map.of("motivo", "x"), a.token()), 403);
        // y el cliente atacado sigue activo
        esperarEstado(consultar("/api/v1/me", b.token()), 200);
    }

    @Test
    void elAdminNoPuedeDesactivarAOtroAdministradorPorLaRutaDeClientes() throws Exception {
        Sesion admin = ingresarAdmin();
        MvcResult r = enviarConToken("/api/v1/admin/clientes/" + admin.id() + "/desactivar", Map.of("motivo", "x"), admin.token());
        esperarEstado(r, 404);
    }

    @Test
    void elAdminAutorizaOtroAdministradorPorCorreoYEntrarConGoogleNuncaDaEseRol() throws Exception {
        Sesion admin = ingresarAdmin();
        String correo = "segundo-admin-" + System.nanoTime() + "@goltime.test";

        MvcResult creado = enviarConToken("/api/v1/admin/administradores",
                Map.of("nombres", "Luisa", "apellidos", "Rojas", "correo", correo), admin.token());
        esperarEstado(creado, 201);
        assertThat((String) JsonPath.read(creado.getResponse().getContentAsString(), "$.rol")).isEqualTo("ADMIN");

        // Entra con la cuenta de Google de ese correo y queda vinculada: es administradora.
        Sesion nuevoAdmin = entrar(correo, false);
        esperarEstado(consultar("/api/v1/admin/clientes", nuevoAdmin.token()), 200);
        assertThat(jdbc.queryForObject("SELECT rol FROM usuarios WHERE correo = ?", String.class, correo)).isEqualTo("ADMIN");

        // Un correo cualquiera que entra con Google solo llega a ser cliente, y no abre nada de administración.
        Sesion cualquiera = nuevoClienteConSesion();
        esperarEstado(consultar("/api/v1/admin/clientes", cualquiera.token()), 403);

        MvcResult duplicado = enviarConToken("/api/v1/admin/administradores",
                Map.of("nombres", "Luisa", "apellidos", "Rojas", "correo", correo.toUpperCase()), admin.token());
        esperarEstado(duplicado, 409);
    }
}
