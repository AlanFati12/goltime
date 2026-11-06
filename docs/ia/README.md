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

### Mejora de UX (18/oct/2026)

- Petición: tras probar la aplicación, la persona pidió que se vea más limpia, fácil de interactuar y menos llena de información.
- Cambios (solo frontend, ver ADR 0013): menú de administración agrupado, inicio de administración convertido en tablero, encabezado público con menú de cuenta, filtros y métricas secundarias plegables, pestañas en la gestión y la lista de torneos, tarjetas de sedes más sobrias y piezas comunes nuevas (`EncabezadoPagina`, `Plegable`, `Pestanas`, `MenuDesplegable`).
- Pruebas: 66 en frontend (4 nuevas para `Pestanas` y `Plegable`); lint, tipos y build limpios. Revisado en el navegador en escritorio y en móvil.
- Qué se corrigió: el detalle de sede desbordaba el ancho a 1024 px (columnas de la cuadrícula sin `minmax(0, …)`); varias páginas tenían doble relleno.
- Pendiente: revisar con usuarios reales el resto de formularios largos (canchas, sedes, bloqueos) para plegarlos igual.

### Bot de WhatsApp, parte 1: teléfono normalizado y verificación (24/oct/2026)

- Petición: integrar un bot de WhatsApp (n8n + Evolution API) que consulte, reserve, cancele y reprograme. Se diseñó primero con preguntas a la persona (ADR 0018) y se empezó por exigir el WhatsApp en el registro sin que `3001234567` y `+57 300 123 4567` cuenten como números distintos.
- Backend: objeto de valor `NumeroTelefono` (E.164, Colombia por defecto, celular de 10 dígitos que empieza por 3); migración V13 (`telefono_verificado`, normalización de datos existentes, índice único parcial, `verificaciones_telefono`); `ServicioVerificacionTelefono` en `autenticacion` que identifica a quien escribe (cliente, sin verificar, sin cuenta, desactivado) y vincula el número con un código enviado al correo; el registro y el perfil rechazan un número ya verificado por otra cuenta (`TELEFONO_YA_REGISTRADO`); el teléfono de mostrador también se normaliza.
- Frontend: `CampoWhatsapp` (indicativo + número, reconoce el país si pegan el número completo) en registro, perfil y reserva de mostrador; estado «Vinculado / Sin vincular» en el perfil; teléfonos agrupados y marca de WhatsApp vinculado en el panel.
- Pruebas: 384 en backend (18 de `NumeroTelefono`, 3 nuevas de `Usuario`, 7 de integración de la verificación: vinculación, intentos agotados, dos cuentas con el mismo número, cambio de número, límite por hora) y 76 en frontend (10 nuevas). Verificado en Docker y en el navegador (registro con un fijo rechazado, celular guardado en E.164, perfil «Sin vincular»).
- Qué se corrigió: la primera versión ponía la verificación en `usuarios` y creaba un ciclo `usuarios → notificaciones → usuarios` que detectó `ArquitecturaTest`; un intento fallido debía sobrevivir al rollback (`noRollbackFor`); una prueba fallaba 1 de cada 10 veces porque el «número nuevo» podía coincidir con el viejo; al teclear `+593…` carácter por carácter se perdía el `+` (ahora se guarda un borrador hasta reconocer el país); los ejemplos usaban un número real y se cambiaron por uno ficticio.
- Pendiente: módulo `whatsapp` con los endpoints del bot, panel del admin y workflows de n8n.

### Bot de WhatsApp, parte 2: stack propio y módulo `whatsapp` (24/oct/2026)

- Petición: respaldar y borrar el stack viejo de n8n + Evolution (bots de otros negocios), definir uno nuevo dentro de Goltime, seguir con el módulo del bot y conectar el número +57 323 908 5762.
- Infraestructura: respaldo completo en `~/Documents/Respaldos/n8n-project-2026-10-24` (carpeta, volúmenes y export de n8n, con instrucciones para restaurar); `docker-compose.whatsapp.yml` con n8n 2.40.5, Evolution v2.3.7 y su PostgreSQL; variables nuevas en `.env.example`; `scripts/whatsapp/conectar.sh` para el QR. El número quedó conectado.
- Backend: migración V14 (origen `WHATSAPP`, idempotencia sin cuenta, claves `whatsapp_*`); `reservas` solicita, lista, cancela y reprograma a nombre de un `Titular` (cliente registrado o sin cuenta) y publica `AvisoReservaEvento`; módulo `whatsapp` con cadena de seguridad propia por clave, contexto, catálogo, franjas libres compactas, cotización, reservas, vinculación, torneos activos, tabla, marcadores y resumen redactado de un partido; avisos salientes a n8n firmados con HMAC, solo a números vinculados o a quien reservó por WhatsApp.
- Pruebas: 396 en backend; 9 de integración del bot (seguridad, sin cuenta con idempotencia y límite, cliente vinculado visible en la web, cuenta sin vincular y desactivada, franjas y precio, resumen sin goleadores del torneo, bot apagado, avisos) y 3 unitarias (textos de los avisos y firma HMAC).
- Qué se corrigió: el evento `ReservaCreadaEvento` suponía siempre un cliente registrado (NullPointerException con clientes sin cuenta); la duración de bloque por defecto es de 60 minutos y la primera versión de las pruebas reservaba 90.
- Pendiente: workflows de n8n (agente con `gpt-6-luna`), panel del admin (insignia y filtro «WhatsApp», configuración del bot, estadísticas por origen) y guion de demo.

