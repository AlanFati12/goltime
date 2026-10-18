# ADR 0004: Seguridad con JWT y refresh token rotativo

- Estado: aceptada e implementada en las Fases 0 y 1
- Decisión:
  - Validación de JWT con `spring-boot-starter-oauth2-resource-server` (Nimbus), firma HS256 con secreto de 256 bits o más desde `JWT_SECRETO`. La API no arranca si el secreto es corto.
  - Access token de 15 minutos con claims `sub` (id), `rol`, `tv` (version_token), `exp`, `iat`, `jti`. Se guarda solo en memoria en el front.
  - Refresh token opaco aleatorio, guardado hasheado en BD (`refresh_tokens`), en cookie `HttpOnly`, `SameSite=Strict`, `Secure` fuera de dev. Rotación en cada uso y revocación de toda la familia (`familia_id`) si se reutiliza uno ya rotado.
  - Al desactivar un usuario o cambiar su clave se incrementa `usuarios.version_token`; un validador de claims en Fase 1 rechaza los tokens con `tv` viejo.
  - BCrypt costo 12 mediante `DelegatingPasswordEncoder` (permite migrar a Argon2id sin romper claves existentes).
  - Errores 401/403 como `ProblemDetail` con `codigo`. CSRF deshabilitado porque la API es sin estado con Bearer; el refresh solo se acepta por cookie `SameSite=Strict`.
  - Nginx sirve el front y hace proxy de `/api` para que ambos compartan origen.

## Detalles implementados en la Fase 1

- Un `OAuth2TokenValidator` (`ValidadorEstadoUsuario`) consulta en cada petición el estado y `version_token` del usuario (una consulta liviana por clave primaria). Costo: un acceso a BD por petición autenticada; beneficio: desactivar o cambiar la contraseña cierra las sesiones al instante. Si el tráfico lo exige, se puede cachear unos segundos.
- Las sesiones se cierran por eventos de dominio (`ClaveCambiadaEvento`, `UsuarioDesactivadoEvento`): el módulo `usuarios` los publica y `autenticacion` revoca los refresh tokens en la misma transacción, sin que `usuarios` dependa de `autenticacion`.
- La renovación toma un bloqueo de escritura sobre la fila del refresh token y confirma aunque falle (`noRollbackFor`), para que la revocación de la familia por reutilización quede guardada.
- El front comparte una sola petición de renovación entre llamadas simultáneas; enviar dos veces el mismo refresh token se leería como robo.
- Bloqueo por intentos: la clave es `correo|IP`, para que un tercero no pueda bloquear la cuenta de otra persona desde otra red. El límite de tasa por IP cubre el ingreso masivo.
- Con credenciales correctas se informa si la cuenta está desactivada; con credenciales incorrectas el mensaje es siempre el mismo. El registro sí revela si un correo ya existe (409), mitigado con límite de tasa.
- Los parámetros de `server.forward-headers-strategy: native` permiten que la IP real llegue desde Nginx (`X-Forwarded-For`) y alimente el límite de tasa y la auditoría.
