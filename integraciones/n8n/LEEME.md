# Workflows de n8n del bot de WhatsApp

Generados por `construir_workflows.py` (edita ese archivo, no los JSON) e instalados con:

```bash
scripts/whatsapp/n8n-importar.sh
```

| Workflow | Webhook | Qué hace |
|---|---|---|
| `Goltime · Bot WhatsApp` | `POST /webhook/goltime-whatsapp` (lo llama Evolution) | Saca el número real del mensaje, pide el contexto a la API y deja que el agente GolBot (`gpt-6-luna`, Responses API, razonamiento `low`, memoria de 10 mensajes por número) responda con 15 herramientas hacia `/api/v1/integracion/whatsapp`. Audios, imágenes, bot en pausa, cuenta desactivada y errores reciben un texto fijo sin gastar tokens. Al llegar un mensaje el número se pone «en línea» y muestra «escribiendo…»; la respuesta sale a los 10 s (o apenas termine el agente, si tarda más) y luego el número vuelve a «desconectado» (`ESPERA_MS` en el generador). |
| `Goltime · Avisos de reservas` | `POST /webhook/goltime-avisos` (lo llama la API) | Verifica la firma HMAC del cuerpo (`X-Firma-Goltime`) y reenvía el aviso por Evolution. Sin firma válida responde 401. |

Credenciales (las crea el script; los secretos salen de `.env`):

- `Goltime · API del bot`: cabecera `X-Clave-Integracion`.
- `Goltime · Evolution API`: cabecera `apikey`.
- `Goltime · OpenAI`: se crea vacía una sola vez; pega tu API key en n8n › Credentials. Volver a correr el script no la toca.

Por qué el número va fijo: cada herramienta manda `X-WhatsApp-Numero` desde el nodo «Extraer mensaje», no desde el modelo. Así nadie puede pedirle al bot que actúe sobre las reservas de otro número.
