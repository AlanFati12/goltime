# ADR 0019: El administrador cambia una reserva a cualquier estado

- Estado: aceptada (24/oct/2026), rama `feat/bot-whatsapp`
- Contexto: hasta ahora el admin solo podía marcar COMPLETADA (si la reserva ya terminó) o NO_ASISTIO (si ya empezó), además de aprobar, rechazar y cancelar con sus propias reglas. La persona pidió que el administrador pueda llevar **cualquier** reserva a **cualquier** estado, aunque no haya llegado la fecha («que el admin tenga poder de hacer todo»).

## Decisiones

- `PATCH /api/v1/admin/reservas/{id}/estado` acepta los siete estados y exige `motivo` (máx. 500). En el dominio, `Reserva.cambiarEstadoPorAdmin` no aplica reglas de tiempo; solo rechaza el mismo estado (`ESTADO_INVALIDO`) o un motivo vacío. Ajusta lo que acompaña a cada estado: quién aprobó, canceló o rechazó y por qué (y lo borra al salir de CANCELADA o RECHAZADA; el historial lo conserva), y el plazo de una PENDIENTE (el normal: 12 h o 2 h antes del inicio).
- **Ocupación de la cancha.** Conservan su fila en `ocupaciones_cancha` PENDIENTE, CONFIRMADA, **COMPLETADA y NO_ASISTIO** (`EstadoReserva.conservaOcupacion()`); la liberan RECHAZADA, EXPIRADA y CANCELADA. Pasar de un estado que libera a uno que conserva vuelve a ocupar la cancha (y sus dependientes); si el horario ya lo tomó otra reserva o un bloqueo, la restricción `EXCLUDE` lo impide, se responde `RESERVA_SOLAPADA` (409) y no cambia nada. Pasar a un estado que libera suelta la ocupación y publica `ReservaLiberadaEvento` (lista de espera, usos de promociones y premios usados).
- **Desviación del encargo:** se pidió que «pasar a un estado no activo libere la ocupación». No se hace para COMPLETADA ni NO_ASISTIO, por dos razones: (1) hoy las reservas jugadas ya conservan su ocupación (la tarea automática nunca la suelta y las estadísticas cuentan esos estados como horario usado); (2) si una COMPLETADA futura soltara la cancha, otra reserva podría tomar el mismo horario y quedarían dos «jugadas» en la misma cancha a la vez (ingresos dobles), y devolverla a CONFIRMADA chocaría siempre. Si el cliente no va a venir, lo que libera el horario es CANCELADA.
- **Pago:** al liberar con dinero abonado queda `REEMBOLSO_PENDIENTE` (como al cancelar); al volver a ocupar, el estado del pago se recalcula con lo que sigue pagado (ABONADO, PAGADO o PENDIENTE).
- **Premios e ingresos:** entrar a COMPLETADA publica `ReservaCompletadaEvento` y salir de ella `CompletadaRevertidaEvento`, los mismos que usa `premios` para otorgar o revocar. Solo COMPLETADA cuenta (regla 6 intacta).
- **Trazabilidad:** historial con la acción nueva `ESTADO_CAMBIADO` (migración V17 amplía el `CHECK`), motivo, estado antes y después y si la cancha se ocupó o liberó; auditoría `CAMBIAR_ESTADO_RESERVA` con los mismos datos y el motivo.
- **Avisos al cliente:** confirmar (si aún no se juega, con el .ics), rechazar y cancelar usan el aviso de siempre; los demás destinos usan `RESERVA_ESTADO_<ESTADO>` con el motivo. Todos publican `AvisoReservaEvento`, y el bot de WhatsApp tiene texto para el aviso genérico.
- **Previsualización:** `GET /api/v1/admin/reservas/{id}/estado/previsualizar?estado=` dice, sin cambiar nada, si ocupará o liberará la cancha, si el horario está libre, si entra o sale de premios, si queda reembolso pendiente, si la tarea automática la moverá enseguida y si se avisa al cliente. La página de detalle del admin reemplaza «Marcar como completada / no asistió» por **Cambiar estado** (lista de estados, efecto y motivo) y no deja confirmar si hay choque.

## Consecuencias

- Las tareas automáticas siguen corriendo: una CONFIRMADA cuya hora ya pasó se completa sola, y una PENDIENTE cuyo plazo ya venció expira sola. La previsualización lo advierte; para dejarla jugada o no asistida, el admin elige ese estado directamente.
- Reactivar una reserva con descuento conserva el precio congelado, pero el uso de la promoción o el premio que se liberó al cancelarla no se vuelve a consumir. Se acepta: es una decisión explícita del admin y queda en el historial.
- Aprobar, rechazar y cancelar siguen existiendo con sus reglas (son los atajos del día a día); el cambio de estado es la herramienta de corrección sin límites.
