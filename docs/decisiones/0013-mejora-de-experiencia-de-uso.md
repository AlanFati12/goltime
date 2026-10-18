# ADR 0013: Mejora de la experiencia de uso (UX)

- Estado: aceptada (posterior a la Fase 9, rama `feature/mejora-ux`)
- Contexto: tras probar la aplicación se vio que las pantallas mostraban demasiada información a la vez: menú de administración con 17 opciones planas, un inicio de administración que solo era una lista de enlaces con descripciones largas, filtros siempre desplegados y un encabezado público con muchos enlaces.
- Decisiones:
  1. **Mostrar lo necesario y plegar el resto.** Filtros secundarios detrás de «Más filtros» (reservas, reservar, estadísticas); indicadores secundarios detrás de «Más indicadores»; se abren solos cuando hay un filtro activo.
  2. **Menú de administración agrupado por tarea** (Operación, Competencias, Crecimiento, Negocio, Sistema), con contador de solicitudes por aprobar, y cajón desplegable en móvil.
  3. **Inicio de administración como tablero**: saludo, cuatro indicadores (por aprobar, reservas de hoy, ingresos y ocupación de 7 días), las solicitudes por aprobar con el botón Aprobar y las reservas de hoy. Reemplaza la lista de accesos.
  4. **Encabezado público corto**: Sedes, Torneos, Promociones, un botón «Reservar» y un menú de la cuenta que reúne los accesos personales; menú de hamburguesa en móvil. Se retiró del inicio la línea «API conectada».
  5. **Secciones largas en pestañas.** La gestión de un torneo se divide en Equipos, Fixture y Partidos y resultados (abre en la que toca según el estado); la lista de torneos usa las mismas pestañas.
  6. **Piezas comunes nuevas** en `componentes/ui`: `EncabezadoPagina`, `Plegable`, `Pestanas`, `MenuDesplegable`; y `MenuUsuario` en `layout`. Tarjetas con más aire, borde más suave y radios mayores.
- Alcance: solo el frontend; ninguna regla de negocio ni endpoint cambió. El frontend sigue sin calcular nada de negocio: el tablero consume los mismos endpoints de reservas y estadísticas.
- También se corrigió un desbordamiento horizontal del detalle de sede en pantallas de 1024 px, y el doble relleno de las páginas de torneos, promociones y cuenta.
