# PROMPT MAESTRO — GOLTIME

> Copia este documento completo a la IA de desarrollo (o déjalo en la raíz del repositorio como `PROMPT.md` / `CLAUDE.md`). Está escrito en segunda persona, dirigido a la IA.

---

## 0. Tu rol y cómo debes trabajar

Eres un ingeniero de software senior full-stack (Spring Boot + React). Vas a construir **Goltime**, una aplicación web para la gestión de canchas sintéticas de fútbol. Es el proyecto final de Ingeniería de Software de la Universidad Surcolombiana (USCO) y se expondrá en el evento **INNOVASOFT IX el 19 de noviembre de 2026**, así que debe funcionar de punta a punta, verse profesional en una demo y estar bien documentado.

Reglas de trabajo:

1. **Antes de escribir código, lee todo este documento y hazme las preguntas de la sección 18** (y cualquier otra que consideres necesaria). No asumas en silencio: si algo es ambiguo, pregunta; si decides algo por tu cuenta, déjalo registrado en `docs/decisiones/` (formato ADR corto).
2. Trabaja **por fases** (sección 15). Al terminar cada fase: el proyecto compila, las pruebas pasan, la app arranca con `docker compose up`, y me das un resumen de qué se hizo y qué sigue.
3. Haz commits pequeños con **Conventional Commits** (`feat:`, `fix:`, `refactor:`, `docs:`, `test:`, `chore:`).
4. Nunca dejes secretos en el código (JWT secret, contraseñas de BD, SMTP). Todo va en variables de entorno con un `.env.example`.
5. Todo el texto visible para el usuario va en **español (Colombia)**. Moneda **COP** (sin decimales, formato `$ 80.000`). Zona horaria **America/Bogota**. El código (clases, variables, endpoints) puede ir en inglés o español, pero sé consistente; propón una convención al inicio.
6. Si consideras que una tecnología o decisión de este documento es peor que una alternativa, **dímelo con argumentos antes de cambiarla**.
7. Lleva el registro de uso de IA descrito en la sección 14 desde el primer día (es obligatorio para la nota).

---

## 1. Contexto académico — requisitos de la rúbrica que afectan al software

Estos puntos son **obligatorios** y condicionan el diseño:

| Requisito de la rúbrica | Qué significa para el código |
|---|---|
| Aplicación **cliente-servidor** | Frontend (SPA) y backend (API REST) son proyectos separados, desplegables por separado, comunicados solo por HTTP/JSON. |
| **Arquitectura distinta a la monolítica** | Nada de backend que renderice vistas. Backend con arquitectura **hexagonal / limpia** (o microservicios, ver sección 4). Cada pieza (front, API, BD) en su propio contenedor. Debe poder explicarse con diagramas de componentes y despliegue. |
| Explicar el proceso de desarrollo y **evidenciar buenas prácticas** | Git con ramas y PR, Conventional Commits, pruebas automatizadas, linters/formatters, CI (GitHub Actions), README, documentación OpenAPI, migraciones versionadas, ADRs. |
| Establecer el **alcance según el tiempo** | Hay ~1 mes. Se prioriza un MVP sólido (sección 15) y lo demás queda como extensión. |
| Uso de **plataforma colaborativa** para seguimiento | El repositorio estará en GitHub; las tareas se gestionarán en GitHub Projects o Trello. Estructura el trabajo en tareas pequeñas que se puedan mapear a historias de usuario. |
| Entregables posteriores: diagrama de clases, secuencias, componentes, actividades, despliegue, modelo relacional, SAD | El código debe ser **fácil de diagramar**: paquetes que correspondan a componentes, entidades limpias que correspondan al diagrama de clases, migraciones Flyway que sean el modelo relacional oficial. |
| Apartado obligatorio si se usa IA | Registro de herramienta/modelo, prompts, reglas, agentes, skills y mecanismos de control (sección 14). |
| Exposición en INNOVASOFT | Datos de demostración realistas (sección 13) y un flujo de demo que no falle. |

---

## 2. Visión del producto

**Goltime** permite a un negocio de canchas sintéticas (con una o varias sedes) administrar sus canchas, horarios, reservas, promociones, programa de premios y torneos, y consultar estadísticas de ocupación e ingresos. Los clientes consultan disponibilidad, reservan, reprograman y cancelan, ven promociones, acumulan premios y siguen los torneos activos y pasados con sus estadísticas.

Descripción oficial del proyecto:
> Aplicación web para canchas sintéticas en la que el administrador puede crear torneos, cantidad de canchas según su capacidad, horarios, tener estadísticas financieras según el número de canchas que hayan sido ocupadas en el último tiempo, y al cliente se le permite ver las canchas disponibles, poder reservarlas, cancelarlas, mirar los torneos activos y pasados. El proyecto contempla el desarrollo de una aplicación web para la gestión de canchas sintéticas, permitiendo al administrador gestionar canchas, horarios, reservas, torneos y consultar estadísticas de ocupación e ingresos. Los clientes podrán consultar la disponibilidad de las canchas, realizar y cancelar reservas, y visualizar torneos activos y pasados.

**Importante:** el primer frontend es **provisional**; se le agregarán cosas y probablemente se rediseñará. Por eso la UI debe construirse con componentes reutilizables y tokens de diseño centralizados, y toda la lógica de negocio debe vivir en el backend (el front nunca calcula precios, disponibilidad ni tablas de posiciones por su cuenta).

---

## 3. Stack tecnológico

### Base solicitada (respétala salvo que argumentes algo mejor)
- **Backend:** Spring Boot (última versión estable que soporten todas las dependencias; 3.5.x o 4.x) con **Java 21 LTS**, Maven o Gradle.
- **Seguridad:** Spring Security + **JWT**.
- **Frontend:** **React + Tailwind CSS**.

### Recomendaciones complementarias (úsalas salvo objeción)
**Backend**
- **PostgreSQL 16+** como base de datos. Motivo clave: permite una **restricción de exclusión** (`EXCLUDE USING gist` sobre rangos de tiempo con la extensión `btree_gist`) que hace **imposible a nivel de base de datos** que dos reservas activas se solapen en la misma cancha, incluso con peticiones concurrentes. MySQL no tiene esto.
- **Flyway** para migraciones versionadas (el esquema nunca se genera con `ddl-auto=update`; usar `validate`).
- Spring Data JPA (Hibernate), **Bean Validation**, **MapStruct** para DTO↔dominio, Lombok (con moderación, nunca `@Data` en entidades JPA).
- JWT con **`spring-boot-starter-oauth2-resource-server`** (Nimbus) en lugar de filtros JWT hechos a mano.
- Hash de contraseñas con **BCrypt (cost 12)** o **Argon2id** vía `DelegatingPasswordEncoder`.
- **springdoc-openapi** (Swagger UI) para documentar la API.
- **Bucket4j** para limitar intentos de login/registro.
- `@Scheduled` (o ShedLock si se escala) para tareas programadas.
- **Spring Mail** para correos; en desarrollo, **Mailpit** en Docker para ver los correos sin enviarlos de verdad.
- Pruebas: JUnit 5, Mockito, **Testcontainers** (PostgreSQL real en pruebas de integración), ArchUnit opcional para verificar que las capas de la arquitectura hexagonal no se violan.
- Respuestas de error con **ProblemDetail (RFC 7807)**.

