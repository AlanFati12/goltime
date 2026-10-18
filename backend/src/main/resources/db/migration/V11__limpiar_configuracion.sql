-- V11: claves de configuración que nunca se usaron (los premios se definen con reglas propias y los descuentos nunca bajan el precio de cero).
DELETE FROM configuracion WHERE clave IN ('dias_reclamar_premio', 'precio_minimo_reserva', 'programa_premios_activo');
UPDATE configuracion SET descripcion = 'Antelación mínima para que un cliente reserve, en horas (el administrador no la tiene)' WHERE clave = 'antelacion_minima_horas';
