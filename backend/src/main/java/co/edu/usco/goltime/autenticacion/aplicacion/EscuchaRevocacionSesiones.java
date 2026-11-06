package co.edu.usco.goltime.autenticacion.aplicacion;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import co.edu.usco.goltime.usuarios.dominio.UsuarioDesactivadoEvento;

/**
 * Cierra todas las sesiones de un usuario cuando es desactivado. Corre en la
 * misma transacción del evento, así que ambos cambios se confirman juntos.
 */
@Component
class EscuchaRevocacionSesiones {

    private final RepositorioTokensRefresco tokens;

    EscuchaRevocacionSesiones(RepositorioTokensRefresco tokens) {
        this.tokens = tokens;
    }

    @EventListener
    void alDesactivar(UsuarioDesactivadoEvento evento) {
        tokens.revocarTodosDe(evento.usuarioId());
    }
}
