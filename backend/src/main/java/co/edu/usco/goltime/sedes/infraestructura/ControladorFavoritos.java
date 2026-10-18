package co.edu.usco.goltime.sedes.infraestructura;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import co.edu.usco.goltime.compartido.aplicacion.ContextoActual;
import co.edu.usco.goltime.sedes.aplicacion.ServicioFavoritos;
import co.edu.usco.goltime.sedes.aplicacion.ServicioFavoritos.Favorita;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/me/favoritos")
@Tag(name = "Cliente: favoritos", description = "Canchas favoritas")
public class ControladorFavoritos {

    private final ServicioFavoritos favoritos;
    private final ContextoActual contexto;

    public ControladorFavoritos(ServicioFavoritos favoritos, ContextoActual contexto) {
        this.favoritos = favoritos;
        this.contexto = contexto;
    }

    @GetMapping
    public List<Favorita> listar() {
        return favoritos.deUsuario(contexto.actor().orElseThrow().id());
    }

    @PutMapping("/{canchaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcar(@PathVariable long canchaId) {
        favoritos.marcar(contexto.actor().orElseThrow().id(), canchaId);
    }

    @DeleteMapping("/{canchaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void quitar(@PathVariable long canchaId) {
        favoritos.quitar(contexto.actor().orElseThrow().id(), canchaId);
    }
}
