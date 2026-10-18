# Registro del uso de IA

Apartado exigido por la rúbrica. Se actualiza al cierre de cada fase.

## Herramienta y modelo

| Herramienta | Modelo | Uso |
|---|---|---|
| Claude Code (app de escritorio de Claude) | Claude Sonnet 5.5 | Diseño de la arquitectura, generación de código (backend Spring Boot y frontend React), pruebas, migraciones, Dockerización y documentación |

## Cómo se integró en el proceso

1. Un **prompt maestro** definió el producto, el stack, las reglas de negocio, el modelo de datos y las fases. La IA debía preguntar antes de construir.
2. La IA hizo 15 preguntas de alcance y el equipo respondió con opciones. Las respuestas se consolidaron en [../propuesta-inicial.md](../propuesta-inicial.md), que el humano aprobó antes de escribir código.
3. Desarrollo **por fases**; cada una termina con compilación, pruebas pasando y `docker compose up` funcionando.
4. Las decisiones relevantes quedan como ADR en [../decisiones/](../decisiones/).
5. El humano revisa, aprueba cada fase y decide cambios de alcance.

## Archivos

- [prompts.md](prompts.md): prompts relevantes y su resultado
- [reglas-y-controles.md](reglas-y-controles.md): reglas dadas a la IA, controles humanos, errores detectados
- [agentes-y-skills.md](agentes-y-skills.md): herramientas, agentes y skills usados

## Bitácora por fase

### Fase 0: Base (18/oct/2026)

- Monorepo `backend/` y `frontend/`, repositorio git local (ramas `main` y `develop`).
- Backend: Spring Boot 3.5.16 con Java 21, módulos hexagonales, seguridad JWT base (resource server HS256, roles, ProblemDetail), auditoría JPA, Flyway V1, Swagger, ArchUnit. 12 pruebas pasando, incluida una de integración con PostgreSQL real (Testcontainers).
- Frontend: Vite + React + TypeScript + Tailwind 4 con tokens de diseño, cliente Axios con renovación de token, sesión en memoria, rutas protegidas, página de inicio que consulta la API. 7 pruebas pasando.
- Infraestructura: Docker Compose (db, api, web con Nginx, mailpit), workflows de GitHub Actions (escritos, sin subir), `.env.example`.
- Documentación: README, reglas de trabajo del proyecto, 5 ADR, modelo de datos.
- Qué se corrigió: la IA usó primero un import de Jackson 3 que no corresponde a Spring Boot 3.5 (se detectó al compilar) y una prueba de frontend no limpiaba el DOM entre casos (se detectó al ejecutarla).

### Fase 1: Autenticación y usuarios (18/oct/2026)

- Backend: registro, ingreso, refresh con rotación y detección de reutilización, logout, recuperación de contraseña y verificación de correo por correo (Mailpit), límite de tasa (Bucket4j) y bloqueo temporal, validación de `version_token` en cada petición, perfil y cambio de contraseña, administración de clientes (listar, desactivar con previsualización y confirmación, reactivar, habilitar inscripción de equipos), creación de administradores y administrador inicial desde variables de entorno, auditoría en `audit_log`.
- Frontend: formularios de registro, ingreso, olvidé/restablecer contraseña y verificación (React Hook Form + Zod, indicador de fortaleza), rehidratación de sesión con la cookie de refresh, perfil, panel de administración con clientes y administradores, modal de desactivación con previsualización de conflictos.
- Pruebas: 62 en backend (dominio, ArchUnit, 5 clases de integración con PostgreSQL real que cubren rotación, reutilización de refresh, tokens falsificados, IDOR de roles, desactivación inmediata y auditoría) y 16 en frontend. Verificado además de punta a punta por curl a través de Nginx y en el navegador.
- Qué se corrigió: los repositorios Spring Data no se detectan si son interfaces anidadas (se movieron a nivel superior); varias pruebas tenían errores propios (token con `iat` posterior a `exp`, correo con espacios que la validación rechaza); un botón anidado dentro de un enlace en el encabezado.
- Pendiente para fases siguientes: notificar por correo o in-app al cliente cuando el admin lo desactiva (Fase 3, con notificaciones), reemplazar `ReservasDeUsuarioSinModulo` por la implementación real (Fase 3), estadísticas y reservas del perfil.

### Fase 2: Sedes, canchas, tarifas, horarios y festivos (18/oct/2026)

- Backend: migraciones V3 y V4; dominio puro (`HorarioSemanal`, `Cancha`, `Tarifa`, `CalculadoraPrecio` con franjas, festivos y proporción por tramos); servicios de sedes, canchas, tarifas, festivos, fotos y cotización; persistencia JPA con arreglos y JSONB de PostgreSQL; endpoints públicos y de administración; almacenamiento de fotos que valida la firma del archivo; canchas dependientes con sus reglas; cambio de estado con previsualización de conflictos; auditoría de cada cambio.
- Frontend: catálogo público (sedes, detalle, cancha con galería, tarifas, horario y calculadora de precio), y administración de sedes, canchas (dependencias, estado, tarifas, horario propio, fotos) y festivos.
- Pruebas: 107 en backend (25 de dominio nuevas más 20 de integración de este módulo) y 29 en frontend. Verificado además en Docker con datos de demostración y en el navegador.
- Qué se corrigió: los tipos `SMALLINT` no coincidían con los `int` de las entidades (lo detectó `ddl-auto=validate` al arrancar, y se alinearon los tipos en la migración aún no publicada); dos pruebas esperaban una respuesta distinta a la real (el firewall de Spring Security devuelve 400 ante barras codificadas y un filtro JsonPath devuelve arreglos); un texto de botón mal redactado ("Pasar a en mantenimiento").
- Pendiente: el calendario de disponibilidad y las reservas (Fase 3) usarán estas reglas; los adaptadores provisionales `ReservasDeEspacioSinModulo` y `ReservasDeUsuarioSinModulo` se reemplazan allí.

### Fase 3: Disponibilidad y reservas del cliente (18/oct/2026)

- Backend: migración V5; dominio de reservas con máquina de estados y políticas (cancelación tardía, máximo de reprogramaciones, expiración 12 h o 2 h antes); disponibilidad pública con canchas compuestas; solicitud con idempotencia y anti-solapamiento en la base de datos; cancelación, reprogramación con previsualización de precio, aprobación y rechazo; historial de cambios; tareas programadas (expirar, completar, recordar); cancelación en cascada al desactivar sedes, canchas o clientes; módulo de notificaciones in-app.
- Frontend: página de reserva (grilla de horarios, próximos 7 días, filtros, resumen con precio calculado por el servidor), mis reservas con pestañas, detalle con línea de tiempo y reprogramación, comprobante imprimible, campana de notificaciones y panel de aprobación del admin.
- Pruebas: 178 en backend (29 de dominio y 42 de integración nuevas, con prueba de concurrencia de 8 clientes y de doble clic) y 41 en frontend. Verificado además en Docker y en el navegador (solicitud, aprobación y notificación).
- Qué se corrigió: la prueba unitaria de `Reserva.rechazar` destapó un error real (el estado cambiaba antes de validar el motivo); un ciclo de beans entre sedes y reservas (resuelto con `@Lazy`); un mock que reemplazaba un adaptador que implementaba dos puertos (se separó en dos adaptadores); una grilla que dejaba pulsar horarios del día anterior mientras cargaba el nuevo.
- Pendiente: crear reservas a nombre de clientes y de mostrador, agenda, bloqueos de horario, pagos y auditoría completa (Fase 4); correos de confirmación o rechazo y .ics (Fase 8).
