-- V15: identificación por @lid (ADR 0018). Por privacidad, WhatsApp a veces no envía el teléfono de quien escribe
-- sino un identificador anónimo y estable (@lid). Se guarda solo la parte numérica.

-- Un cliente vincula su @lid con el código que se envía al correo de su cuenta.
ALTER TABLE usuarios ADD COLUMN whatsapp_lid VARCHAR(40);
CREATE UNIQUE INDEX uq_usuarios_whatsapp_lid ON usuarios (whatsapp_lid) WHERE whatsapp_lid IS NOT NULL;

-- Quien reserva sin cuenta desde un @lid queda atado a él. Su teléfono es solo de contacto (no verificado), por eso
-- estos registros nunca se reutilizan al buscar clientes de mostrador por teléfono.
ALTER TABLE clientes_mostrador ADD COLUMN whatsapp_lid VARCHAR(40);
CREATE UNIQUE INDEX uq_clientes_mostrador_whatsapp_lid ON clientes_mostrador (whatsapp_lid) WHERE whatsapp_lid IS NOT NULL;

-- Un código de vinculación se pide desde un teléfono o desde un @lid (con el correo de la cuenta).
ALTER TABLE verificaciones_telefono ALTER COLUMN telefono DROP NOT NULL;
ALTER TABLE verificaciones_telefono ADD COLUMN whatsapp_lid VARCHAR(40);
ALTER TABLE verificaciones_telefono ADD CONSTRAINT ck_verificaciones_origen CHECK ((telefono IS NULL) <> (whatsapp_lid IS NULL));
CREATE INDEX ix_verificaciones_telefono_lid ON verificaciones_telefono (whatsapp_lid, creado_en);
