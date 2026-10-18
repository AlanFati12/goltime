package co.edu.usco.goltime.promociones.aplicacion;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import co.edu.usco.goltime.promociones.dominio.Promocion;

/** Promociones y el registro de sus usos (uno por reserva). */
public interface RepositorioPromociones {

    /** Resumen de uso de una promoción. */
    record Uso(long promocionId, long reservaId, long clienteId, int monto, String estado, Instant creadoEn) {
    }

    record ResumenUsos(int usosActivos, long montoDescontado) {
    }

    Promocion guardar(Promocion promocion);

    Optional<Promocion> buscar(long id);

    Optional<Promocion> buscarPorCodigo(String codigo);

    boolean existeCodigo(String codigo, Long ignorarId);

    /** {@code activa} nulo = todas. */
    List<Promocion> listar(Boolean activa);

    /** Promociones activas sin código que cubren la fecha. */
    List<Promocion> automaticasVigentes(LocalDate fecha);

    /** Promociones activas y públicas cuya vigencia no ha terminado (para mostrarlas a los clientes). */
    List<Promocion> publicasVigentes(LocalDate hoy);

    /** Bloquea la fila de la promoción hasta terminar la transacción (cupos concurrentes). */
    void bloquear(long promocionId);

    int usosActivos(long promocionId);

    int usosActivosDeCliente(long promocionId, long clienteId);

    void registrarUso(long promocionId, long reservaId, long clienteId, int monto);

    void liberarUsosDeReserva(long reservaId);

    ResumenUsos resumenUsos(long promocionId);

    List<Uso> ultimosUsos(long promocionId, int limite);
}
