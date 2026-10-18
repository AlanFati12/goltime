package co.edu.usco.goltime.promociones.infraestructura;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import co.edu.usco.goltime.compartido.aplicacion.ContextoActual;
import co.edu.usco.goltime.promociones.aplicacion.ServicioDescuentos;
import co.edu.usco.goltime.promociones.aplicacion.ServicioPromociones;
import co.edu.usco.goltime.promociones.dominio.Promocion;
import co.edu.usco.goltime.promociones.infraestructura.DtosPromociones.RespuestaDetallePromocion;
import co.edu.usco.goltime.promociones.infraestructura.DtosPromociones.RespuestaPrevisualizacion;
import co.edu.usco.goltime.promociones.infraestructura.DtosPromociones.RespuestaPromocion;
import co.edu.usco.goltime.promociones.infraestructura.DtosPromociones.RespuestaUso;
import co.edu.usco.goltime.promociones.infraestructura.DtosPromociones.SolicitudActiva;
import co.edu.usco.goltime.promociones.infraestructura.DtosPromociones.SolicitudPromocion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Controladores REST de promociones: administración, vitrina pública y previsualización del cliente. */
final class ControladoresPromociones {

    private ControladoresPromociones() {
    }

    static RespuestaPromocion aRespuesta(Promocion p, boolean conCodigo) {
        return new RespuestaPromocion(p.id(), p.nombre(), p.descripcion(), conCodigo ? p.codigo() : null, p.tipo(), p.valor(), p.tipo().texto(p.valor()), p.fechaDesde(),
                p.fechaHasta(), p.diasSemana().stream().sorted().toList(), p.minutoInicio(), p.minutoFin(), p.sedeId(), p.canchaId(), p.usosMaximos(), p.usosPorCliente(),
                p.soloPrimeraReserva(), p.publica(), p.activa(), p.esAutomatica());
    }

    static Promocion aDominio(Long id, SolicitudPromocion s, boolean activa) {
        return new Promocion(id, s.nombre(), s.descripcion(), s.codigo(), s.tipo(), s.valor(), s.fechaDesde(), s.fechaHasta(), s.diasSemana(), s.minutoInicio(), s.minutoFin(),
                s.sedeId(), s.canchaId(), s.usosMaximos(), s.usosPorCliente(), Boolean.TRUE.equals(s.soloPrimeraReserva()), s.publica() == null || s.publica(), activa);
    }

    @RestController
    @RequestMapping("/api/v1/admin/promociones")
    @PreAuthorize("hasRole('ADMIN')")
    @Tag(name = "Admin: promociones", description = "Descuentos por código o automáticos")
    static class Admin {

        private final ServicioPromociones promociones;

        Admin(ServicioPromociones promociones) {
            this.promociones = promociones;
        }

        @GetMapping
        public List<RespuestaPromocion> listar(@RequestParam(required = false) Boolean activa) {
            return promociones.listar(activa).stream().map(p -> aRespuesta(p, true)).toList();
        }

        @PostMapping
        @ResponseStatus(HttpStatus.CREATED)
        @Operation(summary = "Crear una promoción", description = "Con `codigo` la usa quien lo escribe; sin código se aplica sola a las reservas que cumplan sus condiciones.")
        public RespuestaPromocion crear(@Valid @RequestBody SolicitudPromocion s) {
            return aRespuesta(promociones.crear(aDominio(null, s, true)), true);
        }

        @GetMapping("/{id}")
        public RespuestaDetallePromocion detalle(@PathVariable long id) {
            var d = promociones.detalle(id);
            return new RespuestaDetallePromocion(aRespuesta(d.promocion(), true), d.usos().usosActivos(), d.usos().montoDescontado(),
                    d.ultimosUsos().stream().map(u -> new RespuestaUso(u.reservaId(), u.clienteId(), u.monto(), u.estado(), u.creadoEn())).toList());
        }

        @PutMapping("/{id}")
        public RespuestaPromocion actualizar(@PathVariable long id, @Valid @RequestBody SolicitudPromocion s) {
            return aRespuesta(promociones.actualizar(id, aDominio(id, s, true)), true);
        }

        @PostMapping("/{id}/estado")
        public RespuestaPromocion estado(@PathVariable long id, @Valid @RequestBody SolicitudActiva s) {
            return aRespuesta(promociones.cambiarActiva(id, s.activa()), true);
        }
    }

    @RestController
    @RequestMapping("/api/v1/publico/promociones")
    @Tag(name = "Público: promociones", description = "Promociones vigentes que el negocio quiere mostrar")
    static class Publico {

        private final ServicioPromociones promociones;

        Publico(ServicioPromociones promociones) {
            this.promociones = promociones;
        }

        @GetMapping
        public List<RespuestaPromocion> vigentes() {
            return promociones.publicas().stream().map(p -> aRespuesta(p, true)).toList();
        }
    }

    @RestController
    @RequestMapping("/api/v1/me/descuentos")
    @Tag(name = "Cliente: descuentos", description = "Previsualiza el precio con un código o un premio antes de reservar")
    static class Cliente {

        private final ServicioDescuentos descuentos;
        private final ContextoActual contexto;

        Cliente(ServicioDescuentos descuentos, ContextoActual contexto) {
            this.descuentos = descuentos;
            this.contexto = contexto;
        }

        @GetMapping("/previsualizar")
        @Operation(summary = "Precio de la reserva con el descuento que se aplicaría",
                description = "Un código o premio inválido responde 422 PROMOCION_INVALIDA con el motivo. Solo se aplica el descuento de mayor valor.")
        public RespuestaPrevisualizacion previsualizar(@RequestParam long canchaId, @RequestParam Instant inicio, @RequestParam Instant fin,
                @RequestParam(required = false) String codigo, @RequestParam(required = false) Long premioId) {
            var p = descuentos.previsualizar(contexto.actor().orElseThrow().id(), canchaId, inicio, fin, codigo, premioId);
            return new RespuestaPrevisualizacion(p.precioBase(), p.descuento(), p.precioFinal(), p.descripcion(), p.origen());
        }
    }
}
