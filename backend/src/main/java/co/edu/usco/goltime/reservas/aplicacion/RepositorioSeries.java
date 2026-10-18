package co.edu.usco.goltime.reservas.aplicacion;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Series de reservas recurrentes. */
public interface RepositorioSeries {

    record ResumenSerie(long id, long canchaId, String canchaNombre, String cliente, int diaSemana, int minutoInicio, int duracionMin, int semanasPedidas,
            int reservasActivas, int reservasFuturas, Instant creadaEn) {
    }

    long crear(long canchaId, int diaSemana, int minutoInicio, int duracionMin, int semanas, String nombreEquipo, Long creadaPor);

    void asignarCliente(long serieId, Long clienteId, Long mostradorId);

    /** Liga la reserva a la serie y la marca con origen RECURRENTE. */
    void asignarReserva(long reservaId, long serieId);

    Long mostradorDeReserva(long reservaId);

    Optional<ResumenSerie> buscar(long id);

    List<ResumenSerie> listar(Instant ahora);

    /** Reservas de la serie PENDIENTE o CONFIRMADA que todavía no empiezan. */
    List<Long> idsFuturasActivas(long serieId, Instant ahora);
}
