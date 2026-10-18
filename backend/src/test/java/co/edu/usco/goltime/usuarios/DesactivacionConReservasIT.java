package co.edu.usco.goltime.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

import co.edu.usco.goltime.BaseIntegracion;
import co.edu.usco.goltime.usuarios.aplicacion.ReservasDeUsuario;

/** Verifica el patrón de conflictos: con reservas futuras la desactivación exige confirmación explícita. */
class DesactivacionConReservasIT extends BaseIntegracion {

    @MockitoBean
    ReservasDeUsuario reservas;

    @Test
    void conReservasFuturasExigeConfirmacionYLuegoLasCancela() throws Exception {
        Sesion admin = ingresarAdmin();
        Sesion cliente = nuevoClienteConSesion();
        when(reservas.contarFuturasActivas(cliente.id())).thenReturn(2);
        when(reservas.cancelarFuturas(anyLong(), anyString())).thenReturn(2);
        String base = "/api/v1/admin/clientes/" + cliente.id();

        MvcResult preview = enviarConToken(base + "/desactivar/previsualizar", Map.of(), admin.token());
        assertThat((Integer) JsonPath.read(preview.getResponse().getContentAsString(), "$.reservasFuturasActivas")).isEqualTo(2);
        assertThat((Boolean) JsonPath.read(preview.getResponse().getContentAsString(), "$.requiereConfirmacion")).isTrue();

        MvcResult sinConfirmar = enviarConToken(base + "/desactivar", Map.of("motivo", "Mal comportamiento"), admin.token());
        esperarEstado(sinConfirmar, 409);
        assertThat((String) JsonPath.read(sinConfirmar.getResponse().getContentAsString(), "$.codigo")).isEqualTo("CONFLICTOS_SIN_CONFIRMAR");
        verify(reservas, never()).cancelarFuturas(anyLong(), anyString());
        esperarEstado(consultar("/api/v1/me", cliente.token()), 200); // sigue activo

        MvcResult confirmado = enviarConToken(base + "/desactivar",
                Map.of("motivo", "Mal comportamiento", "confirmarConflictos", true), admin.token());
        esperarEstado(confirmado, 200);
        verify(reservas).cancelarFuturas(cliente.id(), "Cuenta desactivada: Mal comportamiento");
        esperarEstado(consultar("/api/v1/me", cliente.token()), 401);
    }
}
