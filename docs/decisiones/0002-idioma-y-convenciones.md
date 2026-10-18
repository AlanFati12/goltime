# ADR 0002: Idioma del código y convenciones

- Estado: aceptada
- Decisión: **todo el código en español**: clases, métodos, variables, endpoints, tablas y columnas. Los textos de la interfaz también van en español (Colombia).
- Consecuencias y convenciones derivadas:
  - Los nombres de columnas de auditoría pasan a español: `creado_en`, `actualizado_en`, `creado_por`, `actualizado_por` (el prompt maestro usaba `created_at`, etc.).
  - Endpoints en español y plural: `/api/v1/reservas`, `/api/v1/admin/canchas`. Lo público de solo lectura va bajo `/api/v1/publico/**`; la autenticación bajo `/api/v1/autenticacion/**`; lo administrativo bajo `/api/v1/admin/**`. Esto reemplaza los ejemplos en inglés del prompt maestro.
  - Términos del framework se mantienen como son (`Controller` → sufijo `Controlador`, `Repository` → `Repositorio`, `Service` → `Servicio` cuando se necesite).
  - IDs `BIGINT GENERATED ALWAYS AS IDENTITY`. Enums como `VARCHAR` con `CHECK`.
  - Fechas: `timestamptz` en UTC; conversión a America/Bogota solo en presentación (`ZonaHoraria` en backend, `formato.ts` en frontend).
