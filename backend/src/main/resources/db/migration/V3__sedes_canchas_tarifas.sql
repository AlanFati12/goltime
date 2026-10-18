-- V3: sedes, canchas, horarios de operación, tarifas y festivos.
-- Las horas del día se guardan como minutos desde la medianoche (0..1440) para poder representar
-- un cierre a las 24:00, que un TIME no permite sin ambigüedad. dia_semana sigue ISO-8601: 1=lunes ... 7=domingo.

CREATE TABLE sedes (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre          VARCHAR(120)  NOT NULL,
    direccion       VARCHAR(200)  NOT NULL,
    ciudad          VARCHAR(80)   NOT NULL,
    telefono        VARCHAR(30),
    whatsapp        VARCHAR(30),
    descripcion     TEXT,
    servicios       JSONB         NOT NULL DEFAULT '[]'::jsonb,
    latitud         NUMERIC(9, 6) CHECK (latitud BETWEEN -90 AND 90),
    longitud        NUMERIC(9, 6) CHECK (longitud BETWEEN -180 AND 180),
    activa          BOOLEAN       NOT NULL DEFAULT TRUE,
    version         BIGINT        NOT NULL DEFAULT 0,
    creado_en       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    actualizado_en  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    creado_por      BIGINT,
    actualizado_por BIGINT
);
CREATE UNIQUE INDEX uq_sedes_nombre ON sedes (lower(nombre));

