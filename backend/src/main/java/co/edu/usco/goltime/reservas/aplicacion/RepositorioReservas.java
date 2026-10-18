package co.edu.usco.goltime.reservas.aplicacion;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import co.edu.usco.goltime.compartido.dominio.Pagina;
import co.edu.usco.goltime.compartido.dominio.SolicitudPagina;
import co.edu.usco.goltime.reservas.dominio.EstadoPago;
import co.edu.usco.goltime.reservas.dominio.EstadoReserva;
import co.edu.usco.goltime.reservas.dominio.OrigenReserva;
import co.edu.usco.goltime.reservas.dominio.Reserva;

/** Puerto de persistencia y consulta de reservas. */
public interface RepositorioReservas {

    /** Inserta (id nulo) o actualiza; devuelve la reserva con su id. */
    Reserva guardar(Reserva reserva);

    Optional<Reserva> buscarPorId(long id);

    Optional<Reserva> buscarPorClaveIdempotencia(long clienteId, String clave);

    /** Bloquea hasta terminar la transacción a quien use la misma clave (evita dobles reservas por doble clic). */
    void bloquearPorClave(String clave);

    /** Código legible y único, ej. GT-2026-000123. */
    String siguienteCodigo(int anio);

    int contarActivasFuturas(long clienteId, Instant ahora);

    List<Long> idsFuturasActivasDeCliente(long clienteId, Instant ahora);

    Pagina<Reserva> deCliente(long clienteId, PestanaReservas pestana, Instant ahora, SolicitudPagina pagina);

    Pagina<Reserva> deAdmin(FiltroReservasAdmin filtro, SolicitudPagina pagina);

    Map<Long, Relacionados> relacionados(Collection<Long> reservaIds);

    List<Long> idsPendientesVencidas(Instant ahora, int limite);

    List<Long> idsConfirmadasTerminadas(Instant ahora, int limite);

    List<Long> idsPorRecordar(Instant ahora, Instant hasta, int limite);

    enum PestanaReservas {
        PROXIMAS, HISTORIAL, CANCELADAS
    }

    /** Filtros del listado de administración; los nulos no filtran. */
    record FiltroReservasAdmin(EstadoReserva estado, Long sedeId, Long canchaId, EstadoPago estadoPago, OrigenReserva origen,
            Instant desde, Instant hasta, String busqueda) {
    }
}
