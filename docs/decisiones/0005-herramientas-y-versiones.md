# ADR 0005: Herramientas y versiones

- Estado: aceptada
- Backend: Spring Boot **3.5.16**, Java 21, PostgreSQL 16, Flyway, springdoc-openapi **2.8.17** (la serie 3.x es para Spring Boot 4), MapStruct 1.6.3, Bucket4j 8.21, ArchUnit 1.5.1, Testcontainers (versión de Spring Boot), JaCoCo.
- Frontend: Vite + React 19 + TypeScript, **Tailwind CSS v4** (plugin de Vite y tokens en `@theme`), React Router, TanStack Query, Zustand, React Hook Form + Zod, Axios, Recharts, date-fns y date-fns-tz, Vitest + Testing Library.
- Cambios respecto al prompt maestro:
  - **oxlint** en lugar de ESLint: es el linter que trae la plantilla actual de Vite. Prettier se agregó con el plugin de Tailwind.
  - **shadcn/ui** no se instaló todavía; se creó `cn()` y un `Boton` con `class-variance-authority`, que es la base de shadcn. Los componentes de shadcn se agregan por demanda desde la Fase 1.
- Entorno local: no hay JDK 21 instalado. En la máquina de desarrollo Maven corre con JDK 26 y compila con `--release 21` (las pruebas usan `-Dnet.bytebuddy.experimental=true`; JaCoCo avisa de clases del JDK que no puede instrumentar, sin afectar el resultado). La imagen Docker y el CI usan Java 21 real.
