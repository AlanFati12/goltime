-- V6: bloqueos de horario (puntuales y recurrentes) y pagos registrados manualmente por el admin.

CREATE TABLE bloqueos (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sede_id         BIGINT       NOT NULL REFERENCES sedes (id),
    tipo            VARCHAR(20)  NOT NULL CHECK (tipo IN ('PUNTUAL', 'RECURRENTE')),
    -- PUNTUAL: intervalo continuo [inicio, fin); con todo_el_dia se extiende a días completos (hora de Bogotá)
    inicio          TIMESTAMPTZ,
    fin             TIMESTAMPTZ,
    todo_el_dia     BOOLEAN      NOT NULL DEFAULT FALSE,
    -- RECURRENTE: ciertos días de la semana, en un rango horario, entre dos fechas
    dias_semana     SMALLINT[],
    minuto_inicio   INTEGER CHECK (minuto_inicio BETWEEN 0 AND 1439),
    minuto_fin      INTEGER CHECK (minuto_fin BETWEEN 1 AND 1440),
    fecha_desde     DATE,
    fecha_hasta     DATE,
    motivo_tipo     VARCHAR(30)  NOT NULL CHECK (motivo_tipo IN ('MANTENIMIENTO', 'EVENTO_PRIVADO', 'TORNEO', 'CLIMA', 'OTRO')),
    motivo_detalle  VARCHAR(500),
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    version         BIGINT       NOT NULL DEFAULT 0,
    creado_en       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    creado_por      BIGINT,
    actualizado_por BIGINT,
    CONSTRAINT ck_bloqueos_puntual CHECK (tipo <> 'PUNTUAL' OR (inicio IS NOT NULL AND fin IS NOT NULL AND fin > inicio)),
    CONSTRAINT ck_bloqueos_recurrente CHECK (tipo <> 'RECURRENTE' OR (
        cardinality(dias_semana) > 0 AND minuto_inicio IS NOT NULL AND minuto_fin IS NOT NULL AND minuto_fin > minuto_inicio
        AND fecha_desde IS NOT NULL AND fecha_hasta IS NOT NULL AND fecha_hasta >= fecha_desde))
);
CREATE INDEX ix_bloqueos_sede_activo ON bloqueos (sede_id, activo);

-- Canchas elegidas para el bloqueo; sin filas = toda la sede.
CREATE TABLE bloqueo_canchas (
    bloqueo_id BIGINT NOT NULL REFERENCES bloqueos (id),
    cancha_id  BIGINT NOT NULL REFERENCES canchas (id),
    PRIMARY KEY (bloqueo_id, cancha_id)
);

ALTER TABLE ocupaciones_cancha ADD CONSTRAINT fk_ocupaciones_bloqueo FOREIGN KEY (bloqueo_id) REFERENCES bloqueos (id);
CREATE INDEX ix_ocupaciones_bloqueo ON ocupaciones_cancha (bloqueo_id);

CREATE TABLE pagos (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    reserva_id     BIGINT       NOT NULL REFERENCES reservas (id),
    monto          INTEGER      NOT NULL CHECK (monto > 0),
    metodo         VARCHAR(20)  NOT NULL CHECK (metodo IN ('EFECTIVO', 'TRANSFERENCIA', 'NEQUI', 'DAVIPLATA', 'TARJETA')),
    referencia     VARCHAR(100),
    tipo           VARCHAR(20)  NOT NULL CHECK (tipo IN ('PAGO', 'ABONO', 'REEMBOLSO')),
    registrado_por BIGINT       NOT NULL REFERENCES usuarios (id),
    fecha          TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_pagos_reserva ON pagos (reserva_id, fecha);
CREATE INDEX ix_pagos_fecha ON pagos (fecha);
