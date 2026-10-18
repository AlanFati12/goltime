-- V2: el refresh token recuerda si la sesión es persistente ("recordarme") para conservar
-- esa duración al rotarlo.
ALTER TABLE refresh_tokens ADD COLUMN recordar BOOLEAN NOT NULL DEFAULT FALSE;
