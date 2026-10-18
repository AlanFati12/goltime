-- V7: torneos. Equipos y jugadores reutilizables, inscripciones y plantillas por torneo, fases, grupos,
-- partidos (con avance automático en llaves) y eventos de partido.

CREATE TABLE equipos (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre                 VARCHAR(100) NOT NULL,
    color                  VARCHAR(20),
    escudo                 VARCHAR(100),
    delegado_nombre        VARCHAR(150),
    delegado_telefono      VARCHAR(30),
    usuario_responsable_id BIGINT REFERENCES usuarios (id),
    activo                 BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    creado_por             BIGINT,
    actualizado_por        BIGINT
);
CREATE UNIQUE INDEX uq_equipos_nombre ON equipos (lower(nombre));

CREATE TABLE jugadores (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombres         VARCHAR(100) NOT NULL,
    apellidos       VARCHAR(100) NOT NULL,
    documento       VARCHAR(30),
    posicion        VARCHAR(30),
    usuario_id      BIGINT REFERENCES usuarios (id),
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    creado_por      BIGINT,
    actualizado_por BIGINT
);
CREATE INDEX ix_jugadores_usuario ON jugadores (usuario_id);

-- Plantilla habitual de un equipo; al inscribirlo en un torneo se copia a plantillas_torneo.
CREATE TABLE equipo_jugadores (
    equipo_id  BIGINT  NOT NULL REFERENCES equipos (id),
    jugador_id BIGINT  NOT NULL REFERENCES jugadores (id),
    dorsal     INTEGER CHECK (dorsal BETWEEN 0 AND 99),
    activo     BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (equipo_id, jugador_id)
);
CREATE UNIQUE INDEX uq_equipo_jugadores_dorsal ON equipo_jugadores (equipo_id, dorsal) WHERE activo AND dorsal IS NOT NULL;

CREATE TABLE torneos (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre                  VARCHAR(150) NOT NULL,
    descripcion             TEXT,
    sede_id                 BIGINT       NOT NULL REFERENCES sedes (id),
    jugadores_por_equipo    INTEGER      NOT NULL CHECK (jugadores_por_equipo BETWEEN 2 AND 11),
    formato                 VARCHAR(30)  NOT NULL CHECK (formato IN ('LIGA', 'ELIMINACION_DIRECTA', 'GRUPOS_Y_ELIMINACION')),
    estado                  VARCHAR(20)  NOT NULL DEFAULT 'BORRADOR' CHECK (estado IN ('BORRADOR', 'INSCRIPCIONES', 'EN_CURSO', 'FINALIZADO', 'CANCELADO')),
    fecha_inicio            DATE,
    fecha_fin               DATE,
    cupo_equipos            INTEGER      CHECK (cupo_equipos >= 2),
    valor_inscripcion       INTEGER      NOT NULL DEFAULT 0 CHECK (valor_inscripcion >= 0),
    premios_texto           TEXT,
    reglamento              TEXT,
    modo_emparejamiento     VARCHAR(20)  NOT NULL DEFAULT 'ALEATORIO' CHECK (modo_emparejamiento IN ('MANUAL', 'ALEATORIO', 'SEMBRADO')),
    ida_y_vuelta            BOOLEAN      NOT NULL DEFAULT FALSE,
    tercer_puesto           BOOLEAN      NOT NULL DEFAULT FALSE,
    num_grupos              INTEGER      CHECK (num_grupos >= 1),
    clasificados_por_grupo  INTEGER      CHECK (clasificados_por_grupo >= 1),
    puntos_victoria         INTEGER      NOT NULL DEFAULT 3,
    puntos_empate           INTEGER      NOT NULL DEFAULT 1,
    puntos_derrota          INTEGER      NOT NULL DEFAULT 0,
    criterios_desempate     JSONB        NOT NULL DEFAULT '["PUNTOS","DIFERENCIA_GOL","GOLES_A_FAVOR","ENFRENTAMIENTO_DIRECTO","JUEGO_LIMPIO","SORTEO"]'::jsonb,
    marcador_wo_goles       INTEGER      NOT NULL DEFAULT 3 CHECK (marcador_wo_goles >= 1),
    campeon_id              BIGINT REFERENCES equipos (id),
    subcampeon_id           BIGINT REFERENCES equipos (id),
    tercero_id              BIGINT REFERENCES equipos (id),
    version                 BIGINT       NOT NULL DEFAULT 0,
    creado_en               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    creado_por              BIGINT,
    actualizado_por         BIGINT,
    CONSTRAINT ck_torneos_fechas CHECK (fecha_fin IS NULL OR fecha_inicio IS NULL OR fecha_fin >= fecha_inicio)
);
CREATE INDEX ix_torneos_estado ON torneos (estado);