**Frontend**
- **Vite + React + TypeScript** (TypeScript, no JavaScript plano).
- Tailwind CSS + **shadcn/ui** (componentes accesibles y fáciles de re-estilizar, ideal para un front provisional).
- **React Router** (rutas protegidas por rol).
- **TanStack Query** para estado del servidor (caché, reintentos, invalidaciones).
- **Zustand** para estado de sesión mínimo.
- **React Hook Form + Zod** para formularios y validación.
- **Axios** con interceptor que renueva el access token con el refresh token automáticamente.
- **Recharts** para gráficas de estadísticas.
- **date-fns** (+ `date-fns-tz`) para fechas en America/Bogota.
- Un componente de calendario/grilla de disponibilidad (propio o FullCalendar).
- Visualización de llaves de eliminación (bracket): componente propio con CSS grid o una librería ligera.
- ESLint + Prettier.

**Infraestructura**
- **Docker Compose** con servicios: `db` (PostgreSQL), `api` (Spring Boot), `web` (Nginx sirviendo el build de React), `mailpit`.
- **GitHub Actions**: build + tests del backend y del frontend en cada PR.
- Perfiles de Spring: `dev`, `test`, `prod`.

---

## 4. Arquitectura

### Propuesta por defecto
Arquitectura **cliente-servidor en tres niveles**, con el backend en **arquitectura hexagonal (puertos y adaptadores)** organizada por módulos de negocio:

```
goltime/
├── backend/
│   └── src/main/java/co/edu/usco/goltime/
│       ├── shared/            (errores, utilidades, auditoría base, config común)
│       ├── auth/              (registro, login, tokens, recuperación de contraseña)
│       ├── users/             (clientes, activación/desactivación, perfil)
│       ├── venues/            (sedes, canchas, tarifas, horarios, bloqueos, festivos)
│       ├── bookings/          (disponibilidad, reservas, reprogramación, cancelación, pagos, recurrentes, lista de espera)
│       ├── promotions/
│       ├── rewards/           (programa de premios)
│       ├── tournaments/       (torneos, equipos, jugadores, fixture, partidos, eventos, tablas)
│       ├── notifications/
│       ├── audit/
│       └── stats/
│     Cada módulo:
│       ├── domain/            (entidades/modelos y reglas puras, sin Spring)
│       ├── application/       (casos de uso / servicios, puertos de entrada y salida)
│       └── infrastructure/    (controladores REST, repositorios JPA, mappers, adaptadores)
├── frontend/
├── docs/
│   ├── decisiones/            (ADRs)
│   └── ia/                    (registro obligatorio de uso de IA)
└── docker-compose.yml
```

- Los módulos se comunican mediante **interfaces de aplicación o eventos de dominio** (`ApplicationEventPublisher`), no accediendo a repositorios de otros módulos. Ejemplo: cuando una reserva pasa a `COMPLETADA`, `bookings` publica `ReservaCompletadaEvent` y `rewards` lo escucha para sumar progreso.
- La lógica de negocio crítica (cálculo de precio, disponibilidad, generación de fixture, tabla de posiciones) vive en clases de dominio puras y **100 % testeables sin Spring**.

### Alternativa (pregúntame antes de elegirla)
Si el docente exige explícitamente microservicios: `api-gateway` (Spring Cloud Gateway) + `auth-service` + `booking-service` (sedes, canchas, reservas, promociones, premios, estadísticas) + `tournament-service`, cada uno con su esquema. Es más costoso en tiempo; solo hacerlo si lo confirmo.

---

## 5. Roles y permisos

Solo existen **dos roles**: `ADMIN` y `CLIENTE`.

- El **registro público** solo crea usuarios `CLIENTE`.
- El primer `ADMIN` se crea con una migración/seed a partir de variables de entorno (`ADMIN_EMAIL`, `ADMIN_PASSWORD`). Un admin puede crear otros admins desde el panel.
- Un cliente **solo** puede ver y modificar sus propias reservas, canjes y perfil (verificar propiedad en el backend en cada endpoint; nada de confiar en IDs que mande el front → evitar IDOR).
- Un usuario **desactivado** no puede iniciar sesión y sus tokens vigentes dejan de servir inmediatamente.

---

## 6. Módulos funcionales

### 6.1 Autenticación, registro y perfil
**Cliente**
- Registro: nombres, apellidos, correo (único), teléfono/celular, contraseña + confirmación, aceptación de términos y tratamiento de datos (Ley 1581 de 2012). Documento de identidad opcional.
- Política de contraseña: mínimo 8 caracteres, al menos una letra y un número; indicador de fortaleza en el front.
- Verificación de correo (enlace con token) — recomendable; si no da el tiempo, dejarlo detrás de una bandera de configuración.
- Inicio de sesión, cierre de sesión (revoca el refresh token), "recordarme".
- Recuperar contraseña por correo (token de un solo uso, guardado como hash, expira en 30 min). La respuesta nunca revela si el correo existe.
- Perfil: editar datos, cambiar contraseña (pide la actual), ver historial y estadísticas personales (reservas hechas, horas jugadas, cancelaciones), preferencias de notificación.

**Admin**
- Mismo login. Puede crear otros administradores.

### 6.2 Sedes y canchas
No todos los lugares tienen la misma cantidad ni el mismo tipo de canchas, así que el modelo es **Sede → Canchas**.

**Sede:** nombre, dirección, ciudad, teléfono/WhatsApp, descripción, servicios (parqueadero, camerinos, duchas, cafetería, venta de bebidas…), fotos, ubicación (lat/lng opcional para un mapa), horario de operación por día de la semana, estado (activa/inactiva).

**Cancha:** sede, nombre o número, **modalidad** (fútbol 5, 6, 7, 8, 9, 11 — configurable), **jugadores por equipo** y capacidad total, tipo de superficie (sintética, etc.), dimensiones (largo × ancho, opcional), techada/cubierta (sí/no), iluminación (sí/no), **precio base por hora**, duración del bloque de reserva (p. ej. 60 min), duración mínima y máxima de una reserva, fotos, descripción, estado (`ACTIVA`, `MANTENIMIENTO`, `INACTIVA`), orden de visualización.

Reglas:
- Una cancha en `MANTENIMIENTO` o `INACTIVA` no aparece como reservable. Si el admin cambia una cancha a ese estado y tiene reservas futuras, se aplica el **mismo flujo de advertencia de conflictos** del bloqueo de horarios (6.4).
- No se borra físicamente una cancha con historial; se desactiva (soft delete).
- *(Opcional, preguntar)* **Canchas divisibles/compuestas**: una cancha de fútbol 8 formada por dos de fútbol 5; reservar la grande ocupa las pequeñas y viceversa. Si no se implementa ahora, dejar el modelo preparado (tabla `cancha_composicion`).

