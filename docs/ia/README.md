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

### Fase 4: Reservas del admin, bloqueos, pagos y auditoría (18/oct/2026)

- Backend: migración V6; reservas creadas por el admin (cliente registrado o de mostrador, política propia); reprogramación con motivo y cambios de estado; pagos, abonos y reembolsos con estados de pago; bloqueos puntuales y recurrentes con previsualización, huella de confirmación y cancelación atómica; agenda por sede; filtros del listado y exportación CSV; consulta de la bitácora de auditoría.
- Frontend: agenda por día y semana, listado con filtros y CSV, detalle con pagos e historial, nueva reserva, bloqueos con revisión de conflictos y auditoría.
- Pruebas: 233 en backend (21 de dominio y 55 de integración nuevas, incluida la reversión completa cuando un bloqueo choca con otro y la huella desactualizada) y 48 en frontend. Verificado además en Docker y en el navegador.
- Qué se corrigió: las pruebas destaparon que crear una reserva enviando a la vez un cliente registrado y uno de mostrador se aceptaba en silencio (ahora es un error); varias pruebas de las fases 3 y 4 dependían del día de la semana (los domingos la sede cierra a las 20:00) y se hicieron independientes de la fecha; una prueba de recordatorios asumía que no había otras reservas por recordar.
- Pendiente: estadísticas e ingresos reales con estos pagos (Fase 6), promociones (descuento) y premios (Fases 7 y 8), torneos que reserven canchas (Fase 5), configuración general editable (Fase 9).

### Fase 5: Torneos (18/oct/2026)

- Backend: migración V7 (equipos, jugadores, torneos, inscripciones, plantillas, fases, grupos, partidos con enlaces del cuadro y eventos); dominio puro con tres generadores de fixture (liga con Berger, eliminación con descansos y tercer puesto, grupos y eliminación), `CalculadoraPosiciones` con desempates configurables, resolución de ganador y penales; servicios de equipos, torneos, fixture, partidos y consultas; reservas internas de partidos con el flujo de conflictos de los bloqueos; avance automático del cuadro y resolución de clasificados de grupos; estadísticas y perfiles públicos sin datos privados.
- Frontend: páginas públicas (torneos, detalle con posiciones, llaves, calendario, estadísticas y equipos, partido con línea de eventos, equipo, jugador) y de administración (equipos con plantilla, torneos con asistente de datos y reglas, gestión con inscripciones y fixture, partido con programación, resultado, W.O. y eventos).
- Pruebas: 282 en backend (49 nuevas: 30 de dominio —fixtures, byes, desempates, penales— y 19 de integración con PostgreSQL real que recorren liga, eliminatoria con descansos y penales, grupos, programación con conflictos, eventos y privacidad) y 53 en frontend. Verificado además en Docker (torneo de 8 equipos con grupos, semifinales, tercer puesto y final por la API) y en el navegador.
- Qué se corrigió: un bloque de texto SQL perdía el espacio inicial al concatenarse con la consulta base (error de sintaxis visible en la primera prueba); varias expectativas de las pruebas eran propias (el choque de equipos se evalúa antes que el de cancha); el frontend trataba como `null` campos que el backend omite (mostraba "undefined-undefined pen.").
- Pendiente: solicitud de inscripción por clientes habilitados (Fase 8), `DOBLE_ELIMINACION` y llaves de ida y vuelta (recortables), orden manual de siembra en la interfaz (la API ya lo admite), estadísticas de ingresos que excluyan el origen `TORNEO` (Fase 6).

### Fase 6: Estadísticas (18/oct/2026)

- Backend: módulo `estadisticas` de solo lectura con agregaciones SQL (resumen con comparación contra el periodo anterior, serie por día/semana/mes sin huecos, rendimiento y ocupación por cancha con horario efectivo, horas pico, clientes frecuentes) y exportación CSV; `FormatoCsv` compartido con reservas.
- Frontend: panel `/admin/estadisticas` con filtros, indicadores con variación, gráfica de barras y mapa de calor propios (SVG, sin librerías) con vista de tabla accesible.
- Pruebas: 292 en backend (6 de dominio y 10 de integración nuevas, con datos insertados directamente para fijar cada cifra: ingresos reales vs recaudado vs por cobrar, torneos excluidos de ingresos e incluidos en ocupación, horario propio de cancha, filtros por sede y cancha, validación del periodo, CSV) y 56 en frontend. Verificado en Docker y en el navegador con datos de prueba.
- Qué se corrigió: nada que corregir en las pruebas, pero en el navegador se vio que el valor de un indicador se partía en dos líneas y que un estado aparecía con guion bajo.
- Pendiente: caché de corta duración si el volumen lo exige; los premios (Fase 7) y la fidelización deben seguir ignorando el origen `TORNEO`.

