# ADR 0004: Seguridad con JWT y refresh token rotativo

- Estado: aceptada (base en Fase 0; flujo completo en Fase 1)
- Decisión:
  - Validación de JWT con `spring-boot-starter-oauth2-resource-server` (Nimbus), firma HS256 con secreto de 256 bits o más desde `JWT_SECRETO`. La API no arranca si el secreto es corto.
  - Access token de 15 minutos con claims `sub` (id), `rol`, `tv` (version_token), `exp`, `iat`, `jti`. Se guarda solo en memoria en el front.
  - Refresh token opaco aleatorio, guardado hasheado en BD (`refresh_tokens`), en cookie `HttpOnly`, `SameSite=Strict`, `Secure` fuera de dev. Rotación en cada uso y revocación de toda la familia (`familia_id`) si se reutiliza uno ya rotado.
  - Al desactivar un usuario o cambiar su clave se incrementa `usuarios.version_token`; un validador de claims en Fase 1 rechaza los tokens con `tv` viejo.
  - BCrypt costo 12 mediante `DelegatingPasswordEncoder` (permite migrar a Argon2id sin romper claves existentes).
  - Errores 401/403 como `ProblemDetail` con `codigo`. CSRF deshabilitado porque la API es sin estado con Bearer; el refresh solo se acepta por cookie `SameSite=Strict`.
  - Nginx sirve el front y hace proxy de `/api` para que ambos compartan origen.
