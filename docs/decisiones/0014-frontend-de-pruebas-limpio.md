# ADR 0014: Frontend de pruebas más limpio

- Estado: aceptada (rama `feat/frontend-limpio`)
- Contexto: el frontend actual es un borrador para probar el backend y se va a reemplazar. Mientras tanto se pidió una versión más limpia, agradable e intuitiva para probar el sistema sin que la interfaz estorbe.
- Decisiones:
  1. **Paleta neutra con un solo color de marca.** Verde esmeralda (`#059669`) solo para acciones y estados; fondos, bordes y textos en grises. Nuevo token `tenue` para rellenos suaves y `info` para avisos. Se quitó el modo oscuro que no se usaba.
  2. **Tipografía:** Inter para el texto y Plus Jakarta Sans para títulos (reemplaza a Poppins).
  3. **Iconos con `lucide-react`** (ya estaba en las dependencias y no se usaba) en menús, botones, estados vacíos y datos.
  4. **Menú lateral claro en administración**, con iconos y cajón en móvil. **Área «Mi cuenta» del cliente con menú lateral propio** (`LayoutCuenta`) que reúne reservas, favoritas, espera, premios, torneos, notificaciones y perfil.
  5. **Piezas comunes nuevas** en `componentes/ui`: `EstadoVacio`, `Estadistica`, `Migas`, `Paginacion`, `Segmentado` (en `Pestanas`), `TituloTarjeta` (en `Tarjeta`) y la clase CSS `.tabla` para todas las tablas. `Modal` gana botón de cerrar y bloquea el desplazamiento del fondo.
  6. **Pantallas de acceso** con un marco común (`TarjetaAcceso`), y detalle de reserva del cliente en dos columnas con las acciones a la derecha.
- Alcance: solo el frontend. No cambió ningún endpoint, regla de negocio ni cálculo; el frontend sigue sin calcular precios, disponibilidad ni posiciones.
