# Prompts relevantes

## 1. Prompt maestro (18/oct/2026)

- **Objetivo:** definir producto, stack, arquitectura, roles, módulos, reglas de negocio, modelo de datos, API, seguridad, frontend, calidad, datos de demo, registro de IA y plan por fases.
- **Prompt:** documento "PROMPT MAESTRO: GOLTIME" (18 secciones). **Pendiente:** guardar el texto íntegro en `docs/ia/prompt-maestro.md` (la IA no lo transcribió para no introducir diferencias; copiarlo tal cual).
- **Resultado:** la IA respondió con 18 preguntas de alcance en vez de escribir código, como pedía la sección 0.
- **Qué se aceptó o corrigió:** se aceptó el flujo de preguntar primero.

## 2. Respuestas a las preguntas de alcance

- **Objetivo:** fijar decisiones antes de construir.
- **Decisiones del equipo:** hexagonal + contenedores separados; un negocio con varias sedes; local con Docker y Mailpit (correo real y VPS de Hostinger después); código en español; las reservas requieren aprobación del admin; 4 h de cancelación y 2 reprogramaciones; completar automáticamente con corrección del admin; canchas dependientes entre sí; solicitud de inscripción a torneos habilitada por el admin por cliente; reservas recurrentes y lista de espera incluidas; una sola persona; identidad visual propuesta por la IA; Spring Boot 3.5.x, Java 21 e IDs BIGINT.
- **Resultado:** [../propuesta-inicial.md](../propuesta-inicial.md).
- **Corrección posterior:** el equipo pidió quitar las fechas por fase; se mantuvo solo el orden de prioridad.

## 3. Aprobación y arranque de la Fase 0

- **Prompt:** "Aprobado. Expiración de pendientes: 12 h o 2 h antes del inicio. Haz git init local y empieza la Fase 0."
- **Resultado:** la Fase 0 descrita en [README.md](README.md).

## Frontend de pruebas más limpio (18/oct/2026)

- **Prompt:** "Se realizó este proyecto que tiene un frontend que cambiaré pero quiero probar la página con un front que se vea más limpio, más agradable e intuitivo, así que por favor revisa las pantallas que hay y modifica todo el frontend […]".
- **Resultado:** nuevo sistema visual y rediseño de todas las pantallas en la rama `feat/frontend-limpio`; ver [ADR 0014](../decisiones/0014-frontend-de-pruebas-limpio.md).
- **Qué se verificó:** `npm test`, `npm run lint` y `npm run build` pasan; revisión visual en el navegador como administrador (escritorio y móvil). Las pantallas solo de cliente (mis reservas, favoritas, premios) no se revisaron con sesión de cliente.

## Rediseño «Partido nocturno» (18/oct/2026)

- **Prompts:** primero se pidió probar en Figma la paleta «Partido nocturno» (Midnight & Volt) en la página de inicio y luego una versión más minimalista. Después: "Descarga e instala en el proyecto las skills de diseño Impeccable y la de Emil Kowalski y añade también taste skill, animation designer, css animations, UI animation, luego de esto rediseña la web completamente […] imágenes o animaciones muy profesionales […] responsive […] pantallas de carga […] logo […] animación de scroll".
- **Resultado:** archivo de Figma con la v1 y la v2 de la portada; skills instaladas localmente (no versionadas); rediseño de todo el frontend en la rama `feat/rediseno-visual`. Ver [ADR 0016](../decisiones/0016-tema-partido-nocturno.md).
- **Controles humanos:** la IA pidió confirmación antes de descargar las skills (repositorio, tamaño y licencia de cada una) y antes de bajar las fotos (archivo, autor y peso). No ejecutó los scripts de las skills.
- **Qué se verificó:** `npm test` (66 pruebas), `npm run lint` sin errores, `npm run build`, y revisión en el navegador de portada, sedes, cancha, reservar, torneos, partido, promociones, acceso y 404 en escritorio (1440 px) y móvil (375 px), sin desbordes horizontales. El panel de administración no se revisó con sesión porque la clave del administrador está en `.env`.
