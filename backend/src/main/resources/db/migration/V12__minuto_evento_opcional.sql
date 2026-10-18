-- El minuto de un evento de partido pasa a ser opcional: el administrador puede registrar quién marcó o asistió
-- aunque no anote el minuto exacto. Si se indica, sigue limitado a 0..130.
ALTER TABLE eventos_partido ALTER COLUMN minuto DROP NOT NULL;