CREATE TABLE torneo_canchas (
    torneo_id BIGINT NOT NULL REFERENCES torneos (id),
    cancha_id BIGINT NOT NULL REFERENCES canchas (id),
    PRIMARY KEY (torneo_id, cancha_id)
);

CREATE TABLE inscripciones_torneo (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    torneo_id  BIGINT      NOT NULL REFERENCES torneos (id),
    equipo_id  BIGINT      NOT NULL REFERENCES equipos (id),
    estado     VARCHAR(20) NOT NULL DEFAULT 'APROBADA' CHECK (estado IN ('SOLICITADA', 'APROBADA', 'RECHAZADA', 'RETIRADA')),
    semilla    INTEGER     CHECK (semilla >= 1),
    creado_en  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_inscripcion UNIQUE (torneo_id, equipo_id)
);

-- Un jugador no puede estar en dos equipos del mismo torneo ni repetir dorsal en su equipo.
CREATE TABLE plantillas_torneo (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    inscripcion_id BIGINT  NOT NULL REFERENCES inscripciones_torneo (id),
    torneo_id      BIGINT  NOT NULL REFERENCES torneos (id),
    jugador_id     BIGINT  NOT NULL REFERENCES jugadores (id),
    dorsal         INTEGER CHECK (dorsal BETWEEN 0 AND 99),
    CONSTRAINT uq_plantilla_jugador UNIQUE (torneo_id, jugador_id)
);
CREATE UNIQUE INDEX uq_plantilla_dorsal ON plantillas_torneo (inscripcion_id, dorsal) WHERE dorsal IS NOT NULL;

CREATE TABLE fases (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    torneo_id BIGINT      NOT NULL REFERENCES torneos (id),
    tipo      VARCHAR(20) NOT NULL CHECK (tipo IN ('GRUPOS', 'LIGA', 'ELIMINATORIA')),
    orden     INTEGER     NOT NULL,
    nombre    VARCHAR(60) NOT NULL
);
CREATE INDEX ix_fases_torneo ON fases (torneo_id);

CREATE TABLE grupos (
    id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    fase_id BIGINT      NOT NULL REFERENCES fases (id),
    nombre  VARCHAR(30) NOT NULL,
    CONSTRAINT uq_grupo_nombre UNIQUE (fase_id, nombre)
);

CREATE TABLE grupo_equipos (
    grupo_id       BIGINT NOT NULL REFERENCES grupos (id),
    inscripcion_id BIGINT NOT NULL REFERENCES inscripciones_torneo (id),
    PRIMARY KEY (grupo_id, inscripcion_id)
);

