package co.edu.usco.goltime.notificaciones.infraestructura;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import co.edu.usco.goltime.compartido.dominio.Pagina;
import co.edu.usco.goltime.compartido.dominio.SolicitudPagina;
import co.edu.usco.goltime.notificaciones.aplicacion.ServicioNotificaciones;
import co.edu.usco.goltime.notificaciones.dominio.Notificacion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Notificaciones del usuario autenticado. El id sale siempre del token (no se puede leer las de otro). */
@RestController
@RequestMapping("/api/v1/me/notificaciones")
@Tag(name = "Notificaciones", description = "Notificaciones in-app del usuario autenticado")
public class ControladorNotificaciones {

    private final ServicioNotificaciones notificaciones;

    public ControladorNotificaciones(ServicioNotificaciones notificaciones) {
        this.notificaciones = notificaciones;
    }

    @GetMapping
    @Operation(summary = "Mis notificaciones, de la más reciente a la más antigua")
    public Pagina<RespuestaNotificacion> listar(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "false") boolean soloNoLeidas,
            @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamano) {
        return notificaciones.listar(Long.parseLong(jwt.getSubject()), soloNoLeidas, new SolicitudPagina(pagina, tamano))
                .mapear(RespuestaNotificacion::de);
    }

    @GetMapping("/contador")
    @Operation(summary = "Cantidad de notificaciones sin leer (para la campana)")
    public RespuestaContador contador(@AuthenticationPrincipal Jwt jwt) {
        return new RespuestaContador(notificaciones.contarNoLeidas(Long.parseLong(jwt.getSubject())));
    }

    @PostMapping("/{id}/leer")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Marcar una notificación como leída")
    public void leer(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        notificaciones.marcarLeida(Long.parseLong(jwt.getSubject()), id);
    }

    @PostMapping("/leer-todas")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Marcar todas como leídas")
    public void leerTodas(@AuthenticationPrincipal Jwt jwt) {
        notificaciones.marcarTodasLeidas(Long.parseLong(jwt.getSubject()));
    }

    public record RespuestaContador(long noLeidas) {
    }

    public record RespuestaNotificacion(Long id, String tipo, String titulo, String mensaje, boolean leida,
            String entidad, Long entidadId, Instant creadoEn) {

        static RespuestaNotificacion de(Notificacion n) {
            return new RespuestaNotificacion(n.id(), n.tipo(), n.titulo(), n.mensaje(), n.leida(), n.entidad(),
                    n.entidadId(), n.creadoEn());
        }
    }
}
