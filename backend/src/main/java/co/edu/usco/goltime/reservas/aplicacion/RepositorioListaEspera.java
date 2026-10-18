package co.edu.usco.goltime.reservas.aplicacion;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Clientes que esperan que se libere un horario ocupado. */
public interface RepositorioListaEspera {

    enum Estado {
        ESPERANDO, NOTIFICADA, CUMPLIDA, CANCELADA
    }

    record Espera(long id, long clienteId, long canchaId, String canchaNombre, String sedeNombre, Instant inicio, Instant fin, Estado estado, Instant creadoEn) {
    }

    Espera crear(long clienteId, long canchaId, Instant inicio, Instant fin);

    Optional<Espera> buscar(long id);

    /** Esperas del cliente que siguen vigentes (ESPERANDO o NOTIFICADA) y cuyo horario no ha empezado. */
    List<Espera> activasDeCliente(long clienteId, Instant ahora);

    int contarActivas(long clienteId, Instant ahora);

    boolean existeActiva(long clienteId, long canchaId, Instant inicio, Instant fin);

    /** Esperas ESPERANDO de esas canchas que se solapan con el rango y todavía no empiezan. */
    List<Espera> esperandoEn(Collection<Long> canchaIds, Instant inicio, Instant fin, Instant ahora);

    void cambiarEstado(long id, Estado estado, Instant notificadaEn);

    /** Marca CUMPLIDA las esperas del cliente (ESPERANDO o NOTIFICADA) que se solapan con la reserva que acaba de hacer. */
    int cumplir(long clienteId, Collection<Long> canchaIds, Instant inicio, Instant fin);
}
