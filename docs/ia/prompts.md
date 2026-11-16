# Prompts relevantes

## 1. Prompt maestro (18/oct/2026)

- **Objetivo:** definir producto, stack, arquitectura, roles, módulos, reglas de negocio, modelo de datos, API, seguridad, frontend, calidad, datos de demo, registro de IA y plan por fases.
- **Prompt:** documento "PROMPT MAESTRO: GOLTIME" (18 secciones), guardado íntegro en [prompt-maestro.md](prompt-maestro.md).
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

## Modo oscuro y tareas pendientes (19/oct/2026)

- **Prompt:** "Quiero que crees un botón donde cualquier persona pueda darle y pueda alternar entre versión de modo oscuro y modo claro en esta página […] también revisa si queda alguna tarea o algo pendiente por hacer del proyecto y ponte manos a la obra".
- **Resultado:** modo oscuro con botón en las barras pública y del panel; carga de páginas por rutas (paquete inicial de 939 KB a 391 KB); corrección de esta bitácora (el prompt maestro ya estaba guardado); README al día. Ver [ADR 0017](../decisiones/0017-modo-oscuro-y-carga-por-rutas.md).
- **Qué se detectó:** la CSP de Nginx bloqueaba el script en línea de `index.html`, así que en producción no se aplicaba la clase `js`; se movió a `public/arranque.js`.
- **Qué se verificó:** 66 pruebas, lint sin errores, build, y revisión en <http://localhost:8081> en claro y oscuro (portada, sedes, reservar, torneo, promociones, ingreso), escritorio y móvil; el tema se mantiene al recargar.
- **Revisión antes de fusionar:** se corrigieron tres errores del rediseño (la barra de carga parpadeaba con los refrescos automáticos, el menú móvil no se cerraba al navegar con el logo y la portada ofrecía «Mis premios» al administrador). `mvn verify` falló en 2 de 356 pruebas por un error de las propias pruebas: `diaHabil(n)` corría el domingo al lunes, así que `diaHabil(20)` y `diaHabil(21)` caían el mismo día según el día de la semana en que se corrieran y la segunda reserva chocaba (409). Ahora cuenta días hábiles; 356 de 356 pasan.
- **Pendiente para el equipo:** revisar el panel de administración y las pantallas del cliente con sesión (las claves de prueba están en `.env`), y HTTPS al desplegar en el VPS (ver `docs/seguridad.md`).

## Video promocional para INNOVASOFT IX (16/nov/2026)

- **Prompt:** brief de director creativo para producir de principio a fin un video de 60 s con motion graphics (guion, locución, animación, música, efectos y archivo final). La estructura es la de la categoría «Proyecto de software finalizado» (problema y ODS, solución, prototipo funcional, resultados y cierre), con la paleta «Partido nocturno», el logo real, una voz es-CO, la app grabada funcionando y solo cifras verificadas.
- **Resultado:** proyecto aparte, fuera del repositorio (Remotion + Playwright + Python), con el video final, sus supuestos y créditos, el guion final y los tiempos por palabra. Ver [ADR 0024](../decisiones/0024-video-promocional.md).
- **Controles:**
  - La app se grabó en una copia aislada (`goltime-video`), sin tocar la base de desarrollo, con un administrador de demostración en lugar del correo real.
  - Los teléfonos de los clientes de demostración se enmascararon.
  - Las cifras del video salen de ejecutar las pruebas ese mismo día, no de una búsqueda de texto.
  - Se descargaron los íconos oficiales de los ODS 9 y 8 (un.org); la música y los efectos son sintetizados.
- **Qué se verificó:**
  - `mvn verify`: 413 de 413, BUILD SUCCESS.
  - `npm test`: 102 de 102; lint sin errores; build correcto.
  - Render: 1800 cuadros a 1920×1080 y 30 fps, −14,8 LUFS y pico de −1,8 dBFS.
  - Revisión de fotogramas cada 0,5 s. Se corrigieron un dispositivo que entraba en blanco, el zoom que cortaba texto en el celular, anillos que no se apagaban, un contador que rodaba también el total y el logo de Mailpit en cuadro.
- **Pendiente para el equipo:**
  - Escuchar la pronunciación de «Goltime», «ODS» y «WhatsApp».
  - Confirmar el ODS con el docente.
  - Aclarar la fecha del evento (el brief dice 26/nov y las reglas de trabajo del proyecto, 19/nov).
- **Segunda vuelta (16/nov/2026):** el equipo pidió una voz menos robótica y mostrar el aviso por WhatsApp en vez del correo, que Goltime no usa para eso.
  - **Voz:** sin claves de ElevenLabs ni OpenAI, se probaron las voces «Multilingual» de edge-tts. El equipo escuchó cinco muestras y eligió Brian.
  - **Aviso:** la escena de Mailpit se cambió por un celular con un chat simulado, con el texto real del aviso y el indicador «AVISO SIMULADO».
  - **Ajuste de la voz:** se quitaron comas solo en el texto leído para controlar las pausas de la voz nueva.
  - **Verificación:** render de 1800 cuadros, −15,3 LUFS y pico de −1,7 dBFS, con hojas de contacto revisadas cada 0,5 s.
- **Tercera vuelta (16/nov/2026):** el equipo notó que «Digitalizarlo es innovación» sonaba a otro idioma y que «WhatsApp» sonaba «whatsap-pe»; además pidió quitar «ODS 9» de la voz.
  - **Verificación con Whisper:** se instaló Whisper local (`voz/verificar_voz.py`). Confirmó que esa frase salía en alemán.
  - **Frase nueva:** «Es hora de llevar este servicio a lo digital.» (español al 100 %).
  - **WhatsApp:** se escribe tal cual, porque «guatsáp» alargaba la palabra.
  - **ODS:** se mantiene solo en pantalla, porque la rúbrica lo exige.
