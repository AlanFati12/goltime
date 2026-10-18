# ADR 0015: Planilla del partido, revisión de solicitudes, tablero y base limpia

- Estado: aceptada (rama `feat/torneos-y-tablero`)
- Contexto: al probar, el registro de goles, asistencias y tarjetas existía pero no se encontraba (había que entrar al torneo, abrir la pestaña de partidos, tocar la tarjeta del partido y tener el torneo «En curso»). Además el administrador no podía ver la plantilla ni quién pedía una inscripción antes de aprobarla, los jugadores no se podían corregir, el cliente no podía quitar filas de jugadores y el tablero mostraba poca información.
- Decisiones:
  1. **Planilla del partido**: en la página del partido, una columna por equipo con sus jugadores y botones Gol, Asistencia, Amarilla y Roja. La asistencia rápida se liga sola al último gol del mismo equipo sin asistencia. El marcador se propone desde la planilla; si el partido ya tiene resultado y un gol nuevo lo supera, se ofrece actualizar el marcador. Si el torneo no está en curso se ofrece iniciarlo desde ahí.
  2. **El minuto del evento es opcional** (migración V12). Los eventos sin minuto van al final de la línea de tiempo.
  3. **Lista de partidos en la gestión del torneo** con acceso directo a la planilla («Cargar resultado» / «Editar»).
  4. **Revisión de solicitudes**: `GET /api/v1/admin/torneos/{id}/solicitudes/{inscripcionId}` devuelve equipo, cliente responsable (nombre, correo, celular) y plantilla. El administrador la ve en un diálogo, puede corregir la plantilla y luego aprueba o rechaza.
  5. **Jugadores editables** por el administrador (nombre, documento, posición y dorsal) en la plantilla habitual del equipo y en la de cada torneo. El cliente puede agregar y quitar filas, con posición y dorsal, al pedir la inscripción.
  6. **Tablero**: `GET /api/v1/admin/estadisticas/tablero` (lectura SQL en hora de Bogotá) con hoy, saldos por cobrar y reembolsos, próximos 7 días, estado de cada cancha ahora, pendientes de torneos y clientes.
  7. **Base de datos**: se mantiene en Docker (mismo entorno para todos, igual que las pruebas con Testcontainers); pgAdmin se conecta a `localhost:5433`. `scripts/reiniciar-base.sh` deja la base vacía con un administrador y un cliente de prueba.
  8. **Una sola URL** para usar la aplicación: <http://localhost:8081>.
