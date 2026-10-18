# Reglas dadas a la IA y controles

## Reglas

- Prompt maestro y las reglas de trabajo del proyecto (archivo de instrucciones local, no versionado) (reglas de trabajo, convenciones y reglas de negocio que no se pueden romper).
- Preguntar antes de asumir; registrar decisiones en ADR.
- Trabajo por fases; nada se sube a ningún remoto por ahora.
- Sin secretos en el código; todo por variables de entorno.

## Reglas de negocio que la IA debe respetar

Listadas en las reglas de trabajo del proyecto (solapamiento imposible, aprobación del admin, precio como instantánea, auditoría de actor, previsualización de conflictos, solo `COMPLETADA` cuenta, usuario desactivado sin acceso).

## Controles humanos y automáticos

- El humano aprueba la propuesta y cada fase antes de continuar.
- Pruebas obligatorias: unitarias de dominio, integración con PostgreSQL real (Testcontainers), ArchUnit para la arquitectura, Vitest en frontend.
- CI en GitHub Actions (escrito; se activa cuando se suba a GitHub).
- Linter (oxlint), Prettier y verificación de tipos de TypeScript.
- Revisión de las decisiones de la IA contra el prompt maestro (ver ADR 0005 para las desviaciones).

## Limitaciones y errores detectados

- Fase 0: import de Jackson 3 incorrecto para Spring Boot 3.5; limpieza de DOM faltante en pruebas del front. Ambos se detectaron con compilación y pruebas.
- El entorno local no tiene JDK 21; las pruebas corren con JDK 26 y `--release 21`. La imagen Docker y el CI usan Java 21 real.