CREATE TABLE partidos (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    torneo_id             BIGINT      NOT NULL REFERENCES torneos (id),
    fase_id               BIGINT      NOT NULL REFERENCES fases (id),
    grupo_id              BIGINT REFERENCES grupos (id),
    ronda                 INTEGER     NOT NULL,
    numero_llave          INTEGER     NOT NULL,
    nombre_ronda          VARCHAR(40),
    local_id              BIGINT REFERENCES inscripciones_torneo (id),
    visitante_id          BIGINT REFERENCES inscripciones_torneo (id),
    -- cuando el rival aún no se conoce (ej. clasificados de grupos): texto a mostrar y referencia a resolver
    etiqueta_local        VARCHAR(60),
    etiqueta_visitante    VARCHAR(60),
    ref_local             VARCHAR(20),
    ref_visitante         VARCHAR(20),
    siguiente_partido_id  BIGINT REFERENCES partidos (id),
    lado_en_siguiente     VARCHAR(10) CHECK (lado_en_siguiente IN ('LOCAL', 'VISITANTE')),
    siguiente_perdedor_id BIGINT REFERENCES partidos (id),
    lado_perdedor         VARCHAR(10) CHECK (lado_perdedor IN ('LOCAL', 'VISITANTE')),
    cancha_id             BIGINT REFERENCES canchas (id),
    reserva_id            BIGINT REFERENCES reservas (id),
    inicio                TIMESTAMPTZ,
    duracion_min          INTEGER,
    arbitro               VARCHAR(100),
    estado                VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN
        ('PENDIENTE', 'PROGRAMADO', 'EN_JUEGO', 'FINALIZADO', 'APLAZADO', 'WO', 'BYE')),
    goles_local           INTEGER CHECK (goles_local >= 0),
    goles_visitante       INTEGER CHECK (goles_visitante >= 0),
    penales_local         INTEGER CHECK (penales_local >= 0),
    penales_visitante     INTEGER CHECK (penales_visitante >= 0),
    ganador_id            BIGINT REFERENCES inscripciones_torneo (id),
    es_bye                BOOLEAN     NOT NULL DEFAULT FALSE,
    es_wo                 BOOLEAN     NOT NULL DEFAULT FALSE,
    anulado               BOOLEAN     NOT NULL DEFAULT FALSE,
    figura_jugador_id     BIGINT REFERENCES jugadores (id),
    version               BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT ck_partidos_equipos_distintos CHECK (local_id IS NULL OR visitante_id IS NULL OR local_id <> visitante_id)
);
CREATE INDEX ix_partidos_torneo ON partidos (torneo_id, fase_id, ronda);
CREATE INDEX ix_partidos_grupo ON partidos (grupo_id);
CREATE INDEX ix_partidos_reserva ON partidos (reserva_id);

CREATE TABLE eventos_partido (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    partido_id            BIGINT      NOT NULL REFERENCES partidos (id),
    inscripcion_id        BIGINT      NOT NULL REFERENCES inscripciones_torneo (id),
    jugador_id            BIGINT      NOT NULL REFERENCES jugadores (id),
    tipo                  VARCHAR(20) NOT NULL CHECK (tipo IN ('GOL', 'GOL_PENAL', 'AUTOGOL', 'ASISTENCIA', 'AMARILLA', 'ROJA', 'FALTA')),
    minuto                INTEGER     NOT NULL CHECK (minuto BETWEEN 0 AND 130),
    evento_relacionado_id BIGINT REFERENCES eventos_partido (id),
    observacion           VARCHAR(200),
    creado_en             TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_eventos_partido ON eventos_partido (partido_id, minuto);
CREATE INDEX ix_eventos_jugador ON eventos_partido (jugador_id, tipo);

-- Los partidos de torneo reservan la cancha con una reserva interna (origen TORNEO) que no pertenece a ningún cliente.
ALTER TABLE reservas DROP CONSTRAINT ck_reservas_cliente;
ALTER TABLE reservas ADD CONSTRAINT ck_reservas_cliente CHECK (
    (origen = 'TORNEO' AND cliente_id IS NULL AND cliente_mostrador_id IS NULL)
    OR (origen <> 'TORNEO' AND (cliente_id IS NOT NULL)::int + (cliente_mostrador_id IS NOT NULL)::int = 1));

-- Un documento identifica a un único jugador (se reutiliza entre equipos y torneos).
CREATE UNIQUE INDEX uq_jugadores_documento ON jugadores (documento) WHERE documento IS NOT NULL;
