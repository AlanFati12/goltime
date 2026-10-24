-- V14: reservas por WhatsApp (ADR 0018). Nuevo origen WHATSAPP; un cliente sin cuenta (clientes_mostrador)
-- también puede solicitar por WhatsApp, y su solicitud se protege igual contra duplicados.

ALTER TABLE reservas DROP CONSTRAINT reservas_origen_check;
ALTER TABLE reservas ADD CONSTRAINT reservas_origen_check
    CHECK (origen IN ('WEB', 'ADMIN', 'TORNEO', 'RECURRENTE', 'WHATSAPP'));

CREATE UNIQUE INDEX uq_reservas_idempotencia_sin_cuenta ON reservas (cliente_mostrador_id, clave_idempotencia)
    WHERE clave_idempotencia IS NOT NULL AND cliente_mostrador_id IS NOT NULL;

INSERT INTO configuracion (clave, valor, tipo, descripcion) VALUES
    ('whatsapp_bot_activo',                 'true',  'BOOLEANO', 'Si el bot de WhatsApp atiende; apagado responde que está fuera de servicio'),
    ('whatsapp_reservas_sin_cuenta',        'true',  'BOOLEANO', 'Si quien no tiene cuenta puede solicitar reservas por WhatsApp'),
    ('whatsapp_max_solicitudes_sin_cuenta', '2',     'ENTERO',   'Reservas activas que puede tener a la vez alguien sin cuenta que reserva por WhatsApp'),
    ('whatsapp_numero_negocio',             '',      'TEXTO',    'Número de WhatsApp del bot (con indicativo) para mostrarlo en la web');