### 6.3 Tarifas
- Precio base por hora de cada cancha.
- **Franjas de tarifa** opcionales por cancha: día(s) de la semana + rango horario → precio por hora (ej.: L-V 6:00–17:00 $70.000; L-V 17:00–23:00 $100.000; fines de semana $110.000).
- Tarifa especial en **festivos colombianos** (tabla de festivos administrable; precargar los festivos de 2026 y 2027).
- Si una reserva cruza dos franjas, el precio se calcula proporcionalmente por bloque.
- El precio final se calcula **siempre en el backend** y se guarda como **instantánea** en la reserva (precio base, descuento, promoción aplicada, precio final), para que cambios posteriores de tarifas no alteren reservas existentes ni estadísticas históricas.

### 6.4 Horarios de operación y bloqueos
- **Horario de operación** por sede y día (ej.: L-S 8:00–23:00, D 8:00–20:00, o cerrado). Override opcional por cancha.
- **Bloqueos** creados por el admin para impedir reservas:
  - Alcance: toda la sede, varias canchas o una cancha.
  - Tipo **puntual** (fecha/hora inicio → fecha/hora fin, o día completo) o **recurrente** (ej.: todos los martes de 18:00 a 20:00 entre dos fechas).
  - Motivo obligatorio (mantenimiento, evento privado, torneo, clima, otro) + nota.
  - Se pueden editar y eliminar; todo queda auditado.
- **Flujo de conflictos (obligatorio):**
  1. Al crear/editar un bloqueo, el front llama primero a un endpoint de **previsualización** (`POST /api/v1/admin/blocks/preview`) que devuelve las reservas activas que se verían afectadas (cliente, cancha, fecha, hora, valor, si estaba pagada).
  2. Si hay conflictos, se muestra un **modal de advertencia** con la lista y el texto "Estas N reservas serán canceladas y se notificará a los clientes".
  3. Solo si el admin confirma, en una **única transacción**: se crea el bloqueo, se cancelan esas reservas con `cancelada_por = ADMIN`, motivo "Bloqueo de horario: <motivo>", se registra el historial y se notifica a cada cliente (in-app + correo).
  4. Si alguna reserva afectada tenía pago registrado, queda marcada con `estado_pago = REEMBOLSO_PENDIENTE` para que el admin gestione la devolución.
  5. Si entre la previsualización y la confirmación aparecieron reservas nuevas en ese rango, el backend lo detecta y vuelve a pedir confirmación (no cancelar reservas que el admin no vio).
  - *(Mejora)* En el modal, permitir que el admin **reprograme** individualmente alguna reserva afectada en lugar de cancelarla.
- Partidos de torneo programados en una cancha ocupan ese horario igual que una reserva (ver 6.13).

### 6.5 Reservas — lo que puede hacer el CLIENTE
1. **Consultar disponibilidad**: filtrar por sede, fecha, modalidad (F5, F7…), rango horario y precio. Vista de grilla (canchas × horas) con bloques disponibles, ocupados y bloqueados, y el precio de cada bloque (ya con promoción si aplica). Ver próximos 7 días rápidamente.
2. **Ver detalle de una cancha**: fotos, características, servicios de la sede, tarifas, promociones vigentes.
3. **Crear una reserva**: elegir cancha, fecha, hora de inicio y duración (múltiplos del bloque) → resumen con desglose (precio base, descuento, total) → confirmar. Campos opcionales: nombre del equipo, notas para el admin, código promocional.
4. **Reserva recurrente**: misma cancha y hora cada semana durante N semanas (ej. el partido fijo de los jueves). El sistema muestra qué fechas están disponibles y cuáles no antes de confirmar; se crea una "serie" y cada ocurrencia es una reserva independiente. Se puede cancelar una ocurrencia o la serie completa.
5. **Mis reservas**: pestañas *Próximas*, *Historial* y *Canceladas*, con estado, valor, estado de pago y la **línea de tiempo de cambios** (quién creó, quién reprogramó, quién canceló y cuándo).
6. **Reprogramar**: mover la reserva a otra fecha/hora/cancha disponible respetando la política (antelación mínima y máximo de reprogramaciones por reserva, configurables). Si el nuevo horario tiene otro precio, se muestra la diferencia antes de confirmar.
7. **Cancelar**: con motivo opcional. Si se cancela dentro del plazo permitido (ej. ≥ 4 h antes, configurable), es cancelación normal; si no, queda como **cancelación tardía** (se registra; el admin decide qué hacer con un abono). Confirmación antes de cancelar.
8. **Lista de espera**: si un horario está ocupado, el cliente se anota y recibe una notificación si se libera (el primero en reservar se lo queda; no se asigna automáticamente).
9. **Comprobante** de la reserva (código único legible tipo `GT-2026-000123`, descargable/imprimible) y opción **"Agregar a mi calendario"** (archivo `.ics`).
10. **Compartir** la reserva con los compañeros (enlace o texto para WhatsApp con cancha, sede, fecha, hora y dirección).
11. **Canchas favoritas** y **repetir una reserva anterior** con un clic.
12. **Ver promociones vigentes**.
13. **Programa de premios**: ver progreso (barra "llevas 7 de 10 reservas"), reclamar y elegir premio, ver historial de canjes.
14. **Notificaciones** in-app (campana con contador) y por correo: confirmación, recordatorio (ej. 2 h antes, configurable), reprogramación o cancelación hecha por el admin, liberación de cupo en lista de espera, premio desbloqueado, inicio de torneo.
15. **Calificar** la cancha después de jugar (1–5 estrellas + comentario) — opcional, baja prioridad.
16. **Torneos**: ver la sección pública de torneos (6.13).

Límites configurables para el cliente: máximo de reservas activas simultáneas, antelación máxima para reservar (ej. 30 días), antelación mínima (ej. 1 h), horas mínimas para cancelar sin penalidad, máximo de reprogramaciones por reserva.

**Pagos:** no se integra pasarela de pago real en esta versión. El pago se registra **manualmente por el admin** (efectivo, transferencia, Nequi, Daviplata, tarjeta en sitio) con monto y referencia. Estados de pago: `PENDIENTE`, `ABONADO`, `PAGADO`, `REEMBOLSO_PENDIENTE`, `REEMBOLSADO`. Dejar el diseño preparado para integrar Wompi o Mercado Pago después.

### 6.6 Reservas — lo que puede hacer el ADMIN
- Ver **todas** las reservas en vista de **calendario/agenda** (día y semana, por sede y cancha) y en **tabla** con filtros (fecha, sede, cancha, cliente, estado, estado de pago, origen) y búsqueda por código o nombre de cliente.
- **Crear reservas a nombre de un cliente** registrado, o para un cliente **"de mostrador"** (nombre + teléfono, sin cuenta) que llega o llama.
- **Reprogramar** y **cancelar** cualquier reserva, con **motivo obligatorio**; el cliente recibe notificación.
- Marcar reserva como **COMPLETADA** o **NO_ASISTIÓ** (y un job automático pasa a `COMPLETADA` las reservas confirmadas cuya hora de fin ya pasó, si el admin no marcó otra cosa — preguntar cuál comportamiento prefiero).
- **Registrar pagos y abonos**, y gestionar reembolsos pendientes.
- Ver el **historial completo** de cada reserva.
- Exportar el listado filtrado a CSV.

