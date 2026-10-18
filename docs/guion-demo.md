# Guion de demostración: Goltime en INNOVASOFT IX

Duración: **10 minutos** (más 2 de preguntas). Pensado para mostrar el flujo completo de un negocio real: el cliente reserva, el administrador gestiona, el torneo corre y los números salen solos.

## Antes de empezar (5 minutos, el día anterior y 15 minutos antes)

```bash
cp .env.example .env              # una sola vez; define DB_CLAVE, JWT_SECRETO y ADMIN_CLAVE
docker compose down -v            # base de datos limpia
docker compose up --build -d      # sistema completo (db, api, web, correo de pruebas)
docker compose --profile demo run --rm demo   # carga los datos de demostración (2-3 minutos)
```

Abrir en pestañas: la app <http://localhost:8081>, el correo de pruebas <http://localhost:8025> y la documentación de la API <http://localhost:8080/swagger-ui.html>.

| Quién | Correo | Clave |
|---|---|---|
| Administrador | el `ADMIN_CORREO` de `.env` | el `ADMIN_CLAVE` de `.env` |
| Cliente (habilitada para inscribir equipos) | `camila.ortiz@demo.goltime.test` | `Demo2026*` |
| Cliente | `santiago.rojas@demo.goltime.test` | `Demo2026*` |

Los otros 14 clientes usan el patrón `nombre.apellido@demo.goltime.test` con la misma clave. Abre una ventana normal con el administrador y una de incógnito con el cliente: así se ven los dos lados a la vez.

## Qué hay cargado

2 sedes en Neiva (una con 3 canchas de F5 y una techada de F8 que depende de dos de ellas; otra con 2 de F7), tarifas por franja y festivos; 16 clientes; unos 3 meses de reservas con pagos, cancelaciones y no asistencias; reservas futuras (algunas pendientes de aprobación), una serie recurrente, un bloqueo puntual y uno recurrente; dos promociones (una automática en la mañana y el código `GOLTIME10`); dos reglas de premio con premios ganados; tres torneos: **Copa Universitaria USCO 2026** (finalizado, con descansos y campeón), **Liga Relámpago Neiva** (en curso, grupos y eliminación) y **Copa Verano Goltime** (inscripciones abiertas, con una solicitud pendiente).

## Recorrido

### 1. Lo que ve cualquiera (1 min) · sin iniciar sesión
- Portada: próximos partidos de los torneos. Menú **Sedes**: dos sedes, canchas, precios y horarios; abrir una cancha y mostrar la **calculadora de precio** (cambia por franja).
- **Promociones**: dos tarjetas; el código `GOLTIME10`.
- **Torneos** → *Liga Relámpago Neiva*: pestañas **Posiciones** (por grupo, con forma reciente), **Llaves**, **Calendario** y **Estadísticas** (goleadores, asistencias, tarjetas). Abrir un partido: marcador y línea de tiempo de eventos.

> Qué decir: «Las posiciones nunca las calcula el navegador: las calcula el servidor con los criterios de desempate del torneo.»

### 2. Un cliente reserva (2 min) · ventana de incógnito
- Ingresar como **Camila**. **Reservar** → elegir sede y día → tocar un horario libre. En el resumen escribir `GOLTIME10`: el servidor recalcula el precio con el descuento. Enviar.
- Quedó **pendiente**: «el cliente solicita y el administrador aprueba; si nadie responde, el horario se libera solo a las 12 horas o 2 horas antes, lo que ocurra primero».
- Mostrar un horario ocupado y el botón **«Avisarme si se libera»** (lista de espera).

### 3. El administrador responde (2 min) · ventana normal
- **Reservas** → la solicitud de Camila → **Aprobar**. Volver a la ventana del cliente: notificación en la campana.
- Abrir el correo de pruebas (<http://localhost:8025>): llegó la **confirmación con el archivo `.ics`** para agregarla al calendario.
- **Agenda**: vista por día con reservas y bloqueos; la reserva de la cancha F8 aparece también en las dos F5 que la componen. Intentar crear un **bloqueo** que choque con una reserva: la previsualización lista las afectadas y exige confirmar.
- **Reservas recurrentes**: revisar una serie (cada fecha marcada como disponible u ocupada) antes de crearla.

> Qué decir: «Nunca hay dos reservas solapadas: lo garantiza la base de datos con una restricción de exclusión, aun con peticiones simultáneas.»

### 4. Los números (1 min 30 s)
- **Estadísticas**: ingresos reales (solo reservas completadas), recaudado, por cobrar, ocupación, mapa de **horas pico** y clientes frecuentes; comparar con el periodo anterior; **Exportar CSV**.

### 5. Torneos desde el panel (2 min)
- **Torneos** → *Liga Relámpago Neiva* → un partido sin jugar → **Programar**: elegir cancha y hora que choquen con una reserva de cliente: se muestra la lista y hay que confirmar. Programar en un horario libre: la cancha queda reservada.
- **Registrar resultado** y un par de **eventos** (gol con asistencia, tarjeta): la tabla y los goleadores se actualizan; en la eliminatoria el ganador avanza solo por las llaves.
- *Copa Verano Goltime*: la **solicitud de inscripción** de un cliente habilitado → **Aprobar**.

### 6. Fidelidad y control (1 min)
- Como Camila: **Mi cuenta → Mis premios** (progreso y premios ganados). Al reservar, elegir un premio; solo se aplica un descuento por reserva, el de mayor valor.
- Como administrador: **Configuración** (cambiar la antelación mínima o las horas de cancelación y ver el efecto de inmediato) y **Auditoría** (quién hizo qué).

### 7. Cierre (30 s)
Arquitectura hexagonal por módulos verificada por pruebas automáticas, más de 350 pruebas de backend con base de datos real, rutas protegidas verificadas una por una, y el registro del uso de IA en `docs/ia/`.

## Plan B si algo falla
- **Sin internet**: todo corre local; solo las tipografías vienen de Google (hay alternativas del sistema).
- **La carga de datos falló a medias**: `docker compose down -v` y repetir los tres comandos.
- **Un correo no llega**: está en Mailpit aunque no salga de la máquina; recargar la página.
- **Si un cliente quedó bloqueado por intentos de ingreso**: esperar 1 minuto (límite de 10 por minuto) o usar la otra cuenta.

## Preguntas que suelen hacer
- *¿Qué pasa si dos personas reservan a la vez?* Gana la primera; la otra recibe «ese horario acaba de ser tomado».
- *¿Y los pagos en línea?* Hoy los registra el administrador (efectivo, transferencia, Nequi, Daviplata); el diseño deja el puerto listo para Wompi o Mercado Pago.
- *¿Cómo se despliega?* Con el mismo `docker compose` en un VPS, cambiando las variables de entorno (SMTP real, secreto JWT, cookie segura) y poniendo HTTPS delante.
