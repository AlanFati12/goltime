-- V17: el administrador puede llevar una reserva de cualquier estado a cualquier otro (ADR 0019). Cada cambio
-- queda en el historial como ESTADO_CAMBIADO, con el estado de antes y el de después y el motivo.

ALTER TABLE reserva_historial DROP CONSTRAINT reserva_historial_accion_check;
ALTER TABLE reserva_historial ADD CONSTRAINT reserva_historial_accion_check
    CHECK (accion IN ('CREADA', 'APROBADA', 'RECHAZADA', 'EXPIRADA', 'REPROGRAMADA', 'CANCELADA', 'COMPLETADA', 'NO_ASISTIO',
        'PAGO_REGISTRADO', 'MODIFICADA', 'ESTADO_CAMBIADO'));