### 6.7 Trazabilidad y auditoría (quién hizo qué)
Requisito explícito: debe saberse **quién** creó, reprogramó, canceló o modificó algo.

1. **Historial de reserva** (`reserva_historial`): cada acción guarda tipo (`CREADA`, `REPROGRAMADA`, `CANCELADA`, `COMPLETADA`, `NO_ASISTIO`, `PAGO_REGISTRADO`, `MODIFICADA`), **actor** (id y nombre), **rol del actor** (`ADMIN` / `CLIENTE` / `SISTEMA`), fecha y hora, motivo, y los datos **antes y después** (JSONB). En la UI se muestra como línea de tiempo: *"Reprogramada por el administrador Juan Pérez el 12/11/2026 15:40 — de jue 13/11 19:00 (Cancha 2) a vie 14/11 20:00 (Cancha 3). Motivo: mantenimiento."*
2. En la reserva misma, campos rápidos: `creada_por`, `cancelada_por`, `rol_cancelador`, `motivo_cancelacion`, `fecha_cancelacion`, `veces_reprogramada`.
3. **Bitácora general** (`audit_log`) para acciones administrativas sobre cualquier entidad: canchas, tarifas, bloqueos, promociones, premios, usuarios (activar/desactivar), torneos, resultados de partidos. Campos: actor, rol, acción, entidad, id de entidad, cambios (JSONB), motivo, IP, fecha. Pantalla de admin con filtros.
4. Auditoría JPA básica (`created_at`, `updated_at`, `created_by`, `updated_by`) en todas las tablas de negocio, con `AuditorAware` leyendo el usuario del token.
5. El **SISTEMA** también es actor (jobs automáticos: completar reservas, expirar canjes).

### 6.8 Notificaciones
- Tabla de notificaciones in-app (tipo, título, mensaje, leída, enlace a la entidad, fecha). Endpoint para listar, marcar como leída y marcar todas.
- Correo para los eventos importantes (plantillas HTML simples con Thymeleaf). Envío **asíncrono** y tolerante a fallos (si el SMTP falla, la operación de negocio no se revierte).
- Diseñar con un puerto `NotificationSender` para agregar WhatsApp/SMS después.

### 6.9 Promociones
- Campos: nombre, descripción, imagen opcional, **tipo** (`PORCENTAJE`, `MONTO_FIJO_DESCUENTO`, `PRECIO_ESPECIAL_POR_HORA`), valor, **alcance** (todas las canchas, una o varias sedes, una o varias canchas, o una modalidad como "todas las de fútbol 5"), **vigencia** (fecha inicio–fin), **días de la semana** y **franja horaria** opcionales (ej. "happy hour martes 8–11 a.m. -30 %"), código promocional opcional, límite de usos total y por cliente, prioridad, activa/inactiva.
- Regla de combinación: **no acumulables**; se aplica automáticamente la que dé **mayor descuento** al cliente (documentarlo). Un código promocional explícito solo aplica si es válido para esa reserva.
- El precio final nunca puede ser negativo ni menor a un mínimo configurable.
- Registrar cada uso (`promocion_usos`) para estadísticas.
- Las promociones vigentes se muestran en la página de inicio y en la grilla de disponibilidad (precio tachado + precio con descuento).

### 6.10 Programa de premios (fidelización)
- Interruptor global **activar/desactivar** el programa.
- **Reglas de premio** (puede haber varias activas): nombre, **cantidad de reservas requeridas**, ventana de tiempo opcional (ej. "10 reservas en 60 días" o acumulado sin límite), si cuentan solo reservas `COMPLETADAS` (por defecto sí, para evitar trampa reservando y cancelando), duración mínima de la reserva para contar, vigencia de la regla.
- **Catálogo de premios** administrable: nombre (ej. "Caja de cerveza", "Energizantes para todo el equipo", "1 hora gratis"), descripción, imagen, stock opcional, activo/inactivo. El admin puede agregar, editar, desactivar y quitar premios, y definir qué premios están disponibles en cada regla.
- Flujo: el cliente cumple la meta → se crea un **canje** en estado `DISPONIBLE` y se le notifica → el cliente **elige su premio** entre los disponibles (`SOLICITADO`) → el admin lo **entrega** (`ENTREGADO`, con quién y cuándo) → el contador del cliente se reinicia para esa regla. Estados adicionales: `VENCIDO` (si no lo reclama en X días, configurable) y `CANCELADO`.
- Si un premio queda sin stock, deja de mostrarse como opción.
- El admin ve los canjes pendientes de entrega y el historial.
- Si una reserva que contó para el progreso se revierte (p. ej. se marca no asistió por error y se corrige), recalcular el progreso.

### 6.11 Gestión de clientes (admin)
- Listado con búsqueda y filtros (activos/inactivos, con más reservas, registrados en un rango de fechas).
- Detalle del cliente: datos, reservas, cancelaciones (y tardías), no asistencias, pagos, canjes, total gastado.
- **Activar / desactivar** cliente con motivo obligatorio. Al desactivar, si tiene reservas futuras, mostrar advertencia (mismo patrón de conflictos) y preguntar si se cancelan. Un cliente desactivado no puede iniciar sesión; se le muestra un mensaje claro.
- Indicador de cliente "riesgoso" (muchas cancelaciones tardías/no asistencias) — solo informativo.

### 6.12 Estadísticas financieras y de ocupación (admin)
Dashboard con selector de periodo (hoy, últimos 7 días, últimos 30 días, este mes, mes anterior, rango personalizado) y filtro por sede/cancha:
- **Ingresos**: reales (pagos registrados) vs. proyectados (reservas confirmadas aún no pagadas). Por día/semana/mes, por sede, por cancha, por modalidad.
- **Tasa de ocupación** = horas reservadas ÷ horas disponibles (horario de operación menos bloqueos), por cancha y global.
- **Horas pico**: mapa de calor día de la semana × hora.
- Comparación con el periodo anterior (± %).
- Cancelaciones (por cliente vs. por admin, tardías), no asistencias.
- Impacto de promociones (reservas y descuento total otorgado por promoción).
- Top clientes por reservas y por gasto.
- Premios entregados.
- Indicadores del día en el inicio del panel: reservas de hoy, próximas a empezar, ingresos del día, canjes pendientes.
- Exportar a CSV (PDF opcional).
- Las consultas se hacen con SQL agregado eficiente (no cargar todas las reservas en memoria).

### 6.13 Torneos
**Creación y configuración (admin)**
- Datos: nombre, descripción, imagen/logo, sede, canchas que usará, modalidad (F5, F7…), fechas de inicio y fin, premio(s) del torneo, valor de inscripción (informativo), cupo máximo de equipos, reglamento (texto).
- **Formato**:
  - `LIGA` (todos contra todos, opción **ida y vuelta**).
  - `ELIMINACION_DIRECTA` (opción de **partido por el tercer puesto**, opción de llaves de ida y vuelta).
  - `GRUPOS_Y_ELIMINACION` (número de grupos, equipos que clasifican por grupo, opción de mejores terceros si se puede implementar, cruces automáticos 1A vs 2B, 1B vs 2A…).
  - *(Opcional si hay tiempo)* `DOBLE_ELIMINACION`.
  - Diseña el generador de fixture con un patrón **Strategy** para que agregar formatos nuevos sea sencillo.
