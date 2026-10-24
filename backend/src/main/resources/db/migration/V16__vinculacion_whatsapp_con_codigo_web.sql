-- V16: vincular el WhatsApp con un código generado en la web (ADR 0018). Aún no se envían correos, así que el
-- código ya no va al correo: el cliente lo genera en su perfil (con sesión iniciada) y se lo envía al bot desde
-- su WhatsApp. Eso prueba a la vez que es dueño de la cuenta y del WhatsApp.

DROP TABLE verificaciones_telefono;

-- Código de un solo uso, vence a los 10 minutos. Solo se guarda su hash.
CREATE TABLE codigos_vinculacion_whatsapp (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id   BIGINT       NOT NULL REFERENCES usuarios (id),
    codigo_hash  VARCHAR(64)  NOT NULL,
    expira_en    TIMESTAMPTZ  NOT NULL,
    usado_en     TIMESTAMPTZ,
    creado_en    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_codigos_vinculacion_hash ON codigos_vinculacion_whatsapp (codigo_hash);
CREATE INDEX ix_codigos_vinculacion_usuario ON codigos_vinculacion_whatsapp (usuario_id, creado_en);

-- Códigos equivocados enviados desde un WhatsApp (teléfono o lid:<id>): limita los intentos de adivinar.
CREATE TABLE intentos_vinculacion_whatsapp (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    remitente   VARCHAR(50)  NOT NULL,
    creado_en   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX ix_intentos_vinculacion_remitente ON intentos_vinculacion_whatsapp (remitente, creado_en);
