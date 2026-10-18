-- V5: reservas, ocupación de canchas (anti-solapamiento), historial y notificaciones in-app.

-- Con aprobación del administrador, la solicitud debe dejar tiempo para responder antes de expirar
-- (12 h o 2 h antes del inicio): por eso la antelación mínima por defecto pasa de 1 h a 3 h.
UPDATE configuracion SET valor = '3' WHERE clave = 'antelacion_minima_horas';

CREATE SEQUENCE reservas_codigo_seq START 1;

CREATE TABLE reservas (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo                  VARCHAR(20)  NOT NULL UNIQUE,
    cliente_id              BIGINT REFERENCES usuarios (id),
    cliente_mostrador_id    BIGINT REFERENCES clientes_mostrador (id),
    cancha_id               BIGINT       NOT NULL REFERENCES canchas (id),
    inicio                  TIMESTAMPTZ  NOT NULL,
    fin                     TIMESTAMPTZ  NOT NULL,
    estado                  VARCHAR(20)  NOT NULL CHECK (estado IN
        ('PENDIENTE', 'CONFIRMADA', 'RECHAZADA', 'EXPIRADA', 'CANCELADA', 'COMPLETADA', 'NO_ASISTIO')),
    origen                  VARCHAR(20)  NOT NULL DEFAULT 'WEB' CHECK (origen IN ('WEB', 'ADMIN', 'TORNEO', 'RECURRENTE')),
    serie_id                BIGINT,
    -- Instantánea del precio al reservar: cambios posteriores de tarifas no alteran reservas existentes.
    precio_base             INTEGER      NOT NULL CHECK (precio_base >= 0),
    descuento               INTEGER      NOT NULL DEFAULT 0 CHECK (descuento >= 0),
    precio_final            INTEGER      NOT NULL CHECK (precio_final >= 0),
    promocion_id            BIGINT,
    estado_pago             VARCHAR(25)  NOT NULL DEFAULT 'PENDIENTE' CHECK (estado_pago IN
        ('PENDIENTE', 'ABONADO', 'PAGADO', 'REEMBOLSO_PENDIENTE', 'REEMBOLSADO')),
    monto_pagado            INTEGER      NOT NULL DEFAULT 0 CHECK (monto_pagado >= 0),
    nombre_equipo           VARCHAR(100),
    notas                   VARCHAR(500),
    cancelada_por           BIGINT REFERENCES usuarios (id),
    rol_cancelador          VARCHAR(20)  CHECK (rol_cancelador IN ('ADMIN', 'CLIENTE', 'SISTEMA')),
    motivo_cancelacion      VARCHAR(500),
    fecha_cancelacion       TIMESTAMPTZ,
    cancelacion_tardia      BOOLEAN      NOT NULL DEFAULT FALSE,
    veces_reprogramada      INTEGER      NOT NULL DEFAULT 0,
    aprobada_por            BIGINT REFERENCES usuarios (id),
    fecha_aprobacion        TIMESTAMPTZ,
    motivo_rechazo          VARCHAR(500),
    expira_en               TIMESTAMPTZ,
    clave_idempotencia      VARCHAR(100),
    recordatorio_enviado_en TIMESTAMPTZ,
    version                 BIGINT       NOT NULL DEFAULT 0,
    creado_en               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    creado_por              BIGINT,
    actualizado_por         BIGINT,
    CONSTRAINT ck_reservas_rango CHECK (fin > inicio),
    CONSTRAINT ck_reservas_cliente CHECK ((cliente_id IS NOT NULL)::int + (cliente_mostrador_id IS NOT NULL)::int = 1)
);
CREATE INDEX ix_reservas_cliente_inicio ON reservas (cliente_id, inicio DESC);
CREATE INDEX ix_reservas_cancha_inicio ON reservas (cancha_id, inicio);
CREATE INDEX ix_reservas_estado_inicio ON reservas (estado, inicio);
CREATE INDEX ix_reservas_pendientes_expiran ON reservas (expira_en) WHERE estado = 'PENDIENTE';
CREATE UNIQUE INDEX uq_reservas_idempotencia ON reservas (cliente_id, clave_idempotencia) WHERE clave_idempotencia IS NOT NULL;

-- Cada fila ocupa una cancha durante un rango. La restricción de exclusión hace IMPOSIBLE, a nivel de base
-- de datos y aun con peticiones concurrentes, que dos ocupaciones se solapen en la misma cancha.
-- Una reserva de una cancha compuesta ocupa la propia cancha y todas sus componentes (ADR 0003).
CREATE TABLE ocupaciones_cancha (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cancha_id   BIGINT      NOT NULL REFERENCES canchas (id),
    rango       TSTZRANGE   NOT NULL,
    tipo        VARCHAR(20) NOT NULL CHECK (tipo IN ('RESERVA', 'BLOQUEO')),
    reserva_id  BIGINT REFERENCES reservas (id),
    bloqueo_id  BIGINT,
    CONSTRAINT ck_ocupaciones_origen CHECK (
        (tipo = 'RESERVA' AND reserva_id IS NOT NULL) OR (tipo = 'BLOQUEO' AND bloqueo_id IS NOT NULL)),
    CONSTRAINT ex_ocupaciones_sin_solape EXCLUDE USING gist (cancha_id WITH =, rango WITH &&)
);
CREATE INDEX ix_ocupaciones_reserva ON ocupaciones_cancha (reserva_id);

CREATE TABLE reserva_historial (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    reserva_id       BIGINT      NOT NULL REFERENCES reservas (id),
    accion           VARCHAR(30) NOT NULL CHECK (accion IN ('CREADA', 'APROBADA', 'RECHAZADA', 'EXPIRADA', 'REPROGRAMADA',
        'CANCELADA', 'COMPLETADA', 'NO_ASISTIO', 'PAGO_REGISTRADO', 'MODIFICADA')),
    actor_id         BIGINT REFERENCES usuarios (id),
    actor_rol        VARCHAR(20) NOT NULL CHECK (actor_rol IN ('ADMIN', 'CLIENTE', 'SISTEMA')),
    motivo           VARCHAR(500),
    datos_anteriores JSONB,
    datos_nuevos     JSONB,
    fecha            TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_reserva_historial_reserva ON reserva_historial (reserva_id, fecha);

CREATE TABLE notificaciones (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id BIGINT       NOT NULL REFERENCES usuarios (id),
    tipo       VARCHAR(40)  NOT NULL,
    titulo     VARCHAR(150) NOT NULL,
    mensaje    VARCHAR(500) NOT NULL,
    leida      BOOLEAN      NOT NULL DEFAULT FALSE,
    entidad    VARCHAR(40),
    entidad_id BIGINT,
    creado_en  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_notificaciones_usuario ON notificaciones (usuario_id, leida, creado_en DESC);
