# ADR 0010: Estadísticas del negocio

- Estado: aceptada (Fase 6)
- Decisiones:
  1. **Modelo de lectura en SQL.** El módulo `estadisticas` no tiene tablas propias: agrega con SQL sobre `reservas`, `pagos`, `ocupaciones_cancha` y los horarios. Es de solo lectura, vive detrás de un puerto (`Estadisticas.Consultas`) y todos los números se calculan en el backend; el frontend solo los dibuja.
  2. **Ingresos reales = COMPLETADA** (regla 6), excluyendo el origen `TORNEO`. Se muestran junto a dos cifras que no son lo mismo: **recaudado** (pagos menos reembolsos con fecha dentro del periodo, aunque la reserva sea de otra fecha) y **por cobrar** (lo que falta de las reservas completadas). Un periodo pertenece a las reservas que **empiezan** dentro de él, en hora de Bogotá.
  3. **Ocupación** = minutos de reservas `CONFIRMADA`, `COMPLETADA` o `NO_ASISTIO` (la cancha estuvo apartada) y partidos de torneo, recortados al periodo, sobre los minutos que la cancha estuvo abierta según su horario efectivo (el propio de la cancha si lo tiene, si no el de la sede). Las canchas inactivas no cuentan. Una cancha compuesta y sus componentes se miden por separado; es una métrica por cancha, no por metro cuadrado.
  4. **Comparación automática.** El resumen incluye el periodo anterior de igual duración y la variación porcentual (omitida si el periodo anterior vale cero, para no inventar un infinito).
  5. **Serie sin huecos.** La serie por día, semana (desde el lunes) o mes devuelve también las cubetas sin movimiento, así las gráficas no deforman el tiempo. El periodo está acotado a 366 días.
  6. **Horas pico.** Cada reserva cuenta en todas las horas que cubre (una de 19:00 a 21:00 suma a las 19 y a las 20), por día de la semana ISO. No incluye torneos: mide la demanda de clientes.
  7. **CSV seguro.** `GET /admin/estadisticas/exportar` entrega resumen, serie y detalle por cancha en UTF-8 con BOM; la neutralización de fórmulas se movió a `compartido.dominio.FormatoCsv` y la comparten reservas y estadísticas.
  8. **Gráficas.** Una sola serie en el verde de la marca, barras delgadas con extremo redondeado, mapa de calor de una sola tonalidad, descripción accesible (`role="img"`) y vista de tabla con los mismos datos, sin librerías externas.
  9. **Pendiente.** Una caché de corta duración de estas consultas si el volumen lo pide; hoy cada consulta es una agregación indexada sobre `reservas(estado, inicio)` y `pagos(fecha)`.