### Bot de WhatsApp, parte 3: workflows de n8n (24/oct/2026)

- Petición: el workflow de n8n con el agente (`gpt-6-luna`), sus herramientas hacia la API y el workflow de avisos.
- Hecho: generador `integraciones/n8n/construir_workflows.py` (dos workflows versionados sin secretos) y `scripts/whatsapp/n8n-importar.sh` (credenciales desde `.env`, la de OpenAI vacía una sola vez, importar, publicar y reiniciar). Se partió de las versiones de nodos que ya funcionaban en el bot viejo (agente 3.1, modelo 1.3 con Responses API, HTTP Request Tool 4.2).
- Verificado: avisos con firma falsa o ausente → 401; con firma válida llega a Evolution (número inexistente → 400, así que la credencial sirve). Mensajes simulados: el propio se ignora, el audio recibe el texto fijo y el texto llega al agente con el contexto armado (falla solo por la API key de relleno).
- Qué se corrigió: en la base de Evolution los mensajes aparecen con `@lid`, pero el código de Evolution 2.3.7 cambia `remoteJid` por el teléfono antes del webhook; el bot viejo tomaba el número de `remoteJid` sin revisar. Los filtros opcionales vacíos que manda el modelo (`modalidad=`) se tratan como «sin filtro». `WEBHOOK_URL` pasó a `N8N_WEBHOOK_URL`.
- Con la API key real (la pegó la persona en n8n): conversación simulada completa desde un número inexistente. El agente rechazó una pregunta fuera de tema, explicó que el domingo no abren y ofreció el lunes, cotizó, pidió nombre y confirmación y solo entonces creó la solicitud; al aprobarla como admin, el aviso firmado llegó a n8n y a Evolution. La reserva y el cliente de prueba se borraron.
- Ajuste: la disponibilidad del bot trae una «nota» cuando no hay franjas (no abren o está lleno) y el prompt pide consultar el día siguiente.
- Pendiente: conversación real desde un celular; panel del admin.

### Bot de WhatsApp, parte 4: ritmo de respuesta (24/oct/2026)

- Petición: 10 s de espera entre respuestas, estado «escribiendo» mientras responde y «cerrar sesión» de la conversación al terminar.
- Hecho: el workflow pone el número «en línea» y «escribiendo…» al recibir; todas las respuestas (agente, fijas y de error) pasan por un solo nodo que calcula cuánto falta para los 10 s y se envían con ese `delay`, y al final el número queda «desconectado» aunque el envío falle. «Cerrar sesión» se interpretó como dejar de aparecer en línea, no como borrar la memoria (rompería las reservas de varios mensajes).
- Verificado con mensajes simulados: el agente tardó 3 s y el envío esperó 2,6 s más; el audio esperó 9,9 s; «desconectado» se ejecuta al final.
- Qué se corrigió: la respuesta fija leía la salida del nodo de presencia en vez del mensaje; una prueba corrió con la versión anterior porque n8n aún reiniciaba (el script de importación ahora espera a que esté listo).

### Bot de WhatsApp, parte 5: identificación por @lid (24/oct/2026)

