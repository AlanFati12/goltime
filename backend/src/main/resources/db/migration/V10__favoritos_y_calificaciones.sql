-- V10: canchas favoritas de los clientes y calificaciones de reservas jugadas.

CREATE TABLE favoritos (
    usuario_id BIGINT      NOT NULL REFERENCES usuarios (id),
    cancha_id  BIGINT      NOT NULL REFERENCES canchas (id),
    creado_en  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (usuario_id, cancha_id)
);

-- Una calificación por reserva completada, hecha por quien la jugó.
CREATE TABLE calificaciones (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    reserva_id BIGINT       NOT NULL UNIQUE REFERENCES reservas (id),
    cliente_id BIGINT       NOT NULL REFERENCES usuarios (id),
    cancha_id  BIGINT       NOT NULL REFERENCES canchas (id),
    estrellas  INTEGER      NOT NULL CHECK (estrellas BETWEEN 1 AND 5),
    comentario VARCHAR(500),
    creada_en  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizada_en TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_calificaciones_cancha ON calificaciones (cancha_id, creada_en DESC);
