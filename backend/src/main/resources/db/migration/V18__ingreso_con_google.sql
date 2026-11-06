-- V18: el único acceso es con Google (ADR 0023).
-- La cuenta se identifica por el `sub` que entrega Google (no cambia aunque la persona cambie de correo).
ALTER TABLE usuarios ADD COLUMN google_sub VARCHAR(64);
CREATE UNIQUE INDEX uq_usuarios_google_sub ON usuarios (google_sub) WHERE google_sub IS NOT NULL;

-- Ya no hay contraseñas: se elimina el hash guardado. Tampoco hay verificación de correo ni recuperación por correo
-- (Google ya confirma el correo), así que los tokens de ese flujo dejan de existir.
ALTER TABLE usuarios DROP COLUMN clave_hash;
DROP TABLE IF EXISTS tokens_verificacion;
