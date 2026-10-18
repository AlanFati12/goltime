# ADR 0001: Arquitectura hexagonal modular con contenedores separados

- Estado: aceptada
- Contexto: la rúbrica exige una aplicación cliente-servidor con arquitectura distinta a la monolítica. El equipo es de una persona y el plazo es de pocas semanas.
- Decisión: SPA React (Nginx), API Spring Boot y PostgreSQL en contenedores separados, comunicados solo por HTTP/JSON. El backend usa arquitectura hexagonal (puertos y adaptadores) organizada por módulos de negocio, con `dominio`, `aplicacion` e `infraestructura` en cada uno. Los módulos se comunican por interfaces de aplicación o eventos de dominio. `ArquitecturaTest` (ArchUnit) verifica que el dominio no dependa de Spring/JPA y que no haya ciclos entre módulos.
- Alternativas: microservicios (gateway + auth + booking + tournament). Descartada por costo de tiempo; los módulos quedan lo bastante aislados para partirlos después si el docente lo exige.
- Riesgo conocido: el backend se despliega como una sola API (monolito modular). La justificación académica se apoya en la separación por capas, los contenedores independientes y los diagramas de componentes y despliegue.
