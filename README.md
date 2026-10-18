# Goltime

Aplicación web para la gestión de canchas sintéticas de fútbol: sedes, canchas, reservas con aprobación del administrador, torneos, estadísticas, promociones y premios.

Proyecto final de Ingeniería de Software, Universidad Surcolombiana (USCO). Se expone en **INNOVASOFT IX, 19 de noviembre de 2026**.

> Estado: **Fase 6 (estadísticas)** completada, sobre las Fases 0 a 5. Ver [docs/propuesta-inicial.md](docs/propuesta-inicial.md) para el alcance y el orden de fases.

## Arquitectura resumida

Cliente-servidor en tres niveles, cada pieza en su propio contenedor:

| Contenedor | Tecnología | Función |
|---|---|---|
| `web` | React + Vite + TypeScript + Tailwind, servido por Nginx | SPA. Nginx también hace proxy de `/api` hacia la API (mismo origen) |
| `api` | Spring Boot 3.5 · Java 21 · arquitectura hexagonal modular | API REST `/api/v1` con JWT |
| `db` | PostgreSQL 16 | Datos. El esquema lo manejan migraciones Flyway |
| `mailpit` | Mailpit | Bandeja de correos de desarrollo |

Backend organizado por módulos de negocio (`autenticacion`, `usuarios`, `sedes`, `reservas`, `promociones`, `premios`, `torneos`, `notificaciones`, `auditoria`, `estadisticas`), cada uno con `dominio/`, `aplicacion/` e `infraestructura/`. Un test de ArchUnit impide que se viole esa separación. Detalle y diagramas en [docs/propuesta-inicial.md](docs/propuesta-inicial.md) y [docs/decisiones/](docs/decisiones/).

## Requisitos

- Docker Desktop (con Docker Compose v2)
- Para desarrollo sin Docker: Java 21, Maven 3.9+, Node 24+

## Cómo correrlo con Docker

```bash
cp .env.example .env      # y edita las claves (JWT_SECRETO de 32+ caracteres)
docker compose up --build
```

| Servicio | URL |
|---|---|
| Aplicación web | <http://localhost:8081> |
| API (estado) | <http://localhost:8080/api/v1/publico/estado> |
| Swagger UI (solo perfil `dev`) | <http://localhost:8080/swagger-ui.html> |
| Correos de desarrollo (Mailpit) | <http://localhost:8025> |
| PostgreSQL | `localhost:5433` |

## Variables de entorno

Todas están documentadas en [.env.example](.env.example). Nunca se suben secretos al repositorio (`.env` está en `.gitignore`).

| Variable | Para qué |
|---|---|
| `DB_NOMBRE`, `DB_USUARIO`, `DB_CLAVE` | Base de datos |
| `JWT_SECRETO` | Firma de tokens (HS256), mínimo 32 caracteres. La API no arranca sin él |
| `ADMIN_CORREO`, `ADMIN_CLAVE` | Administrador inicial (lo crea la Fase 1) |
| `SMTP_*`, `CORREO_REMITENTE` | Correo. Por defecto apunta a Mailpit; para correo real basta cambiar estos valores |
| `CORS_ORIGENES` | Orígenes del front permitidos |
| `PERFIL` | Perfil de Spring: `dev`, `test`, `prod` |

## Desarrollo sin Docker (opcional)

```bash
# Base de datos y correo en Docker, API y front en tu máquina
docker compose up -d db mailpit

cd backend && DB_URL=jdbc:postgresql://localhost:5433/goltime DB_CLAVE=... JWT_SECRETO=... mvn spring-boot:run
cd frontend && npm install && npm run dev     # http://localhost:5173, con proxy a la API
```

## Pruebas

```bash
cd backend && mvn verify      # unitarias, ArchUnit e integración (Testcontainers: necesita Docker)
cd frontend && npm test && npm run lint && npm run build
```

## Convenciones

