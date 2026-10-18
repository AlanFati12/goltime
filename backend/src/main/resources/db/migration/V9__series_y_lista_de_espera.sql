-- V9: reservas recurrentes (series creadas por el admin) y lista de espera de horarios ocupados.

CREATE TABLE series_reservas (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cancha_id            BIGINT      NOT NULL REFERENCES canchas (id),
    cliente_id           BIGINT REFERENCES usuarios (id),
    cliente_mostrador_id BIGINT REFERENCES clientes_mostrador (id),
    dia_semana           INTEGER     NOT NULL CHECK (dia_semana BETWEEN 1 AND 7),
    minuto_inicio        INTEGER     NOT NULL CHECK (minuto_inicio BETWEEN 0 AND 1439),
    duracion_min         INTEGER     NOT NULL CHECK (duracion_min > 0),
    semanas_pedidas      INTEGER     NOT NULL CHECK (semanas_pedidas BETWEEN 2 AND 26),
    nombre_equipo        VARCHAR(100),
    creada_por           BIGINT REFERENCES usuarios (id),
    creada_en            TIMESTAMPTZ NOT NULL DEFAULT now()
);
ALTER TABLE reservas ADD CONSTRAINT fk_reservas_serie FOREIGN KEY (serie_id) REFERENCES series_reservas (id);
CREATE INDEX ix_reservas_serie ON reservas (serie_id) WHERE serie_id IS NOT NULL;

-- Un cliente pide que le avisen si un horario ocupado se libera (por cancelación, rechazo o expiración).
CREATE TABLE lista_espera (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cliente_id    BIGINT      NOT NULL REFERENCES usuarios (id),
    cancha_id     BIGINT      NOT NULL REFERENCES canchas (id),
    inicio        TIMESTAMPTZ NOT NULL,
    fin           TIMESTAMPTZ NOT NULL,
    estado        VARCHAR(12) NOT NULL DEFAULT 'ESPERANDO' CHECK (estado IN ('ESPERANDO', 'NOTIFICADA', 'CUMPLIDA', 'CANCELADA')),
    creado_en     TIMESTAMPTZ NOT NULL DEFAULT now(),
    notificada_en TIMESTAMPTZ,
    CONSTRAINT ck_lista_espera_rango CHECK (fin > inicio)
);
CREATE INDEX ix_lista_espera_cancha ON lista_espera (cancha_id, estado, inicio);
CREATE INDEX ix_lista_espera_cliente ON lista_espera (cliente_id, estado);
CREATE UNIQUE INDEX uq_lista_espera_activa ON lista_espera (cliente_id, cancha_id, inicio, fin) WHERE estado IN ('ESPERANDO', 'NOTIFICADA');