- Problema: la persona le escribió al bot y recibió «No logré reconocer tu número». WhatsApp mandó el mensaje con un identificador anónimo (`…@lid`, `addressingMode: lid`) y sin teléfono; Baileys 7 tampoco tenía la relación @lid ↔ teléfono y Evolution no la expone.
- Hecho (aprobado por la persona): migración V15; `LidWhatsapp`; identificación por @lid en `ServicioVerificacionTelefono` (vinculación con el correo de la cuenta, respuesta igual exista o no el correo, límites por @lid y por cuenta); clientes sin cuenta atados al @lid con teléfono de contacto que nunca se cruza con los de mostrador; `RemitenteWhatsapp` en el módulo del bot (`X-WhatsApp-Numero` o `X-WhatsApp-Lid`); avisos al chat `<lid>@lid`; `whatsappVinculado` en el perfil y en la lista de clientes; workflow y prompt de n8n que piden el correo o el celular de contacto según el caso.
- Pruebas: 398 en backend (2 nuevas de integración del bot (sin cuenta por @lid, que ni otro @lid ni el teléfono de contacto pueden ver; vinculación por correo, reserva como cliente, aviso al @lid y @lid ya ocupado) y 76 en frontend. Simulado en n8n: un @lid que dice tener cuenta recibe la petición del correo.
- Qué se corrigió: al reconstruir la web solo con `docker-compose.yml` se recreó la API sin la clave de integración; ahora `COMPOSE_FILE` en `.env` incluye siempre el bot.

### Bot de WhatsApp, parte 6: vinculación con código de la web (24/oct/2026)

- Petición: aún no se envían correos, así que la vinculación no puede depender del correo; la persona propuso una clave dinámica por cuenta o un código que aparezca en la web. Además, al iniciar la conversación el bot debe decir si la cuenta está vinculada y, si no, preguntar si quiere vincularla o seguir sin vincular.
- Decisión: código de un solo uso generado en *Mi perfil › WhatsApp* (con sesión) que se le envía al bot; prueba la cuenta y el WhatsApp a la vez. Se descartó la clave fija (se filtra y sirve para siempre).
- Hecho: V16 (`codigos_vinculacion_whatsapp`, `intentos_vinculacion_whatsapp`, se elimina `verificaciones_telefono`); `CodigoVinculacion` (6 caracteres sin ambiguos, hash); `ServicioVinculacionWhatsapp` reemplaza al de verificación por correo (generar, vincular por teléfono o @lid, desvincular, 5 fallos por hora); `/api/v1/me/whatsapp` (código con enlace `wa.me` y desvincular); tarjeta «WhatsApp» en el perfil con cuenta regresiva, copiar, abrir WhatsApp y actualización automática al vincularse; el contexto del bot trae `vinculado` y el enlace al perfil; el prompt pregunta al inicio si vincular o seguir.
- Verificado: 400 pruebas en backend y 78 en frontend, con las nuevas de `VinculacionWhatsappIT` y de la tarjeta del perfil; en vivo, un @lid nuevo recibió respuesta a su pregunta más la oferta de vincular, y con una cuenta de prueba el código del perfil enviado desde un @lid simulado la vinculó y el perfil cambió solo a «Vinculado» (la cuenta de prueba se borró).
- Qué se corrigió: una prueba mandaba un código de 15 caracteres que la validación rechazaba antes de contarlo como intento; el `INSERT` del código usa `ON CONFLICT` porque en PostgreSQL una clave duplicada aborta la transacción.


### El admin cambia una reserva a cualquier estado (24/oct/2026)

- Petición: «que el administrador pudiese cambiar el estado de cualquier reserva a cualquier estado […] aunque no se haya llegado la fecha», por ejemplo marcar COMPLETADA una reserva futura.
- Hecho (ADR 0019): `Reserva.cambiarEstadoPorAdmin` sin reglas de tiempo y con motivo obligatorio; `ServicioReservasAdmin.cambiarEstado` vuelve a ocupar la cancha (`RESERVA_SOLAPADA` si ya la tomó otra reserva o un bloqueo, sin cambiar nada) o la libera y avisa a la lista de espera, publica los eventos de premios, deja historial (`ESTADO_CAMBIADO`, migración V17) y auditoría (`CAMBIAR_ESTADO_RESERVA`) y avisa al cliente, también por WhatsApp (`RESERVA_ESTADO_<ESTADO>`). `GET .../estado/previsualizar` muestra el efecto. En el detalle de la reserva, «Cambiar estado» (lista, efecto y motivo) reemplaza los botones de completada / no asistió.
- Desviación argumentada: COMPLETADA y NO_ASISTIO **conservan** la ocupación (como ya pasaba con las jugadas); si una completada futura soltara la cancha, otra reserva podría tomar el mismo horario y se contarían ingresos dobles. Lo que libera el horario es CANCELADA, RECHAZADA o EXPIRADA.
- Verificado: 414 pruebas en backend (nuevas de dominio para todas las combinaciones de estados, de integración para futura → COMPLETADA, CANCELADA → CONFIRMADA libre y con choque, COMPLETADA → PENDIENTE → EXPIRADA, reembolso al liberar y recuperación del pago al reactivar, aviso genérico para una jugada que vuelve a CONFIRMADA; premios otorgados y revocados; texto de WhatsApp) y 82 en frontend (modal y historial). La pila local se reconstruyó: V17 aplicada y las rutas nuevas responden 401 sin sesión. No se probó el modal en el navegador con sesión de admin.

### Optimización de rendimiento (nov/2026)

- Petición: el sitio se sentía lento en el servidor; se pidió una lista de mejoras posibles y evaluarlas antes de aplicarlas.
- Método: medición primero (Lighthouse móvil con 4G lenta y CPU 4× más lenta, y un contador de sentencias SQL por petición), cambios por lotes, medición después de cada uno. Resultado en la portada: 60 → 91 puntos, 9,3 → 3,3 s de contenido principal, 1.550 → 497 KB (ver ADR 0022).
- Qué se corrigió en el camino: una N+1 real (3 consultas por cancha en cada visita a la portada) que el listado de pruebas no cubría; saltos de diseño (CLS) causados por el pie de página, que mide ~790 px en celular; la pantalla de bienvenida que esperaba 900 ms a propósito y la espera de una renovación de sesión que solo podía devolver un 401 para los visitantes.
- Pruebas: 417 en backend (2 nuevas: N+1 y registro de lentitud) y 92 en frontend (10 nuevas: retraso de búsqueda, optimistas con reversión, renovación de sesión, medidas de imagen).
- Pendiente: probar en un teléfono físico, decidir si se pone Cloudflare delante del dominio y elegir un centro de datos cercano a Colombia al volver a desplegar.

