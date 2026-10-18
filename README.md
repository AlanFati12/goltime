# Goltime

Aplicación web para la gestión de canchas sintéticas de fútbol: sedes, canchas, reservas con aprobación del administrador, torneos, estadísticas, promociones y premios.

Proyecto final de Ingeniería de Software, Universidad Surcolombiana (USCO). Se expone en **INNOVASOFT IX, 19 de noviembre de 2026**.

> Estado: **Fase 1 (autenticación y usuarios)** completada, sobre la Fase 0 (base). Ver [docs/propuesta-inicial.md](docs/propuesta-inicial.md) para el alcance y el orden de fases.

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