- **Reglas de puntuación** configurables: puntos por victoria/empate/derrota (por defecto 3/1/0), **criterios de desempate en orden configurable** (puntos, diferencia de gol, goles a favor, enfrentamiento directo, menos tarjetas/juego limpio, sorteo), cómo se resuelve un empate en eliminación (penales por defecto), marcador por W.O. (por defecto 3-0).
- **Modo de emparejamiento**: `MANUAL` (el admin arma los cruces/grupos arrastrando o seleccionando equipos), `ALEATORIO` (sorteo) o `SEMBRADO` (por cabezas de serie). Aplica tanto a la primera ronda/grupos como, si el admin lo elige, a las rondas siguientes en ligas de varias fases.
- **Equipos sin rival (byes):** en eliminación directa, si el número de equipos no es potencia de 2, el sistema calcula los byes necesarios (siguiente potencia de 2 − número de equipos), se los asigna a los cabezas de serie o al azar (según el modo) y esos equipos **avanzan automáticamente** a la siguiente ronda (partido marcado como `BYE`, sin marcador y sin contar en estadísticas). En liga con número impar de equipos, cada fecha un equipo **descansa** (algoritmo del círculo / Berger). Si un equipo se retira, el admin decide entre: dar W.O. a sus rivales pendientes o anular sus partidos (y recalcular la tabla).
- **Avance automático en llaves**: cada partido de eliminación conoce el partido siguiente y en qué lado (local/visitante) va el ganador; al registrar el resultado final, el ganador se ubica automáticamente en la siguiente llave. Si se corrige un resultado ya propagado, advertir y recalcular solo si la siguiente llave no se ha jugado.
- **Estados del torneo**: `BORRADOR` → `INSCRIPCIONES` → `EN_CURSO` → `FINALIZADO` (o `CANCELADO`). El admin decide cuándo cambia el estado. Solo se ven públicamente los que no están en `BORRADOR`. Al finalizar se registra el **campeón**, subcampeón y tercero.
- El fixture puede **regenerarse** mientras no se haya jugado ningún partido.

**Equipos y jugadores**
- Equipo: nombre, escudo/logo, color, capitán/delegado (nombre y teléfono), cliente responsable opcional (vinculado a un usuario).
- Jugador: nombres, apellidos, documento opcional, número de camiseta, posición, foto opcional, usuario vinculado opcional (para que el cliente vea "mis estadísticas").
- Los equipos y jugadores son reutilizables entre torneos; la **inscripción** y la **plantilla** se registran por torneo (un jugador no puede estar en dos equipos del mismo torneo).
- El admin crea equipos, agrega/edita/quita jugadores.
- *(Opcional, preguntar)* El cliente puede **solicitar la inscripción** de su equipo a un torneo en estado `INSCRIPCIONES`; el admin la aprueba o rechaza.

**Partidos y eventos**
- Cada partido: fase, ronda/jornada, grupo, local, visitante, **fecha, hora y cancha** (al programarlo se **ocupa** ese horario en la cancha creando una reserva interna con origen `TORNEO`, validando conflictos con el mismo flujo de advertencia), árbitro opcional, estado (`PROGRAMADO`, `EN_JUEGO`, `FINALIZADO`, `APLAZADO`, `WO`, `BYE`).
- El admin registra el **marcador** y los **eventos** del partido: **gol** (jugador, minuto, si fue de penal), **autogol**, **asistencia** (vinculada a un gol), **tarjeta amarilla**, **tarjeta roja**, **falta** (jugador, minuto), y opcionalmente la **figura del partido**. El marcador debe ser **consistente** con los goles registrados (validar o derivar).
- *(Opcional)* Sanciones automáticas: suspensión por roja o por acumulación de amarillas (configurable), con aviso al admin al programar el siguiente partido.

**Vista del cliente (pública dentro de la app)**
- Sección **Torneos** con pestañas **Activos** y **Finalizados** (y "Próximos" para los que están en inscripciones).
- Detalle del torneo: información, **tabla de posiciones** (PJ, PG, PE, PP, GF, GC, DG, Pts, últimos 5 resultados), **grupos**, **llaves** (bracket visual), **calendario y resultados** por jornada, **goleadores**, **asistidores**, **tarjetas / juego limpio**, **faltas**, figura del torneo, campeón destacado en los finalizados.
- Detalle de partido: marcador y línea de tiempo de eventos.
- Página de equipo: plantilla y estadísticas del equipo; página de jugador: estadísticas en el torneo.
- Las tablas y rankings los calcula el backend (vista SQL o servicio de dominio con caché que se invalida al registrar un resultado).

### 6.14 Configuración general (admin)
Pantalla de parámetros del negocio (tabla `configuracion` tipada o entidad única): horas mínimas para cancelar sin penalidad, máximo de reprogramaciones, antelación mínima y máxima para reservar, máximo de reservas activas por cliente, horas antes para el recordatorio, días para reclamar un premio, completar reservas automáticamente (sí/no), datos de contacto del negocio, términos y condiciones.

---

## 7. Reglas de negocio clave (resumen verificable)

1. **Nunca** puede haber dos reservas activas (`CONFIRMADA`, `PENDIENTE`) solapadas en la misma cancha. Se garantiza en tres niveles: validación en el servicio, transacción, y **restricción de exclusión en PostgreSQL**. Debe existir una prueba de integración que lance reservas concurrentes al mismo horario y verifique que solo una gana.
2. No se puede reservar fuera del horario de operación, sobre un bloqueo, en una cancha no activa, en el pasado, ni fuera de la ventana de antelación.
3. Las reservas se alinean a la duración del bloque de la cancha.
4. El precio se calcula en el backend y se guarda como instantánea.
5. Toda reprogramación/cancelación registra actor, rol, motivo y fecha.
6. Bloquear horarios, desactivar canchas o desactivar clientes con reservas futuras **requiere previsualización y confirmación explícita**.
7. Solo las reservas `COMPLETADAS` cuentan para premios y para ingresos reales.
8. Un cliente desactivado no puede autenticarse ni operar.
9. Las acciones del admin sobre datos de un cliente notifican al cliente.
10. Los resultados de torneo actualizan automáticamente tablas, rankings y llaves.
11. Fechas y horas se guardan en UTC (`timestamptz`) y se muestran en America/Bogota.
12. Borrados lógicos para entidades con historial (canchas, sedes, usuarios, premios, promociones, equipos).

---

## 8. Modelo de datos (PostgreSQL)

Propuesta base. Ajústala si lo ves necesario, pero justifica los cambios. Todas las tablas de negocio llevan `id` (BIGINT identity o UUID — elige uno y sé consistente), `created_at`, `updated_at`, `created_by`, `updated_by`. Usa enums como `VARCHAR` con `CHECK` (más fáciles de migrar que enums nativos). Índices en todas las FK y en columnas de filtro frecuentes.

