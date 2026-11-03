-- V13: el teléfono del cliente es su WhatsApp (ADR 0018). Se guarda en formato E.164 (+573001234567)
-- para que el bot lo reconozca, y se verifica la primera vez que la persona le escribe al bot.

ALTER TABLE usuarios ADD COLUMN telefono_verificado BOOLEAN NOT NULL DEFAULT FALSE;

-- Normaliza los teléfonos existentes que se pueden interpretar sin dudas; los demás quedan como están
-- y se corrigen cuando la persona edite su perfil (el backend ya no acepta otro formato).
CREATE FUNCTION normalizar_telefono_v13(texto TEXT) RETURNS TEXT LANGUAGE sql IMMUTABLE AS $$
    SELECT CASE
        WHEN limpio ~ '^\+[1-9][0-9]{7,14}$' THEN limpio
        WHEN limpio ~ '^00[1-9][0-9]{7,14}$' THEN '+' || substr(limpio, 3)
        WHEN limpio ~ '^3[0-9]{9}$' THEN '+57' || limpio
        WHEN limpio ~ '^573[0-9]{9}$' THEN '+' || limpio
        ELSE texto
    END
    FROM (SELECT regexp_replace(texto, '[\s().-]', '', 'g') AS limpio) AS t
$$;

UPDATE usuarios SET telefono = normalizar_telefono_v13(telefono) WHERE telefono IS NOT NULL;
UPDATE clientes_mostrador SET telefono = normalizar_telefono_v13(telefono) WHERE telefono IS NOT NULL;

DROP FUNCTION normalizar_telefono_v13(TEXT);

-- Varias cuentas pueden escribir el mismo número, pero solo una puede tenerlo verificado.
CREATE UNIQUE INDEX uq_usuarios_telefono_verificado ON usuarios (telefono) WHERE telefono_verificado;
CREATE INDEX ix_usuarios_telefono ON usuarios (telefono);
CREATE INDEX ix_clientes_mostrador_telefono ON clientes_mostrador (telefono);

-- Código de 6 dígitos que se envía al correo de la cuenta para probar que quien escribe por WhatsApp
-- también es dueño de la cuenta. Solo se guarda el hash; vence en 10 minutos y admite 5 intentos.
CREATE TABLE verificaciones_telefono (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id   BIGINT       NOT NULL REFERENCES usuarios (id),
    telefono     VARCHAR(30)  NOT NULL,
    codigo_hash  VARCHAR(64)  NOT NULL,
    intentos     INTEGER      NOT NULL DEFAULT 0 CHECK (intentos >= 0),
    expira_en    TIMESTAMPTZ  NOT NULL,
    usado_en     TIMESTAMPTZ,
    creado_en    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX ix_verificaciones_telefono_numero ON verificaciones_telefono (telefono, creado_en);
CREATE INDEX ix_verificaciones_telefono_usuario ON verificaciones_telefono (usuario_id);
