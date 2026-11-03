# ADR 0021: El bot agrupa los mensajes seguidos de una persona

- Estado: aceptada
- Contexto: la gente escribe una idea en varios mensajes cortos («hola» / «quiero reservar» / «para mañana a las 8»). Cada mensaje disparaba una ejecución del workflow y el bot respondía a cada uno por separado.
- Decisión:
  1. **Silencio de 6 segundos** (`SILENCIO_S` en `integraciones/n8n/construir_workflows.py`). Cada mensaje de texto se guarda y espera ese tiempo; si en el intervalo llegó otro del mismo chat, esa ejecución termina sin responder. La última se lleva todos los pendientes, en el orden en que se enviaron, y los une con saltos de línea como un solo mensaje para el agente. Las respuestas siguen saliendo unos 10 s después del último mensaje: el tiempo de «escribiendo…» ya cuenta desde ahí.
  2. **Estado en PostgreSQL, no en n8n.** n8n no comparte los datos estáticos entre ejecuciones simultáneas hasta que terminan, así que un buffer en memoria no sirve. Los pendientes viven en la tabla `goltime_buffer_mensajes`, dentro de la base que ya usa Evolution (no toca las tablas de Evolution). La unión es una sola sentencia `DELETE … RETURNING` condicionada al silencio, por lo que si dos ejecuciones coinciden solo una recibe los mensajes.
  3. **Lo que no es texto no se agrupa** (audio, imágenes…): pasa directo y recibe su respuesta fija inmediata sin alterar el grupo.
  4. **El agente sabe leer varias líneas**: el prompt le indica que son mensajes seguidos y que debe responderlos todos en uno.
  5. El script `scripts/whatsapp/n8n-importar.sh` crea la tabla y la credencial `Goltime · Buffer de mensajes` (idempotente).
- Prueba: con n8n 2.40.5 real, PostgreSQL y dos servidores falsos de la API y de Evolution (el agente de OpenAI cambiado por un eco). Casos verificados: 3 mensajes seguidos → 1 respuesta; ráfaga de 10 mensajes en 2 s → 1 respuesta en orden; dos personas a la vez sin mezclarse; mensaje suelto; dos mensajes con 9 s de pausa → 2 respuestas; un audio entre dos textos → aviso inmediato y los textos agrupados; tabla vacía al final.
- Consecuencias: la respuesta puede tardar hasta unos 6 s más cuando la persona envía un solo mensaje y el agente es muy rápido; subir o bajar `SILENCIO_S` cambia el equilibrio entre agrupar mejor y responder antes. Si un mensaje llega mientras el agente ya está procesando el grupo anterior, se trata como un grupo nuevo.
