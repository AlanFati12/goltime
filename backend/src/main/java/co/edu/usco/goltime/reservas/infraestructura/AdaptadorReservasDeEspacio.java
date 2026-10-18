package co.edu.usco.goltime.reservas.infraestructura;

import java.util.List;
import java.util.Set;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import co.edu.usco.goltime.compartido.aplicacion.ContextoActual;
import co.edu.usco.goltime.compartido.dominio.Actor;
import co.edu.usco.goltime.reservas.aplicacion.ServicioReservas;
import co.edu.usco.goltime.sedes.aplicacion.ReservasDeEspacio;

/**
 * Implementa el puerto que usa el módulo de sedes para previsualizar y ejecutar la cancelación de reservas
 * al desactivar una sede o sacar una cancha de servicio. Las cancelaciones las hace el administrador que
 * ejecuta la acción (queda como actor en el historial) y cada cliente es notificado.
 */
@Component
class AdaptadorReservasDeEspacio implements ReservasDeEspacio {

    private final ServicioReservas reservas;
    private final ContextoActual contexto;

    /** {@code @Lazy} rompe el ciclo de beans: ServicioCanchas usa este puerto y ServicioReservas usa ServicioCanchas. */
    AdaptadorReservasDeEspacio(@Lazy ServicioReservas reservas, ContextoActual contexto) {
        this.reservas = reservas;
        this.contexto = contexto;
    }

    @Override
    public int contarFuturasActivasEnCancha(long canchaId) {
        return reservas.idsFuturasEnCanchas(Set.of(canchaId)).size();
    }

    @Override
    public int contarFuturasActivasEnSede(long sedeId) {
        return reservas.idsFuturasEnSede(sedeId).size();
    }

    @Override
    public int cancelarFuturasEnCancha(long canchaId, String motivo) {
        return cancelar(reservas.idsFuturasEnCanchas(Set.of(canchaId)), motivo);
    }

    @Override
    public int cancelarFuturasEnSede(long sedeId, String motivo) {
        return cancelar(reservas.idsFuturasEnSede(sedeId), motivo);
    }

    private int cancelar(List<Long> ids, String motivo) {
        Actor admin = contexto.actor().orElse(Actor.sistema());
        ids.forEach(id -> reservas.cancelarPorAdmin(id, admin, motivo));
        return ids.size();
    }
}