**Usuarios y seguridad**
- `usuarios` (nombres, apellidos, email UNIQUE, telefono, documento, password_hash, rol, activo, email_verificado, motivo_desactivacion, desactivado_por → usuarios, fecha_desactivacion, token_version, ultimo_login)
- `refresh_tokens` (usuario_id → usuarios, token_hash, expira_en, revocado, reemplazado_por, user_agent, ip)
- `tokens_verificacion` (usuario_id, tipo [VERIFICAR_EMAIL, RESET_PASSWORD], token_hash, expira_en, usado_en)
- `clientes_mostrador` (nombre, telefono) — para reservas hechas por el admin a personas sin cuenta

**Sedes, canchas y horarios**
- `sedes` (nombre, direccion, ciudad, telefono, descripcion, servicios JSONB o tabla aparte, lat, lng, activa)
- `sede_fotos`, `cancha_fotos` (url, orden)
- `sede_horarios` (sede_id, dia_semana 1–7, hora_apertura, hora_cierre, cerrado) — UNIQUE(sede_id, dia_semana)
- `canchas` (sede_id, nombre, modalidad, jugadores_por_equipo, superficie, largo_m, ancho_m, techada, iluminacion, precio_base_hora, duracion_bloque_min, duracion_min_min, duracion_max_min, estado, descripcion, orden) — UNIQUE(sede_id, nombre)
- `cancha_composicion` (cancha_padre_id, cancha_hija_id) — opcional
- `tarifas` (cancha_id, dias_semana, hora_inicio, hora_fin, aplica_festivo, precio_hora, vigente_desde, vigente_hasta)
- `festivos` (fecha UNIQUE, nombre)
- `bloqueos` (sede_id, tipo [PUNTUAL, RECURRENTE], inicio, fin, todo_el_dia, dias_semana, hora_inicio, hora_fin, fecha_desde, fecha_hasta, motivo_tipo, motivo_detalle, activo)
- `bloqueo_canchas` (bloqueo_id, cancha_id) — vacío = toda la sede

**Reservas**
- `reservas` (codigo UNIQUE, cliente_id → usuarios NULL, cliente_mostrador_id NULL [CHECK: exactamente uno], cancha_id, inicio timestamptz, fin timestamptz, estado [PENDIENTE, CONFIRMADA, CANCELADA, COMPLETADA, NO_ASISTIO], origen [WEB, ADMIN, TORNEO, RECURRENTE], serie_id → series_reserva, precio_base, descuento, precio_final, promocion_id, estado_pago, monto_pagado, nombre_equipo, notas, cancelada_por → usuarios, rol_cancelador, motivo_cancelacion, fecha_cancelacion, cancelacion_tardia, veces_reprogramada, version [optimistic locking])
  - `EXCLUDE USING gist (cancha_id WITH =, tstzrange(inicio, fin, '[)') WITH &&) WHERE (estado IN ('PENDIENTE','CONFIRMADA'))` (requiere `CREATE EXTENSION btree_gist`)
  - CHECK (fin > inicio)
- `series_reserva` (cliente_id, cancha_id, dia_semana, hora_inicio, duracion_min, fecha_desde, fecha_hasta, activa)
- `reserva_historial` (reserva_id, accion, actor_id, actor_rol, motivo, datos_anteriores JSONB, datos_nuevos JSONB, fecha)
- `pagos` (reserva_id, monto, metodo, referencia, tipo [PAGO, ABONO, REEMBOLSO], registrado_por, fecha)
- `lista_espera` (cliente_id, cancha_id NULL, sede_id, inicio, fin, estado, notificado_en)
- `favoritos` (cliente_id, cancha_id) — PK compuesta
- `calificaciones` (reserva_id UNIQUE, cliente_id, cancha_id, puntaje 1–5, comentario) — opcional

**Promociones y premios**
- `promociones` (nombre, descripcion, imagen, tipo, valor, alcance [TODAS, SEDES, CANCHAS, MODALIDAD], modalidad, fecha_inicio, fecha_fin, dias_semana, hora_inicio, hora_fin, codigo UNIQUE NULL, usos_maximos, usos_por_cliente, prioridad, activa)
- `promocion_sedes`, `promocion_canchas` (tablas puente)
- `promocion_usos` (promocion_id, reserva_id, cliente_id, descuento_aplicado, fecha)
- `programa_premios_config` (activo) — o clave en `configuracion`
- `reglas_premio` (nombre, reservas_requeridas, ventana_dias NULL, solo_completadas, duracion_minima_min, vigente_desde, vigente_hasta, activa)
- `premios` (nombre, descripcion, imagen, stock NULL, activo)
- `regla_premio_opciones` (regla_id, premio_id) — premios elegibles por regla
- `progreso_premio` (cliente_id, regla_id, contador, ciclo_desde) — o calculado por consulta
- `canjes_premio` (cliente_id, regla_id, premio_id NULL hasta que elija, estado, fecha_logro, fecha_eleccion, fecha_entrega, entregado_por, vence_en)

**Torneos**
- `torneos` (nombre, descripcion, imagen, sede_id, modalidad, formato, estado, fecha_inicio, fecha_fin, cupo_equipos, valor_inscripcion, premios_texto, reglamento, modo_emparejamiento, ida_y_vuelta, tercer_puesto, num_grupos, clasificados_por_grupo, puntos_victoria, puntos_empate, puntos_derrota, criterios_desempate JSONB, marcador_wo_goles, campeon_id, subcampeon_id, tercero_id)
- `torneo_canchas` (torneo_id, cancha_id)
- `equipos` (nombre, escudo, color, delegado_nombre, delegado_telefono, usuario_responsable_id NULL, activo)
- `jugadores` (nombres, apellidos, documento, foto, posicion, usuario_id NULL)
- `equipo_jugadores` (equipo_id, jugador_id, dorsal, activo)
- `inscripciones_torneo` (torneo_id, equipo_id, estado [SOLICITADA, APROBADA, RECHAZADA, RETIRADA], semilla) — UNIQUE(torneo_id, equipo_id)
- `plantillas_torneo` (inscripcion_id, jugador_id, dorsal) — UNIQUE(torneo, jugador) vía restricción o validación
- `fases` (torneo_id, tipo [GRUPOS, LIGA, ELIMINATORIA], orden, nombre)
- `grupos` (fase_id, nombre) y `grupo_equipos` (grupo_id, inscripcion_id)
- `partidos` (torneo_id, fase_id, grupo_id NULL, ronda, numero_llave, local_id NULL, visitante_id NULL, siguiente_partido_id NULL, lado_en_siguiente [LOCAL, VISITANTE], cancha_id, reserva_id NULL, inicio, estado, goles_local, goles_visitante, penales_local, penales_visitante, ganador_id, es_bye, es_wo, figura_jugador_id, arbitro)
- `eventos_partido` (partido_id, equipo_id, jugador_id, tipo [GOL, GOL_PENAL, AUTOGOL, ASISTENCIA, AMARILLA, ROJA, FALTA], minuto, evento_relacionado_id [asistencia → gol], observacion)
- Vistas SQL: `v_tabla_posiciones`, `v_goleadores`, `v_asistidores`, `v_tarjetas`

**Transversales**
- `notificaciones` (usuario_id, tipo, titulo, mensaje, leida, entidad, entidad_id, fecha)
- `audit_log` (actor_id, actor_rol, accion, entidad, entidad_id, cambios JSONB, motivo, ip, fecha) — índice por (entidad, entidad_id) y por fecha
- `configuracion` (clave UNIQUE, valor, tipo, descripcion)

