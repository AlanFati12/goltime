-- V8: promociones (descuentos por código o automáticos) y premios de fidelidad (reservas completadas -> beneficio).

CREATE TABLE promociones (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre               VARCHAR(100) NOT NULL,
    descripcion          TEXT,
    -- con código la usa quien lo escribe; sin código se aplica sola si la reserva cumple las condiciones
    codigo               VARCHAR(40),
    tipo                 VARCHAR(15)  NOT NULL CHECK (tipo IN ('PORCENTAJE', 'MONTO_FIJO')),
    valor                INTEGER      NOT NULL CHECK (valor > 0),
    fecha_desde          DATE         NOT NULL,
    fecha_hasta          DATE         NOT NULL,
    dias_semana          SMALLINT[]   NOT NULL DEFAULT '{}',
    minuto_inicio        INTEGER CHECK (minuto_inicio BETWEEN 0 AND 1439),
    minuto_fin           INTEGER CHECK (minuto_fin BETWEEN 1 AND 1440),
    sede_id              BIGINT REFERENCES sedes (id),
    cancha_id            BIGINT REFERENCES canchas (id),
    usos_maximos         INTEGER CHECK (usos_maximos >= 1),
    usos_por_cliente     INTEGER CHECK (usos_por_cliente >= 1),
    solo_primera_reserva BOOLEAN      NOT NULL DEFAULT FALSE,
    publica              BOOLEAN      NOT NULL DEFAULT TRUE,
    activa               BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    creado_por           BIGINT,
    actualizado_por      BIGINT,
    CONSTRAINT ck_promociones_fechas CHECK (fecha_hasta >= fecha_desde),
    CONSTRAINT ck_promociones_porcentaje CHECK (tipo <> 'PORCENTAJE' OR valor <= 100),
    CONSTRAINT ck_promociones_franja CHECK ((minuto_inicio IS NULL) = (minuto_fin IS NULL) AND (minuto_inicio IS NULL OR minuto_fin > minuto_inicio))
);
CREATE UNIQUE INDEX uq_promociones_codigo ON promociones (lower(codigo)) WHERE codigo IS NOT NULL;
CREATE INDEX ix_promociones_vigencia ON promociones (activa, fecha_desde, fecha_hasta);

-- Cada reserva usa como máximo una promoción; si la reserva se cancela, el uso queda LIBERADO y deja de contar.
CREATE TABLE usos_promocion (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    promocion_id BIGINT      NOT NULL REFERENCES promociones (id),
    reserva_id   BIGINT      NOT NULL UNIQUE REFERENCES reservas (id),
    cliente_id   BIGINT      NOT NULL REFERENCES usuarios (id),
    monto        INTEGER     NOT NULL CHECK (monto > 0),
    estado       VARCHAR(10) NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'LIBERADO')),
    creado_en    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_usos_promocion ON usos_promocion (promocion_id, estado);
CREATE INDEX ix_usos_promocion_cliente ON usos_promocion (cliente_id, promocion_id, estado);

CREATE TABLE reglas_premio (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre              VARCHAR(100) NOT NULL,
    descripcion         TEXT,
    reservas_requeridas INTEGER      NOT NULL CHECK (reservas_requeridas >= 1),
    tipo                VARCHAR(15)  NOT NULL CHECK (tipo IN ('PORCENTAJE', 'MONTO_FIJO')),
    valor               INTEGER      NOT NULL CHECK (valor > 0),
    vigencia_dias       INTEGER      NOT NULL DEFAULT 60 CHECK (vigencia_dias >= 1),
    activa              BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_reglas_premio_porcentaje CHECK (tipo <> 'PORCENTAJE' OR valor <= 100)
);

-- El beneficio se copia al otorgarlo: cambiar la regla después no altera los premios ya ganados.
CREATE TABLE premios_cliente (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cliente_id     BIGINT       NOT NULL REFERENCES usuarios (id),
    regla_id       BIGINT       NOT NULL REFERENCES reglas_premio (id),
    nombre         VARCHAR(100) NOT NULL,
    tipo           VARCHAR(15)  NOT NULL,
    valor          INTEGER      NOT NULL,
    estado         VARCHAR(12)  NOT NULL DEFAULT 'DISPONIBLE' CHECK (estado IN ('DISPONIBLE', 'USADO', 'VENCIDO', 'REVOCADO')),
    otorgado_en    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    vence_en       TIMESTAMPTZ  NOT NULL,
    reserva_uso_id BIGINT REFERENCES reservas (id),
    motivo_revocacion VARCHAR(300)
);
CREATE INDEX ix_premios_cliente ON premios_cliente (cliente_id, estado);
CREATE INDEX ix_premios_regla ON premios_cliente (regla_id, cliente_id);
