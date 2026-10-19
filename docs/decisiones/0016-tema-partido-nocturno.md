# ADR 0016: Tema visual «Partido nocturno» y skills de diseño

- Estado: propuesta (rama `feat/rediseno-visual`)
- Contexto: el frontend limpio (ADR 0014) era correcto pero genérico: verde esmeralda, Inter, tarjetas iguales con ícono y poco carácter para una exposición. Se pidió un rediseño completo, más llamativo, con imágenes, animaciones y pantallas de carga, aplicando skills de diseño de terceros. Antes se probó la paleta en Figma (archivo «Goltime – Tema Partido Nocturno»).
- Decisiones:
  1. **Idea**: una cancha sintética de noche bajo los reflectores. Azul medianoche (`noche` #0b1220) como fondo de las secciones fuertes y lima como la luz.
  2. **Dos limas con roles distintos**: `volt` (#84cc16) para la acción principal de las páginas públicas, siempre con texto `noche` encima; `primario` (#4d7c0f) para estados activos, enlaces y selecciones dentro de la app, porque admite texto blanco y se lee sobre blanco (contraste ≥ 4,5:1). El lima vivo con texto blanco no se usa.
  3. **Tipografía**: Barlow Condensed para títulos (aire de marcador deportivo, en mayúsculas) y Barlow para el texto. Reemplaza a Plus Jakarta Sans e Inter.
  4. **Logo nuevo**: el círculo central de la cancha con agujas de reloj adentro (*gol* + *time*) y la línea media. Con las agujas girando es también el indicador de carga.
  5. **Movimiento**: pantalla de arranque en `index.html` (se pinta antes de cargar la app), barra de carga ligada a React Query, transición entre páginas, revelado al hacer scroll con `IntersectionObserver` (el contenido es visible sin JS), paralaje con `animation-timeline: view()` donde el navegador lo soporta, y una cinta animada en la portada. Todo respeta `prefers-reduced-motion`. Solo se animan `transform`, `opacity`, `filter` y `clip-path`.
  6. **Imágenes**: 8 fotos libres de Unsplash en `frontend/public/img` (WebP, ~2,6 MB en total), con crédito en el pie. En sedes y canchas se usan las fotos reales cargadas en el sistema.
  7. **Botones redondos (píldora)**, superficies separadas por tono y no por sombra; la sombra queda para lo que flota (menús, diálogos).
  8. **Sin emojis como íconos**: los eventos del partido usan íconos dibujados (balón, tarjetas).
  9. **Skills instaladas localmente (no versionadas)**: Impeccable, las de Emil Kowalski (diseño e interfaz animada), taste-skill, css-animations, ui-animation y animation-designer. Se revisaron antes de instalarlas; sus scripts (que descargan binarios, abren Chrome o instalan hooks) no se ejecutaron. animation-designer es AGPL-3.0: solo son instrucciones para la IA, no se distribuye con la aplicación.
- Consecuencias: el panel de administración hereda tokens, componentes y logo, con barra lateral oscura y un tablero con cabecera fotográfica. La regla de que el frontend no calcula precios ni disponibilidad se mantiene: la portada solo muestra el horario publicado de cada sede (abierta ahora o no) y el precio base más bajo que ya entrega el backend.