Entregable de esta sección: migraciones Flyway `V1__...sql`, `V2__...sql`, etc., más un archivo `docs/modelo-datos.md` que explique cada tabla y relación (servirá para el modelo relacional del informe).

---

## 9. API REST

- Prefijo `/api/v1`. Recursos en plural. Rutas de admin bajo `/api/v1/admin/**`. Rutas públicas (sin token): registro, login, refresh, recuperar contraseña, sedes/canchas/disponibilidad en modo lectura, promociones vigentes, torneos públicos.
- DTOs de entrada y salida (nunca exponer entidades JPA). Validación con Bean Validation.
- Paginación (`page`, `size`, `sort`) en todos los listados.
- Errores con `ProblemDetail` y códigos de negocio (`RESERVA_SOLAPADA`, `FUERA_DE_HORARIO`, `POLITICA_CANCELACION`, `CONFLICTOS_SIN_CONFIRMAR`…) que el front pueda traducir a mensajes amigables.
- `409 Conflict` para choques de horario y conflictos de versión.
- Idempotencia en la creación de reservas (cabecera `Idempotency-Key`) para evitar dobles reservas por doble clic.
- Endpoints de previsualización (`/preview`) para bloqueos, cambio de estado de cancha y desactivación de clientes.
- Ejemplos orientativos:
  - `GET /availability?sedeId=&fecha=&modalidad=`
  - `POST /bookings`, `GET /me/bookings`, `PATCH /bookings/{id}/reschedule`, `POST /bookings/{id}/cancel`, `GET /bookings/{id}/history`, `GET /bookings/{id}/ics`
  - `POST /bookings/series`, `POST /waitlist`
  - `GET/POST/PUT /admin/venues`, `/admin/courts`, `/admin/rates`, `/admin/blocks` (+ `/preview`)
  - `GET /admin/bookings`, `POST /admin/bookings`, `POST /admin/bookings/{id}/payments`, `PATCH /admin/bookings/{id}/status`
  - `/admin/promotions`, `/admin/rewards/rules`, `/admin/rewards/catalog`, `/admin/rewards/redemptions`, `/me/rewards`
  - `/admin/tournaments`, `/admin/tournaments/{id}/fixture/generate`, `/admin/matches/{id}/result`, `/admin/matches/{id}/events`, `/tournaments?estado=ACTIVO`, `/tournaments/{id}/standings`, `/tournaments/{id}/bracket`, `/tournaments/{id}/top-scorers`
  - `/admin/users/{id}/deactivate` (+ `/preview`), `/admin/stats/revenue`, `/admin/stats/occupancy`, `/admin/stats/peak-hours`, `/admin/audit`
  - `/me/notifications`
- Swagger UI disponible en desarrollo con autenticación por Bearer configurada.

---

## 10. Seguridad

- **Access token JWT** de vida corta (15 min) firmado (HS256 con secreto ≥ 256 bits desde variable de entorno, o RS256). Claims: `sub` (id), `rol`, `tv` (token_version), `exp`, `iat`, `jti`.
- **Refresh token** opaco y aleatorio (7–14 días), guardado **hasheado** en BD, enviado en **cookie `HttpOnly`, `Secure`, `SameSite=Strict`**, con **rotación** en cada uso y detección de reutilización (si se reutiliza uno ya rotado, revocar toda la familia).
- El access token se guarda **en memoria** en el front (no en localStorage).
- Al desactivar un usuario o cambiar su contraseña se incrementa `token_version`, invalidando los tokens existentes.
- Contraseñas con BCrypt(12)/Argon2id. Nunca devolver el hash en ninguna respuesta. Nunca registrar contraseñas ni tokens en logs.
- **Rate limiting** en login, registro y recuperación de contraseña; bloqueo temporal tras N intentos fallidos.
- Mensajes de error de login genéricos (no revelar si el correo existe).
- Autorización por rol con `@PreAuthorize` **y** verificación de propiedad en cada recurso del cliente.
- CORS restringido al origen del frontend. Cabeceras de seguridad (CSP básica, X-Content-Type-Options, etc.) en Nginx.
- Validación y sanitización de toda entrada; consultas parametrizadas (JPA) — nada de SQL concatenado.
- Subida de imágenes: validar tipo y tamaño, nombre aleatorio, guardar en volumen local (o almacenamiento tipo S3 después).
- Pruebas automáticas de seguridad básicas: un cliente no puede ver/modificar reservas ajenas ni acceder a `/admin/**`.

---

## 11. Frontend (provisional pero bien estructurado)

**Principios**
- Mobile-first (la mayoría de clientes reservará desde el celular) y responsive en escritorio para el panel admin.
- Tokens de diseño en `tailwind.config` (colores de marca, tipografía, radios) para poder cambiar el estilo luego sin tocar componentes. Paleta sugerida: verde césped como primario, con modo oscuro opcional.
- Estructura por *features* (`src/features/bookings`, `src/features/tournaments`…), componentes compartidos en `src/components/ui`.
- Estados de carga (skeletons), vacíos y de error en todas las vistas. Toasts para confirmaciones. Modales de confirmación para acciones destructivas.
- Accesibilidad básica (labels, foco, contraste).

**Páginas públicas / cliente**
- Landing (hero, cómo funciona, sedes, promociones vigentes, torneos activos, llamado a reservar).
- Registro, login, recuperar/restablecer contraseña, verificar correo.
- Sedes y canchas (listado + detalle).
- Disponibilidad y reserva (grilla, flujo de reserva en pasos: cancha → horario → resumen → confirmación).
- Mis reservas (próximas / historial / canceladas, detalle con línea de tiempo, reprogramar, cancelar, comprobante, .ics, compartir).
- Reservas recurrentes y lista de espera.
- Promociones.
- Mis premios (progreso, elegir premio, historial).
- Torneos (activos / próximos / finalizados) → detalle con pestañas: Posiciones, Grupos, Llaves, Calendario, Goleadores, Asistidores, Tarjetas; detalle de partido, equipo y jugador.
- Notificaciones.
- Perfil (datos, contraseña, preferencias, estadísticas personales, favoritos).

**Panel administrador** (layout con barra lateral)
- Inicio (KPIs del día + accesos rápidos).
- Agenda/calendario de reservas + tabla de reservas.
- Sedes, canchas, tarifas, festivos.
- Horarios de operación y bloqueos (con modal de conflictos).
- Clientes.
- Promociones.
- Premios (configuración del programa, reglas, catálogo, canjes pendientes).
- Torneos (asistente de creación paso a paso: datos → formato y reglas → equipos → generación/armado manual del fixture → programación de partidos), gestión de equipos y jugadores, registro de resultados y eventos.
- Estadísticas.
- Auditoría.
- Configuración.

---

## 12. Calidad y buenas prácticas (evidencia para la nota)