- Código, endpoints, tablas y columnas **en español**; textos de la interfaz en español (Colombia). Ver [ADR 0002](docs/decisiones/0002-idioma-y-convenciones.md).
- Moneda COP sin decimales (`$ 80.000`); fechas en UTC en base de datos y **America/Bogota** en pantalla.
- Commits con [Conventional Commits](https://www.conventionalcommits.org/es/) (`feat:`, `fix:`, `docs:`, `test:`, `chore:`).
- Ramas: `main` (estable), `develop`, `feature/*`.
- Definición de terminado: endpoint documentado en Swagger, validaciones, pruebas de dominio, permisos verificados, auditoría si modifica datos, pantalla con estados de carga/vacío/error y entrada en `docs/ia/`.

## Documentación

- [docs/propuesta-inicial.md](docs/propuesta-inicial.md): decisiones, arquitectura, plan de fases
- [docs/decisiones/](docs/decisiones/): registros de decisiones de arquitectura (ADR)
- [docs/modelo-datos.md](docs/modelo-datos.md): modelo relacional (crece con cada fase)
- [docs/ia/](docs/ia/): registro obligatorio del uso de IA

## Usuarios de prueba

- **Administrador:** se crea en el primer arranque con `ADMIN_CORREO` y `ADMIN_CLAVE` del `.env`. Entra por <http://localhost:8081/ingresar> y llega al panel `/admin`.
- **Clientes:** se crean desde <http://localhost:8081/registro>. Los correos de verificación y recuperación se ven en Mailpit (<http://localhost:8025>).
- Los datos de demostración completos llegan en la Fase 9.

## Estadísticas (Fase 6)

| Qué | Dónde |
|---|---|
| Panel con ingresos reales, recaudo, ocupación, ticket promedio, cancelaciones, serie por día/semana/mes, mapa de horas pico, clientes frecuentes y rendimiento por cancha | Panel: `/admin/estadisticas` (periodo, sede y cancha; compara con el periodo anterior) |
| Exportar a CSV | Botón "Exportar CSV" |
| API admin | `GET /api/v1/admin/estadisticas/resumen`, `/serie`, `/canchas`, `/horas-pico`, `/clientes-frecuentes`, `/exportar` |

Solo las reservas `COMPLETADA` cuentan como ingreso real, y los partidos de torneo no suman ingresos pero sí ocupación. Detalle en el [ADR 0010](docs/decisiones/0010-estadisticas.md).

## Torneos (Fase 5)

| Qué | Dónde |
|---|---|
| Ver torneos activos, próximos y finalizados; tabla, llaves, calendario, goleadores y equipos | Web pública: `/torneos`, `/torneos/:id`, `/torneos/partidos/:id`, `/equipos/:id`, `/jugadores/:id` (y próximos partidos en la portada) |
| Equipos y jugadores reutilizables | Panel: `/admin/equipos`, `/admin/equipos/:id` |
| Crear torneos (liga, eliminación directa, grupos y eliminación) | Panel: `/admin/torneos/nuevo` |
| Inscribir equipos, plantillas, fixture, estados | `/admin/torneos/:id` |
| Programar con reserva de cancha, resultados, W.O., eventos | `/admin/partidos/:id` |
| API pública | `GET /api/v1/publico/torneos`, `.../{id}`, `.../posiciones`, `.../partidos`, `.../estadisticas`, `/publico/partidos/{id}`, `/publico/equipos/{id}`, `/publico/jugadores/{id}` |
| API admin | `/api/v1/admin/equipos`, `/admin/torneos` (+ `/inscripciones`, `/fixture`, `/estado`), `/admin/partidos/{id}` (+ `/programacion`, `/resultado`, `/wo`, `/eventos`) |

Los partidos programados reservan la cancha como cualquier reserva (origen `TORNEO`) y, si chocan con reservas de clientes, siguen el mismo flujo de confirmación que los bloqueos. Las posiciones se calculan en el backend con criterios de desempate configurables. Detalle en el [ADR 0009](docs/decisiones/0009-torneos-fixture-y-reservas-de-partidos.md).

## Administración de reservas (Fase 4)

| Qué | Dónde |
|---|---|
| Calendario por día o semana | Panel: `/admin/agenda` (reservas y bloqueos por cancha; toca un espacio vacío para reservar) |
| Listado con filtros, CSV y detalle | `/admin/reservas`, `/admin/reservas/:id` |
| Crear reservas a nombre de un cliente o de mostrador | `/admin/reservas/nueva` |
| Pagos, abonos y reembolsos | Detalle de la reserva |
| Bloqueos de horario (puntuales y recurrentes) | `/admin/bloqueos`: revisa las reservas afectadas y exige confirmarlas |
| Bitácora de auditoría | `/admin/auditoria` |
| API admin | `POST /admin/reservas`, `PATCH .../reprogramar` y `.../estado`, `GET/POST .../pagos`, `POST .../reembolsos`, `GET /admin/reservas/exportar`, `GET /admin/agenda`, `/admin/bloqueos` (+ `/previsualizar`), `GET /admin/auditoria` |

Detalle del flujo de conflictos, pagos y agenda en el [ADR 0008](docs/decisiones/0008-bloqueos-pagos-y-agenda.md).

## Reservas (Fase 3)

| Qué | Dónde |
|---|---|
| Ver horarios libres y reservar | Web: `/reservar` (grilla por cancha, próximos 7 días, filtros y precio de cada bloque) |
| Mis reservas, detalle, reprogramar, cancelar, comprobante | Web: `/mis-reservas`, `/mis-reservas/:id` |
| Aprobar o rechazar solicitudes | Panel: `/admin/reservas` |
| Notificaciones | Campana del encabezado y `/notificaciones` |
| API cliente | `POST /api/v1/reservas` (cabecera `Idempotency-Key`), `GET /api/v1/me/reservas?pestana=`, `GET /reservas/{id}`, `POST /reservas/{id}/cancelar`, `PATCH /reservas/{id}/reprogramar` (+ `/previsualizar`) |
| API pública | `GET /api/v1/publico/disponibilidad`, `/disponibilidad/semana` |
| API admin | `GET /api/v1/admin/reservas`, `POST .../{id}/aprobar`, `.../rechazar`, `.../cancelar` |

El cliente **solicita** y el administrador **aprueba**; una solicitud sin respuesta expira a las 12 h o 2 h antes del inicio. Dos reservas no pueden solaparse nunca (restricción de exclusión en PostgreSQL). Detalle en [ADR 0007](docs/decisiones/0007-reservas-aprobacion-expiracion-e-idempotencia.md).

## Catálogo (Fase 2)

| Qué | Dónde |
|---|---|
| Ver sedes, canchas, tarifas, horarios y precio de un intervalo | Web pública: `/sedes`, `/sedes/:id`, `/canchas/:id` (con calculadora de precio) |
| Administrar sedes, canchas, tarifas, horarios, fotos y festivos | Panel `/admin/sedes`, `/admin/canchas`, `/admin/festivos` |
| API pública | `GET /api/v1/publico/sedes`, `/canchas`, `/canchas/{id}/cotizacion`, `/festivos`, `/archivos/{nombre}` |
| API de administración | `/api/v1/admin/sedes`, `/admin/canchas`, `/admin/tarifas`, `/admin/festivos` |
| Fotos | Se guardan en el volumen `datos_archivos` (`DIRECTORIO_ARCHIVOS`, por defecto `/app/uploads` en Docker) |

Las reglas de precio, horarios y canchas dependientes están en [ADR 0006](docs/decisiones/0006-catalogo-sedes-canchas-precios.md).

## Autenticación (Fase 1)

| Qué | Cómo funciona |
|---|---|
| Registro | `POST /api/v1/autenticacion/registrar`. Siempre crea `CLIENTE`. Exige aceptar términos (Ley 1581) |
| Sesión | Access token JWT de 15 min en el cuerpo (el front lo guarda en memoria) y refresh token opaco en cookie `HttpOnly; SameSite=Strict` |
| Renovación | `POST /api/v1/autenticacion/refrescar` rota el refresh token. Reutilizar uno ya rotado revoca toda la familia |
| Cierre | `POST /api/v1/autenticacion/salir` revoca el refresh y borra la cookie |
| Recuperación | `POST .../olvide-clave` (siempre 202) y `POST .../restablecer-clave` con token de un solo uso de 30 min |
| Verificación de correo | Enlace por correo. Si `VERIFICACION_CORREO_OBLIGATORIA=true`, sin verificar no se puede ingresar |
| Protección | Límite de tasa por IP (Bucket4j) y bloqueo temporal tras 5 fallos de ingreso (correo + IP) |
| Desactivación | El token de un usuario desactivado o con contraseña cambiada deja de servir al instante (`version_token`) |
| Admin | `/api/v1/admin/clientes` (listar, desactivar con previsualización, reactivar, habilitar inscripción de equipos) y `/api/v1/admin/administradores` |

La documentación interactiva completa está en Swagger.
