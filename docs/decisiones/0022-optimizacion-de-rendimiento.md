# ADR 0022: Optimización de rendimiento

- Estado: aceptada (rama `perf/optimizacion`)
- Contexto: en el servidor la página se sentía lenta y pesada, sobre todo en celulares. Se midió antes de tocar nada (Lighthouse en móvil con 4G lenta y CPU 4× más lenta, contra el `docker compose` local) y se evaluó cada propuesta recibida: se aplicó lo que la medición justificaba y se descartó, con motivo, lo demás.
- Resultado en la portada (móvil, 4G lenta): rendimiento 60 → 91; primer contenido 5,9 s → 2,0 s; contenido más grande 9,3 s → 3,3 s; peso 1.550 KB → 497 KB. Reservar 77 → 83 (LCP 6,0 → 4,2 s), Sedes 83 → 89, Torneos 76 → 89, con saltos de diseño (CLS) en 0.

## Qué se aplicó
1. **Fuentes propias** (`@fontsource`, solo subconjunto latino y 7 pesos) con precarga de las dos críticas. Antes, un `@import` de Google Fonts bloqueaba el CSS y sumaba dos orígenes externos.
2. **Imágenes**: todas ya eran WebP; ahora hay variantes responsivas (640 / 1280 / ancho original, recomprimidas) con `srcset`, medidas reales y carga diferida (`ImagenSitio`), y franjas 2:1 para las cabeceras que usan fotos verticales. El hero de 333 KB baja a 15–101 KB según la pantalla; se precarga la del inicio. `scripts/optimizar-imagenes.py` las genera. Las fotos que sube el administrador se reducen a 1600 px y se recomprimen en el navegador antes de subir (el navegador respeta la orientación EXIF; el servidor la perdería).
3. **Caché y compresión**: `/assets` un año `immutable`, `/img` 30 días, `index.html` siempre revalidado; nginx con gzip y conexiones persistentes hacia la API; en producción comprime Caddy (zstd o gzip).
4. **Menos peso de JavaScript**: se eliminaron `recharts` (sin uso), `date-fns` y `date-fns-tz` (reemplazados por `Intl` y el desfase fijo UTC-5 de Bogotá). El código ya se dividía por ruta.
5. **Arranque**: la pantalla de bienvenida esperaba 900 ms por diseño (ahora 400 ms) y toda la app esperaba una petición de renovación de sesión aun para visitantes: ahora solo se pide si en ese navegador hubo una sesión (aviso en `localStorage`, sin datos sensibles).
6. **Costo en celulares**: sin desenfoques en las animaciones de entrada, el halo del hero con degradado en vez de un blur de 140 px, y sin la capa de grano en pantallas pequeñas.
7. **Esqueletos de carga** con la forma del contenido (página, tabla, tarjetas, lista) y alto mínimo para que el pie de página (muy alto) no «salte».
8. **Datos**: búsqueda con retraso de 350 ms dentro de los hooks de clientes, reservas, equipos y jugadores; la campana ya no pide la lista a visitantes ni con el desplegable cerrado; actualización optimista con reversión y aviso en notificaciones y favoritas.
9. **Backend**: se corrigió una N+1 real en sedes y canchas (las vistas hacían 3 consultas por cancha en cada visita a la portada) con carga por lotes y `default_batch_fetch_size`, y una prueba (`ConsultasEficientesIT`, con un contador de sentencias) que falla si vuelve a ocurrir. Tiempos límite de SMTP (antes infinitos) y de la conexión a la base (5 s), grupo acotado para tareas `@Async`, apagado ordenado, y `/actuator/health` ya no marca la API caída por no tener servidor de correo.
10. **Observabilidad**: registro de toda petición lenta (>800 ms) y de toda sentencia SQL lenta (>250 ms); trazas OpenTelemetry opcionales (apagadas por defecto) con Jaeger en el perfil `observabilidad` (`docker compose --profile observabilidad up -d`, `OTEL_ACTIVADO=true`, http://localhost:16686).

## Qué se evaluó y no se aplicó (y por qué)
- **CDN**: el mayor beneficio (peso y latencia de estáticos) ya se logró con imágenes livianas y caché de un año; una CDN además exige mover los servidores de nombres de `alanpc.tech`, que se comparte con el portafolio. Es la mejor opción si el sitio crece: Cloudflare gratuito. Decisión del dueño del dominio.
- **Brotli**: Caddy solo trae gzip y zstd (brotli pide un plugin propio). Medido: zstd 116 KB y gzip 115 KB para el JavaScript principal; brotli ahorraría ~12 % (~15 KB). No compensa mantener una compilación de Caddy a medida.
- **Cola de trabajos externa** (Redis, RabbitMQ): el correo y los avisos de WhatsApp ya corrían en segundo plano (`@Async`) y no hay tareas lentas pendientes. Un broker suma RAM, un servicio más que operar y un punto de falla. Si algún día hay envíos masivos, el paso lógico es una tabla de salida (outbox) en PostgreSQL.
- **Paginar todas las listas**: ya estaban paginadas las que crecen sin límite (reservas, clientes, notificaciones, auditoría) y los autocompletados tienen tope. El resto está acotado por el negocio, y paginarlo empeoraría la experiencia (listas desplegables, tablas de unas pocas filas); solo se puso un tope de 500 a los equipos.
- **UI optimista en reservas, pagos o aprobaciones**: no. Las reglas de negocio (solapamientos, aprobación, precio) las decide el servidor; mostrar un éxito que luego se revierte en esas acciones es peor que esperar 200 ms.
- **«Acercar el servidor a la base de datos»**: ya están en el mismo equipo y la misma red de Docker (<1 ms). Lo que sí importa es la distancia entre el servidor y las personas: al volver a desplegar conviene elegir el centro de datos más cercano a Colombia.
- **División de JavaScript, re-renders, llamadas duplicadas y en paralelo**: se auditaron. Las rutas ya cargaban por separado; el estado usa selectores y no hay contextos pesados; TanStack Query ya elimina duplicados y lanza en paralelo. No había nada que arreglar salvo lo descrito arriba. React Compiler no se justifica.
- **Reemplazar axios por `fetch`** (~13 KB gzip): reescribir la renovación de token, las descargas y el manejo de errores en toda la app por una ganancia pequeña.
- **Caché HTTP de la API pública**: TanStack ya conserva 30 s en el navegador; un `Cache-Control` en el servidor solo añadiría datos viejos tras ediciones del administrador.
- **AVIF**: otro ~25 % sobre WebP, sin herramienta cómoda en el proyecto.

## Consecuencias
- Quien tenía la sesión abierta antes de desplegar este cambio tendrá que iniciar sesión una vez (no existe todavía el aviso en su navegador).
- Si se reemplaza una imagen de `/img` conviene darle otro nombre (caché de 30 días) o volver a correr `scripts/optimizar-imagenes.py`.
- Las pruebas se hicieron con emulación de celular y red lenta (Lighthouse); no se probó en un teléfono físico.