- Pruebas unitarias del dominio: cálculo de precios (franjas, festivos, promociones), validación de disponibilidad, política de cancelación/reprogramación, generación de fixture (liga par/impar, eliminación con byes, grupos), tabla de posiciones con desempates, avance en llaves, progreso de premios.
- Pruebas de integración con Testcontainers: reserva concurrente, flujo de bloqueo con conflictos, seguridad por rol/propiedad.
- Pruebas del front con Vitest + Testing Library en componentes clave (opcional: un flujo E2E con Playwright para la demo).
- Cobertura objetivo razonable en dominio (≥ 70 %).
- Logs estructurados, `@Transactional` bien delimitado, sin N+1 (usar `fetch join`/EntityGraph donde haga falta).
- README con: requisitos, cómo correr con Docker, variables de entorno, usuarios de prueba, arquitectura resumida, enlace a Swagger.
- CI en GitHub Actions; ramas `main` (estable) / `develop` / `feature/*`; PRs con descripción.

---

## 13. Datos de demostración (para INNOVASOFT)

Un seed activable solo en perfil `dev`/`demo`:
- 1 admin y ~15 clientes con nombres colombianos realistas.
- 2 sedes en Neiva con diferente configuración (ej. una con 3 canchas de F5 y 1 de F8 techada; otra con 2 de F7).
- Tarifas por franja, festivos cargados.
- ~3 meses de reservas históricas (completadas, canceladas por cliente y por admin, no asistencias, pagos) para que las estadísticas y el mapa de calor se vean con datos.
- Reservas futuras, un bloqueo recurrente y uno puntual.
- 2 promociones activas (una por porcentaje en franja de la mañana y una con código).
- Programa de premios activo con 1–2 reglas, 4 premios y algunos canjes en distintos estados.
- 1 torneo **finalizado** (eliminación directa con byes, campeón definido) y 1 torneo **en curso** (grupos + eliminación) con resultados, goleadores, asistencias y tarjetas.
- Script/guion corto de demo en `docs/guion-demo.md`.

---

## 14. Registro obligatorio del uso de IA

La rúbrica exige un apartado detallado sobre la IA usada. Desde el primer día mantén en `docs/ia/`:
- `README.md`: herramienta(s) y modelo(s) usados, para qué se usó cada uno, y cómo se integró en el proceso de desarrollo.
- `prompts.md`: este prompt maestro y cada prompt relevante usado después (fecha, objetivo, prompt, resultado resumido, qué se aceptó o se corrigió).
- `reglas-y-controles.md`: reglas dadas a la IA (este documento, archivos `CLAUDE.md`/`AGENTS.md` si se usan), mecanismos de control humano (revisión de PRs, pruebas obligatorias, CI, no aceptar código sin pruebas), reglas de negocio que la IA debía respetar, y limitaciones/errores detectados.
- `agentes-y-skills.md`: agentes, subagentes, skills, herramientas o integraciones (MCP, etc.) usados, si aplica.
- Al final de cada fase, agrega una entrada breve con lo que hiciste.

---

## 15. Plan por fases (hay ~1 mes hasta el 19 de noviembre de 2026)

Prioridad MVP (imprescindible para la demo) primero:

1. **Fase 0 — Base (día 1–2):** monorepo, Docker Compose, Flyway V1, CI, estructura hexagonal, configuración de seguridad, README, `docs/ia/`.
2. **Fase 1 — Autenticación y usuarios:** registro, login, refresh con rotación, logout, recuperación de contraseña, perfil, roles, seed de admin, activar/desactivar clientes.
3. **Fase 2 — Sedes, canchas, tarifas, horarios, festivos.**
4. **Fase 3 — Disponibilidad y reservas del cliente** (crear, mis reservas, reprogramar, cancelar, políticas, historial/auditoría, notificaciones in-app).
5. **Fase 4 — Reservas del admin y bloqueos con conflictos** (agenda, crear para cliente/mostrador, cancelar/reprogramar con motivo, pagos, bitácora de auditoría).
6. **Fase 5 — Torneos** (equipos, jugadores, formatos, fixture manual/aleatorio, byes, resultados, eventos, tablas, llaves, vista del cliente).
7. **Fase 6 — Estadísticas** (ingresos, ocupación, horas pico, exportación).
8. **Fase 7 — Promociones y programa de premios.**
9. **Fase 8 — Extras** (recurrentes, lista de espera, correos, .ics, favoritos, calificaciones, solicitud de inscripción a torneos, canchas compuestas).
10. **Fase 9 — Pulido y demo** (seed completo, pruebas finales, guion de demo, revisión de seguridad, documentación).

Si el tiempo se acorta, avísame qué recortar de la Fase 8 antes que de las demás.

---

## 16. Fuera de alcance (esta versión)

- Pasarela de pagos real (solo registro manual de pagos).
- Aplicación móvil nativa (la web es responsive).
- Multiempresa con administradores separados por negocio (a menos que lo confirme en las preguntas).
- Envío de WhatsApp/SMS (dejar el puerto preparado).

---

## 17. Definición de terminado

Una funcionalidad está terminada cuando: tiene endpoint documentado en Swagger, validaciones y manejo de errores, pruebas del dominio, verificación de permisos, registro en auditoría si modifica datos, pantalla funcional en el front con estados de carga/vacío/error, y entrada en `docs/ia/`.

---

## 18. Antes de empezar: pregúntame

Hazme **todas** las preguntas que necesites (agrúpalas y numéralas). Como mínimo:

1. ¿El docente acepta **arquitectura hexagonal + cliente-servidor en contenedores separados** como "no monolítica", o se requieren **microservicios**?
2. ¿Somos un grupo de 2 o de hasta 5 personas? ¿Cómo se reparte el trabajo (back/front)? ¿Usaremos GitHub Projects o Trello?
3. ¿Un solo negocio con **varias sedes** administradas por los mismos admins, o varios negocios independientes?
4. Duración del bloque de reserva por defecto (60 min, 90 min, configurable por cancha).
5. Política de cancelación y reprogramación: ¿cuántas horas antes?, ¿cuántas reprogramaciones?, ¿se maneja **abono/anticipo** obligatorio?
6. ¿Las reservas quedan **confirmadas de inmediato** o requieren aprobación del admin o un abono para confirmarse?
7. ¿Las reservas pasadas se marcan **completadas automáticamente** o siempre las marca el admin?
8. ¿Implementamos ahora **canchas compuestas/divisibles**?
9. ¿El cliente puede **solicitar inscripción** de su equipo a un torneo, o solo el admin inscribe equipos?
10. ¿Se necesita **envío real de correos** en la demo (¿qué proveedor SMTP?) o basta con Mailpit + notificaciones in-app?
11. ¿Dónde se va a **desplegar** (solo local con Docker, un VPS, Render/Railway, AWS)?
12. ¿Tienen **identidad visual** (logo, colores) de Goltime o la propongo?
13. ¿Preferencias de idioma en el código (inglés o español)?
14. ¿Ya tienen un prototipo en Figma que deba seguir el front?
15. ¿Hay alguna funcionalidad de este documento que quieran **quitar o priorizar**?

Cuando tengas mis respuestas, propón: (a) la arquitectura final con un diagrama en texto/Mermaid, (b) el modelo de datos final, (c) el plan de fases ajustado. Espera mi aprobación y luego empieza por la Fase 0.
