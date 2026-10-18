package co.edu.usco.goltime.premios.infraestructura;

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
import co.edu.usco.goltime.compartido.dominio.TipoDescuento;
import co.edu.usco.goltime.premios.aplicacion.RepositorioPremios.PremioOtorgado;
import co.edu.usco.goltime.premios.aplicacion.ServicioPremios;
import co.edu.usco.goltime.premios.dominio.EstadoPremio;
import co.edu.usco.goltime.premios.dominio.ReglaPremio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Controladores REST de premios de fidelidad. */
final class ControladoresPremios {

    private ControladoresPremios() {
    }

    record SolicitudRegla(@NotBlank @Size(max = 100) String nombre, String descripcion, @NotNull @Min(1) Integer reservasRequeridas, @NotNull TipoDescuento tipo,
            @NotNull @Min(1) Integer valor, @NotNull @Min(1) Integer vigenciaDias, Boolean activa) {
    }

    record SolicitudRevocacion(@NotBlank @Size(max = 300) String motivo) {
    }

    record RespuestaRegla(Long id, String nombre, String descripcion, int reservasRequeridas, TipoDescuento tipo, int valor, String beneficio, int vigenciaDias, boolean activa) {
    }

    record RespuestaPremio(Long id, Long clienteId, String nombre, TipoDescuento tipo, int valor, String beneficio, EstadoPremio estado, Instant otorgadoEn, Instant venceEn,
            Long reservaUsoId, String motivoRevocacion) {
    }

    record RespuestaProgreso(RespuestaRegla regla, int completadas, int enCiclo, int faltan) {
    }

    record RespuestaMisPremios(List<RespuestaProgreso> progreso, List<RespuestaPremio> premios) {
    }

    private static RespuestaRegla regla(ReglaPremio r) {
        return new RespuestaRegla(r.id(), r.nombre(), r.descripcion(), r.reservasRequeridas(), r.tipo(), r.valor(), r.tipo().texto(r.valor()), r.vigenciaDias(), r.activa());
    }

    private static RespuestaPremio premio(PremioOtorgado p) {
        return new RespuestaPremio(p.id(), p.clienteId(), p.nombre(), p.tipo(), p.valor(), p.tipo().texto(p.valor()), p.estado(), p.otorgadoEn(), p.venceEn(), p.reservaUsoId(),
                p.motivoRevocacion());
    }

    private static ReglaPremio dominio(Long id, SolicitudRegla s) {
        return new ReglaPremio(id, s.nombre(), s.descripcion(), s.reservasRequeridas(), s.tipo(), s.valor(), s.vigenciaDias(), s.activa() == null || s.activa());
    }

    @RestController
    @RequestMapping("/api/v1/admin/premios")
    @PreAuthorize("hasRole('ADMIN')")
    @Tag(name = "Admin: premios", description = "Reglas de fidelidad y premios otorgados")
    static class Admin {

        private final ServicioPremios premios;

        Admin(ServicioPremios premios) {
            this.premios = premios;
        }

        @GetMapping("/reglas")
        public List<RespuestaRegla> reglas() {
            return premios.reglas(false).stream().map(ControladoresPremios::regla).toList();
        }

        @PostMapping("/reglas")
        @ResponseStatus(HttpStatus.CREATED)
        @Operation(summary = "Crear una regla: cada N reservas completadas, un beneficio")
        public RespuestaRegla crear(@Valid @RequestBody SolicitudRegla s) {
            return regla(premios.crearRegla(dominio(null, s)));
        }

        @PutMapping("/reglas/{id}")
        @Operation(summary = "Editar una regla (no cambia los premios ya otorgados)")
        public RespuestaRegla actualizar(@PathVariable long id, @Valid @RequestBody SolicitudRegla s) {
            return regla(premios.actualizarRegla(id, dominio(id, s)));
        }

        @GetMapping("/otorgados")
        public List<RespuestaPremio> otorgados(@RequestParam(required = false) Long clienteId, @RequestParam(required = false) EstadoPremio estado) {
            return premios.listar(clienteId, estado).stream().map(ControladoresPremios::premio).toList();
        }

        @PostMapping("/otorgados/{id}/revocar")
        @Operation(summary = "Revocar un premio disponible (con motivo)")
        public void revocar(@PathVariable long id, @Valid @RequestBody SolicitudRevocacion s) {
            premios.revocar(id, s.motivo());
        }
    }

    @RestController
    @RequestMapping("/api/v1/me/premios")
    @Tag(name = "Cliente: premios", description = "Progreso y premios del cliente")
    static class Cliente {

        private final ServicioPremios premios;
        private final ContextoActual contexto;

        Cliente(ServicioPremios premios, ContextoActual contexto) {
            this.premios = premios;
            this.contexto = contexto;
        }

        @GetMapping
        @Operation(summary = "Mi progreso hacia cada premio y los premios que he ganado")
        public RespuestaMisPremios mios() {
            var r = premios.misPremios(contexto.actor().orElseThrow().id());
            return new RespuestaMisPremios(r.progreso().stream().map(p -> new RespuestaProgreso(regla(p.regla()), p.completadas(), p.enCiclo(), p.faltan())).toList(),
                    r.premios().stream().map(ControladoresPremios::premio).toList());
        }
    }
}
