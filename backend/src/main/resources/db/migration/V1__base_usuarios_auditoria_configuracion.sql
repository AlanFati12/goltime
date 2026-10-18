-- V1: base del sistema. Extensiones, usuarios, tokens, configuración y bitácora de auditoría.
-- Convenciones: tablas y columnas en español, ids BIGINT identity, fechas en timestamptz (UTC),
-- enums como VARCHAR + CHECK (más fáciles de migrar que enums nativos).

CREATE EXTENSION IF NOT EXISTS btree_gist;

-- ---------------------------------------------------------------------------------------------
-- Usuarios
-- ---------------------------------------------------------------------------------------------
CREATE TABLE usuarios (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombres                 VARCHAR(100)  NOT NULL,
    apellidos               VARCHAR(100)  NOT NULL,
    correo                  VARCHAR(255)  NOT NULL,
    telefono                VARCHAR(30),
    documento               VARCHAR(30),
    clave_hash              VARCHAR(255)  NOT NULL,
    rol                     VARCHAR(20)   NOT NULL CHECK (rol IN ('ADMIN', 'CLIENTE')),
    activo                  BOOLEAN       NOT NULL DEFAULT TRUE,
    correo_verificado       BOOLEAN       NOT NULL DEFAULT FALSE,
    motivo_desactivacion    VARCHAR(500),
    desactivado_por         BIGINT REFERENCES usuarios (id),
    fecha_desactivacion     TIMESTAMPTZ,
    version_token           INTEGER       NOT NULL DEFAULT 0,
    ultimo_login            TIMESTAMPTZ,
    acepto_terminos_en      TIMESTAMPTZ,
    puede_inscribir_equipos BOOLEAN       NOT NULL DEFAULT FALSE,
    version                 BIGINT        NOT NULL DEFAULT 0,
    creado_en               TIMESTAMPTZ   NOT NULL DEFAULT now(),
    actualizado_en          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    creado_por              BIGINT,
    actualizado_por         BIGINT
);

-- El correo es único sin distinguir mayúsculas.
CREATE UNIQUE INDEX uq_usuarios_correo ON usuarios (lower(correo));
CREATE INDEX ix_usuarios_rol_activo ON usuarios (rol, activo);
CREATE INDEX ix_usuarios_desactivado_por ON usuarios (desactivado_por);

-- Clientes "de mostrador": personas sin cuenta para las que el admin crea reservas.
CREATE TABLE clientes_mostrador (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre          VARCHAR(150) NOT NULL,
    telefono        VARCHAR(30),
    creado_en       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    creado_por      BIGINT,
    actualizado_por BIGINT
);

