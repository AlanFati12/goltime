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
