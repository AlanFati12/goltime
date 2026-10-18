package co.edu.usco.goltime.reservas.infraestructura;

import java.util.List;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import co.edu.usco.goltime.compartido.aplicacion.ContextoActual;
import co.edu.usco.goltime.compartido.dominio.Actor;
import co.edu.usco.goltime.reservas.aplicacion.ServicioReservas;
import co.edu.usco.goltime.usuarios.aplicacion.ReservasDeUsuario;

/** Implementa el puerto que usa el módulo de usuarios al desactivar a un cliente con reservas futuras. */
@Component
class AdaptadorReservasDeUsuario implements ReservasDeUsuario {

    private final ServicioReservas reservas;
    private final ContextoActual contexto;

    AdaptadorReservasDeUsuario(@Lazy ServicioReservas reservas, ContextoActual contexto) {
        this.reservas = reservas;
        this.contexto = contexto;
    }

    @Override
    public int contarFuturasActivas(long usuarioId) {
        return reservas.idsFuturasDeCliente(usuarioId).size();
    }

    @Override
    public int cancelarFuturas(long usuarioId, String motivo) {
        List<Long> ids = reservas.idsFuturasDeCliente(usuarioId);
        Actor admin = contexto.actor().orElse(Actor.sistema());
        ids.forEach(id -> reservas.cancelarPorAdmin(id, admin, motivo));
        return ids.size();
    }
}