### Fase 7: Promociones y premios (18/oct/2026)

- Backend: migración V8; dominio puro de `Promocion` (vigencia, días, franja, sede, cancha, primera reserva), `TipoDescuento` y `CalculadoraPremios`; puerto `PoliticaDescuentos` en reservas implementado por promociones (un solo descuento, el mayor); cupos con bloqueo de fila; eventos de reserva liberada y de completada corregida; servicio de premios que otorga o revoca comparando lo ganado con lo otorgado; endpoints de administración, vitrina pública y previsualización del cliente.
- Frontend: página pública de promociones, selector de código y premio en la solicitud de reserva con el precio final del servidor, `Mis premios` con barra de progreso, y administración de promociones (con uso) y de premios (reglas, otorgados y revocación).
- Pruebas: 319 en backend (10 de dominio y 11 de integración nuevas: código con cupo que se libera al cancelar, códigos inválidos o fuera de condiciones sin crear reserva, automática contra código, previsualización, reprogramación, premio ganado y usado, premio ajeno, revocación al corregir una completada, torneos sin cliente) y 58 en frontend. Verificado en Docker y en el navegador.
- Qué se corrigió: una prueba esperaba volver a completar una reserva `NO_ASISTIO` (la máquina de estados no lo permite, y está bien); en el navegador la fecha de la franja mostraba `NaN` porque el backend omite los campos nulos y el frontend los comparaba con `null`.
- Pendiente: descuentos acumulables, cupones personales y campañas por correo (fuera del alcance mínimo).

### Fase 8: Extras (18/oct/2026)

- Backend: migraciones V9 y V10; correo por el `Notificador` (conjunto cerrado de avisos, solo con correo verificado y después de confirmar la transacción) con adjuntos y generador `.ics`; series recurrentes con previsualización y cancelación; lista de espera que reacciona a la liberación de horarios; solicitud de inscripción de equipos por clientes habilitados con aprobación o rechazo; favoritas y calificaciones.
- Frontend: panel de series, «Avisarme si se libera», lista de espera, calendario, calificación y resumen en la cancha, favoritas, solicitud de inscripción, solicitudes en la gestión del torneo y `/mi-cuenta`.
- Pruebas: 342 en backend (23 nuevas: correo con adjunto, `.ics`, series, lista de espera, solicitudes de torneo, favoritas y calificaciones) y 62 en frontend. Verificado en Docker (serie de 4 semanas por la API) y en el navegador.
- Qué se corrigió: los clientes de las pruebas chocaron contra el límite de reservas activas (se usaron clientes distintos); dos pruebas compartían días que, corridos al lunes por el domingo, se solapaban; los correos de verificación de registro contaminaban el conteo de avisos (se filtró por plantilla).
- Pendiente: Fase 9 (pulido, datos de demostración y revisión de seguridad).

### Fase 9: Pulido, demostración y seguridad (18/oct/2026)

- Backend: módulo `configuracion` (parámetros del negocio editables con validación de rango, aplicados de inmediato y auditados; se retiraron tres claves que nunca se usaron); `RutasProtegidasIT`, que recorre **todas** las rutas de la API y exige que las de administración respondan 401 sin sesión y 403 a un cliente, que las de cliente exijan sesión y que lo público sea solo lectura.
- Datos de demostración: cargador en Python (`scripts/demo`, servicio `demo` de Docker Compose) que usa la API real y solo toca la base de datos para el historial de fechas pasadas y las cuentas; deja 2 sedes, 16 clientes, ~1.100 reservas de tres meses con pagos, promociones, premios, una serie recurrente, bloqueos y tres torneos (finalizado, en curso con grupos y empate resuelto por criterios, e inscripciones abiertas con una solicitud pendiente).
- Documentación: guion de demo de 10 minutos con plan B (`docs/guion-demo.md`), revisión de seguridad con la prueba que respalda cada punto (`docs/seguridad.md`), el prompt maestro guardado tal cual (`docs/ia/prompt-maestro.md`), README y reglas de trabajo del proyecto al día.
- Qué se corrigió: las cuentas de demostración fallaban al ingresar porque la clave debía llevar el prefijo `{bcrypt}` del codificador delegante (error 500 que destapó la carga real); el cuerpo de una reserva para cliente de mostrador anida los datos; los comandos `docker compose run` se quedaban esperando en esta máquina (problema local de Docker Desktop con contenedores adjuntos).
- Pendiente fuera del alcance de las fases: HTTPS y respaldos en el despliegue real (documentado en `docs/seguridad.md`), pasarela de pagos en línea, y un flujo E2E con Playwright (opcional en el prompt maestro).

