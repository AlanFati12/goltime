# ADR 0017: Modo oscuro y carga de páginas por rutas

- Estado: propuesta (rama `feat/rediseno-visual`)
- Contexto: el prompt maestro pedía un modo oscuro opcional y nunca se hizo. Se pidió un botón para que cualquier persona alterne entre claro y oscuro. Además, el paquete de JavaScript del frontend pesaba 939 KB en un solo archivo.
- Decisiones:
  1. **Mismos tokens, otros valores.** El modo oscuro redefine las variables de color de ADR 0016 bajo `:root[data-tema='oscuro']`; los componentes no cambian de clases. Paleta: fondo `#080d18`, superficies `#0f1626`, texto `#e6ebf3`, `noche` sube a `#19243d` para que marcadores, cabeceras y pie sigan destacando, y el lima profundo pasa a `#a3e635` para leerse sobre oscuro.
  2. **Tokens nuevos** para lo que cambia de papel entre temas: `invertido` / `sobre-invertido` (lo seleccionado y el botón principal de la app: azul noche en claro, lima en oscuro), `sobre-primario`, y tintes `alerta-claro`, `peligro-claro` e `info-claro` en lugar de colores fijos de Tailwind (`amber-50`, `red-50`, `blue-50`).
  3. **Botón de tema** (sol y luna) en la barra pública y en la del panel. Con View Transitions el tema nuevo se revela en un círculo desde el botón; sin soporte o con movimiento reducido cambia al instante.
  4. **Sin destello.** `public/arranque.js` se carga en el `<head>` y aplica el tema guardado (`localStorage`, clave `goltime:tema`) o, si la persona no ha elegido, el del sistema. Va en un archivo propio porque la CSP de Nginx no permite scripts en línea. El tema es una preferencia del navegador; no se guarda en el servidor.
  5. **Carga por rutas.** Todas las páginas salvo la portada se cargan con `React.lazy` (`lib/perezosa.ts`) dentro de `Suspense`, con el logo animado como espera. El paquete inicial bajó de 939 KB a 391 KB.
- Consecuencias: la primera visita a una página nueva descarga su trozo de código (unos KB); la barra superior y el pie no desaparecen mientras tanto.