-- ---------------------------------------------------------------------------------------------
-- Tokens
-- ---------------------------------------------------------------------------------------------
-- Refresh tokens opacos: solo se guarda el hash. "familia_id" permite revocar toda la cadena
-- de rotación si se detecta reutilización de un token ya rotado.
CREATE TABLE refresh_tokens (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id       BIGINT       NOT NULL REFERENCES usuarios (id),
    familia_id       UUID         NOT NULL,
    token_hash       VARCHAR(64)  NOT NULL,
    expira_en        TIMESTAMPTZ  NOT NULL,
    revocado         BOOLEAN      NOT NULL DEFAULT FALSE,
    reemplazado_por  BIGINT REFERENCES refresh_tokens (id),
    user_agent       VARCHAR(300),
    ip               VARCHAR(64),
    creado_en        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_refresh_tokens_hash ON refresh_tokens (token_hash);
CREATE INDEX ix_refresh_tokens_usuario ON refresh_tokens (usuario_id);
CREATE INDEX ix_refresh_tokens_familia ON refresh_tokens (familia_id);

-- Tokens de un solo uso para verificar correo y restablecer contraseña (expiran a los 30 min).
CREATE TABLE tokens_verificacion (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id  BIGINT       NOT NULL REFERENCES usuarios (id),
    tipo        VARCHAR(30)  NOT NULL CHECK (tipo IN ('VERIFICAR_CORREO', 'RESTABLECER_CLAVE')),
    token_hash  VARCHAR(64)  NOT NULL,
    expira_en   TIMESTAMPTZ  NOT NULL,
    usado_en    TIMESTAMPTZ,
    creado_en   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_tokens_verificacion_hash ON tokens_verificacion (token_hash);
CREATE INDEX ix_tokens_verificacion_usuario ON tokens_verificacion (usuario_id);

-- ---------------------------------------------------------------------------------------------
-- Configuración general del negocio (clave/valor tipado)
-- ---------------------------------------------------------------------------------------------
CREATE TABLE configuracion (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    clave           VARCHAR(100) NOT NULL UNIQUE,
    valor           VARCHAR(1000) NOT NULL,
    tipo            VARCHAR(20)  NOT NULL CHECK (tipo IN ('ENTERO', 'DECIMAL', 'BOOLEANO', 'TEXTO')),
    descripcion     VARCHAR(300) NOT NULL,
    actualizado_en  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_por BIGINT
);

INSERT INTO configuracion (clave, valor, tipo, descripcion) VALUES
    ('horas_minimas_cancelacion',        '4',    'ENTERO',   'Horas mínimas antes del inicio para cancelar sin penalidad'),
    ('max_reprogramaciones',             '2',    'ENTERO',   'Máximo de reprogramaciones por reserva'),
    ('antelacion_minima_horas',          '1',    'ENTERO',   'Antelación mínima para reservar, en horas'),
    ('antelacion_maxima_dias',           '30',   'ENTERO',   'Antelación máxima para reservar, en días'),
    ('max_reservas_activas_cliente',     '5',    'ENTERO',   'Máximo de reservas activas simultáneas por cliente'),
    ('horas_recordatorio',               '2',    'ENTERO',   'Horas antes del inicio para enviar el recordatorio'),
    ('horas_expiracion_aprobacion',      '12',   'ENTERO',   'Horas que una reserva PENDIENTE espera la aprobación del admin antes de expirar'),
    ('horas_expiracion_antes_inicio',    '2',    'ENTERO',   'Una reserva PENDIENTE también expira esta cantidad de horas antes de su inicio'),
    ('completar_reservas_automaticamente','true', 'BOOLEANO', 'Pasar a COMPLETADA las reservas confirmadas cuya hora de fin ya pasó'),
    ('dias_reclamar_premio',             '15',   'ENTERO',   'Días que tiene el cliente para reclamar un premio antes de que venza'),
    ('precio_minimo_reserva',            '0',    'ENTERO',   'Precio final mínimo (COP) tras aplicar descuentos'),
    ('programa_premios_activo',          'false','BOOLEANO', 'Interruptor global del programa de premios'),
    ('contacto_negocio',                 '',     'TEXTO',    'Datos de contacto del negocio'),
    ('terminos_y_condiciones',           '',     'TEXTO',    'Texto de términos y condiciones y tratamiento de datos (Ley 1581 de 2012)');

-- ---------------------------------------------------------------------------------------------
-- Bitácora general de auditoría (acciones administrativas sobre cualquier entidad)
-- ---------------------------------------------------------------------------------------------
CREATE TABLE audit_log (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    actor_id    BIGINT REFERENCES usuarios (id),
    actor_rol   VARCHAR(20)  NOT NULL CHECK (actor_rol IN ('ADMIN', 'CLIENTE', 'SISTEMA')),
    accion      VARCHAR(60)  NOT NULL,
    entidad     VARCHAR(60)  NOT NULL,
    entidad_id  BIGINT,
    cambios     JSONB,
    motivo      VARCHAR(500),
    ip          VARCHAR(64),
    fecha       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX ix_audit_log_entidad ON audit_log (entidad, entidad_id);
CREATE INDEX ix_audit_log_fecha ON audit_log (fecha DESC);
CREATE INDEX ix_audit_log_actor ON audit_log (actor_id);
