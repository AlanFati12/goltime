# ADR 0003: Reservas con aprobación del admin y tabla de ocupaciones

- Estado: aceptada
- Contexto: el negocio quiere que el cliente solicite y el administrador apruebe. Además, una cancha puede depender de otras (por ejemplo, una de fútbol 8 formada por dos de fútbol 5), y la restricción `EXCLUDE` de PostgreSQL solo compara filas de la misma cancha.
- Decisión:
  1. Estados de reserva: `PENDIENTE`, `CONFIRMADA`, `RECHAZADA`, `EXPIRADA`, `CANCELADA`, `COMPLETADA`, `NO_ASISTIO`. Las solicitudes del cliente nacen `PENDIENTE` y retienen el horario. Las creadas por el admin nacen `CONFIRMADA`.
  2. Una reserva `PENDIENTE` expira a las 12 h o 2 h antes del inicio, lo que ocurra primero (configurable en `configuracion`: `horas_expiracion_aprobacion`, `horas_expiracion_antes_inicio`). Un job la pasa a `EXPIRADA` y libera el horario. `RECHAZADA` y `EXPIRADA` también lo liberan.
  3. Tabla `ocupaciones_cancha (reserva_id, cancha_id, rango tstzrange)` con `EXCLUDE USING gist (cancha_id WITH =, rango WITH &&)`. Al crear una reserva se insertan filas para la cancha reservada y para todas las canchas relacionadas por `cancha_composicion`. Bloqueos y partidos de torneo usan el mismo mecanismo. Así la garantía contra solapamientos se mantiene a nivel de base de datos aun con canchas dependientes y peticiones concurrentes.
  4. El admin habilita por cliente la posibilidad de solicitar inscripción a torneos con `usuarios.puede_inscribir_equipos`.
- Se implementa a partir de las Fases 2 y 3. Esta decisión se registra ahora para que las migraciones futuras la respeten.
