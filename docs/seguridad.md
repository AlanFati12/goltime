# Revisión de seguridad (Fase 9)

Revisión hecha sobre el código de las Fases 0 a 9 con la lista de la sección 10 del [prompt maestro](ia/prompt-maestro.md). Cada punto indica **dónde se cumple** y **cómo se verifica** (prueba automática o revisión manual).

## Autenticación y sesiones

| Requisito | Estado | Dónde / verificación |
|---|---|---|
| Access token JWT corto, firmado HS256 con secreto de entorno | Cumple | `EmisorTokenAccesoJwt`; el arranque falla sin `JWT_SECRETO` ≥ 32 caracteres. Pruebas de tokens falsificados y vencidos en `AutenticacionIT` |
| Refresh token opaco, hasheado, en cookie `HttpOnly` + `SameSite=Strict`, con rotación y detección de reutilización | Cumple | `ServicioSesion`, `CookieRefresco`; `Secure` se activa con `COOKIE_SEGURA=true` (obligatorio al poner HTTPS). Prueba de reutilización revoca la familia |
| Access token solo en memoria en el navegador | Cumple | `almacenSesion.ts` (zustand, sin `localStorage`) |
| Desactivar la cuenta invalida los tokens | Cumple | `version_token` validado en cada petición (`ValidadorEstadoUsuario`); prueba de desactivación inmediata |
| Sin contraseñas: la identidad la afirma Google | Cumple | ADR 0023. `VerificadorGoogleNimbus` valida firma (claves públicas de Google), emisor, destinatario (ID de cliente) y vigencia; un correo que Google no confirma se rechaza. No existe columna de contraseña (V18); los DTO no exponen el identificador de Google. Pruebas en `AutenticacionIT` |
| Límite de peticiones en el ingreso | Cumple | `FiltroLimiteTasa` sobre `/autenticacion/google`; no hay contraseñas que adivinar |
| El modo de prueba nunca llega a producción | Cumple | `ConfiguracionGoogle` se niega a arrancar con `GOOGLE_MODO_PRUEBA=true` y el perfil `prod`; `docker-compose.produccion.yml` ni siquiera pasa la variable (`ConfiguracionGoogleTest`) |
| Nadie se vuelve administrador entrando | Cumple | Un correo nuevo solo crea `CLIENTE`; el rol `ADMIN` lo da el administrador inicial o otro administrador por correo (`AutenticacionIT`, `GestionClientesIT`) |

## Autorización

| Requisito | Estado | Dónde / verificación |
|---|---|---|
| Rutas de administración solo para ADMIN | Cumple | `ConfiguracionSeguridad` + `@PreAuthorize` en cada controlador. **`RutasProtegidasIT` recorre todas las rutas `/api/v1/admin/**` (más de 100) y exige 401 sin sesión y 403 a un cliente: una ruta nueva sin proteger rompe la prueba** |
| Rutas de cliente exigen sesión | Cumple | La misma prueba recorre el resto de rutas no públicas |
| Lo público es solo lectura | Cumple | La misma prueba: ninguna ruta `/api/v1/publico/**` admite escritura |
| Verificación de propiedad (sin IDOR) | Cumple | Reservas, calificaciones, esperas, solicitudes de torneo, calendario `.ics`: lo ajeno responde 404 como si no existiera (pruebas en `SolicitudReservasIT`, `SeriesYListaEsperaIT`, `SolicitudesTorneoIT`, `FavoritosYCalificacionesIT`, `PromocionesYPremiosIT`) |
| Datos personales en lo público | Cumple | Perfiles de equipo y jugador sin documento, delegado ni responsable (`EventosPartidoIT`); calificaciones con nombre de pila e inicial |

## Entrada, salida y datos

| Requisito | Estado | Dónde / verificación |
|---|---|---|
| Validación de toda entrada | Cumple | Bean Validation en los DTO y reglas de dominio que lanzan errores de negocio |
| Sin SQL concatenado con datos del usuario | Cumple | Todo con parámetros (`?`) o JPA. Las únicas concatenaciones arman listas de marcadores `?` (revisadas una a una) |
| Inyección de fórmulas en CSV | Cumple | `FormatoCsv.celda` neutraliza `=`, `+`, `-`, `@` (prueba en `AdminReservasIT` y `EstadisticasIT`) |
| Subida de imágenes | Cumple | Valida la firma del archivo, el tamaño y guarda con nombre aleatorio (`ServicioFotos`, `TipoImagen`) |
| Errores sin detalles internos | Cumple | `ManejadorErroresGlobal` devuelve `ProblemDetail` con código estable; la traza solo va al log |
| Secretos fuera del código | Cumple | Todo por variables de entorno; `.env` está en `.gitignore`; `.env.example` solo trae marcadores |
| Auditoría de acciones administrativas | Cumple | `audit_log` con actor, rol, IP y cambios antes/después; consulta en `/admin/auditoria` |

## Cabeceras y transporte

| Requisito | Estado | Dónde |
|---|---|---|
| CSP, `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`, `Permissions-Policy` | Cumple | `frontend/nginx.conf` |
| CORS restringido al origen del frontend | Cumple | `PropiedadesCors` (`CORS_ORIGENES`) |
| HTTPS | **Pendiente de despliegue** | Se termina en el proxy del VPS; al hacerlo, `COOKIE_SEGURA=true`, `URL_PUBLICA` con `https` y agregar `Strict-Transport-Security` |

## Dependencias

- Frontend: `npm audit --omit=dev` → **0 vulnerabilidades** (octubre de 2026).
- Backend: Spring Boot 3.5.x con Java 21; se recomienda correr `mvn org.owasp:dependency-check-maven:check` antes del despliegue (necesita descargar la base de datos de vulnerabilidades).

## Riesgos aceptados y siguientes pasos

1. **Datos de demostración**: las cuentas de `docs/guion-demo.md` no tienen contraseña pero solo se pueden usar con el ingreso de prueba (solo desarrollo); aun así, no cargar los datos de demostración en producción.
2. **Límite de tasa en memoria**: protege una instancia; con varias réplicas hay que moverlo a Redis o al proxy.
3. **Pagos**: se registran a mano; al integrar una pasarela, validar firmas de webhooks y no guardar datos de tarjetas.
4. **Correo**: solo se envía a cuentas con correo verificado (todas lo están: Google lo confirma). Ya no hay correos de verificación ni de recuperación de contraseña.
6. **Dependencia de Google**: si Google no está disponible nadie puede ingresar (las sesiones abiertas siguen vivas hasta que venza su renovación). Quien pierda su cuenta de Google pierde el acceso; el administrador no puede restablecerle nada.
5. **Copias de seguridad**: la base de datos y el volumen de fotos no tienen respaldo automático; definirlo en el despliegue.