CREATE TABLE sede_fotos (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sede_id   BIGINT       NOT NULL REFERENCES sedes (id),
    archivo   VARCHAR(100) NOT NULL,
    orden     INTEGER      NOT NULL DEFAULT 0,
    creado_en TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_sede_fotos_sede ON sede_fotos (sede_id);

CREATE TABLE sede_horarios (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sede_id         BIGINT   NOT NULL REFERENCES sedes (id),
    dia_semana      INTEGER NOT NULL CHECK (dia_semana BETWEEN 1 AND 7),
    cerrado         BOOLEAN  NOT NULL DEFAULT FALSE,
    minuto_apertura INTEGER CHECK (minuto_apertura BETWEEN 0 AND 1439),
    minuto_cierre   INTEGER CHECK (minuto_cierre BETWEEN 1 AND 1440),
    CONSTRAINT uq_sede_horarios UNIQUE (sede_id, dia_semana),
    CONSTRAINT ck_sede_horarios_rango CHECK (
        cerrado OR (minuto_apertura IS NOT NULL AND minuto_cierre IS NOT NULL AND minuto_cierre > minuto_apertura))
);

CREATE TABLE canchas (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sede_id              BIGINT       NOT NULL REFERENCES sedes (id),
    nombre               VARCHAR(80)  NOT NULL,
    modalidad            VARCHAR(10)  NOT NULL,
    jugadores_por_equipo INTEGER     NOT NULL CHECK (jugadores_por_equipo BETWEEN 2 AND 11),
    capacidad_total      INTEGER     NOT NULL CHECK (capacidad_total >= 2),
    superficie           VARCHAR(60)  NOT NULL DEFAULT 'Sintética',
    largo_m              NUMERIC(5, 1) CHECK (largo_m > 0),
    ancho_m              NUMERIC(5, 1) CHECK (ancho_m > 0),
    techada              BOOLEAN      NOT NULL DEFAULT FALSE,
    iluminacion          BOOLEAN      NOT NULL DEFAULT TRUE,
    precio_base_hora     INTEGER      NOT NULL CHECK (precio_base_hora >= 0),
    duracion_bloque_min  INTEGER     NOT NULL DEFAULT 60 CHECK (duracion_bloque_min BETWEEN 15 AND 240),
    duracion_min_min     INTEGER     NOT NULL DEFAULT 60,
    duracion_max_min     INTEGER     NOT NULL DEFAULT 120,
    estado               VARCHAR(20)  NOT NULL DEFAULT 'ACTIVA' CHECK (estado IN ('ACTIVA', 'MANTENIMIENTO', 'INACTIVA')),
    descripcion          TEXT,
    orden                INTEGER      NOT NULL DEFAULT 0,
    version              BIGINT       NOT NULL DEFAULT 0,
    creado_en            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    creado_por           BIGINT,
    actualizado_por      BIGINT,
    CONSTRAINT ck_canchas_duraciones CHECK (duracion_max_min >= duracion_min_min AND duracion_min_min >= duracion_bloque_min)
);
CREATE UNIQUE INDEX uq_canchas_sede_nombre ON canchas (sede_id, lower(nombre));
CREATE INDEX ix_canchas_sede_estado ON canchas (sede_id, estado);

CREATE TABLE cancha_fotos (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cancha_id BIGINT       NOT NULL REFERENCES canchas (id),
    archivo   VARCHAR(100) NOT NULL,
    orden     INTEGER      NOT NULL DEFAULT 0,
    creado_en TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_cancha_fotos_cancha ON cancha_fotos (cancha_id);

-- Horario propio de una cancha: si existe, reemplaza al de la sede para esa cancha.
CREATE TABLE cancha_horarios (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cancha_id       BIGINT   NOT NULL REFERENCES canchas (id),
    dia_semana      INTEGER NOT NULL CHECK (dia_semana BETWEEN 1 AND 7),
    cerrado         BOOLEAN  NOT NULL DEFAULT FALSE,
    minuto_apertura INTEGER CHECK (minuto_apertura BETWEEN 0 AND 1439),
    minuto_cierre   INTEGER CHECK (minuto_cierre BETWEEN 1 AND 1440),
    CONSTRAINT uq_cancha_horarios UNIQUE (cancha_id, dia_semana),
    CONSTRAINT ck_cancha_horarios_rango CHECK (
        cerrado OR (minuto_apertura IS NOT NULL AND minuto_cierre IS NOT NULL AND minuto_cierre > minuto_apertura))
);

-- Canchas que dependen de otras: una cancha compuesta (ej. fútbol 8) se forma con varias
-- componentes (ej. dos de fútbol 5). Si cualquiera está ocupada, la compuesta no está disponible y viceversa.
CREATE TABLE cancha_composicion (
    cancha_compuesta_id  BIGINT NOT NULL REFERENCES canchas (id),
    cancha_componente_id BIGINT NOT NULL REFERENCES canchas (id),
    PRIMARY KEY (cancha_compuesta_id, cancha_componente_id),
    CONSTRAINT ck_cancha_composicion_distintas CHECK (cancha_compuesta_id <> cancha_componente_id)
);
CREATE INDEX ix_cancha_composicion_componente ON cancha_composicion (cancha_componente_id);

CREATE TABLE tarifas (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cancha_id       BIGINT   NOT NULL REFERENCES canchas (id),
    dias_semana     SMALLINT[] NOT NULL DEFAULT '{}',
    minuto_inicio   INTEGER NOT NULL CHECK (minuto_inicio BETWEEN 0 AND 1439),
    minuto_fin      INTEGER NOT NULL CHECK (minuto_fin BETWEEN 1 AND 1440),
    aplica_festivo  BOOLEAN  NOT NULL DEFAULT FALSE,
    precio_hora     INTEGER  NOT NULL CHECK (precio_hora >= 0),
    vigente_desde   DATE,
    vigente_hasta   DATE,
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en  TIMESTAMPTZ NOT NULL DEFAULT now(),
    creado_por      BIGINT,
    actualizado_por BIGINT,
    CONSTRAINT ck_tarifas_rango CHECK (minuto_fin > minuto_inicio),
    CONSTRAINT ck_tarifas_vigencia CHECK (vigente_hasta IS NULL OR vigente_desde IS NULL OR vigente_hasta >= vigente_desde),
    -- una franja normal debe indicar días; una de festivo se aplica solo en festivos
    CONSTRAINT ck_tarifas_dias CHECK (aplica_festivo OR cardinality(dias_semana) > 0)
);
CREATE INDEX ix_tarifas_cancha ON tarifas (cancha_id);

CREATE TABLE festivos (
    id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    fecha  DATE         NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL
);

INSERT INTO configuracion (clave, valor, tipo, descripcion) VALUES
    ('modalidades_permitidas', '5,6,7,8,9,11', 'TEXTO', 'Modalidades de fútbol que se pueden asignar a una cancha (jugadores por equipo, separados por coma)');
