#!/usr/bin/env bash
# Instala en n8n los workflows del bot de WhatsApp (ADR 0018) y sus credenciales.
#
#   scripts/whatsapp/n8n-importar.sh
#
# - Genera los workflows desde integraciones/n8n/construir_workflows.py y crea la tabla de mensajes pendientes.
# - Crea o actualiza las credenciales de la API de Goltime y de Evolution con los secretos de .env
#   (pasan por un archivo temporal que se borra al terminar; nunca quedan en el repo).
# - Crea la credencial «Goltime · OpenAI»: si hay OPENAI_API_KEY en .env la usa (y la actualiza); si no, la crea vacía
#   SOLO si no existe y la API key la pegas tú en n8n.
# - Importa, publica los workflows y reinicia n8n para que registre los webhooks.
# Se puede correr las veces que haga falta (por ejemplo después de cambiar el generador).
set -euo pipefail
cd "$(dirname "$0")/../.."
set -a; . ./.env; set +a

# En producción el .env define COMPOSE_FILE (docker-compose.produccion.yml:docker-compose.produccion-bot.yml).
if [ -n "${COMPOSE_FILE:-}" ]; then COMPOSE=(docker compose); else COMPOSE=(docker compose -f docker-compose.yml -f docker-compose.whatsapp.yml); fi
N8N=("${COMPOSE[@]}" exec -T n8n)
DESTINO=/tmp/goltime-n8n
TEMPORAL=$(mktemp -d)
trap 'rm -rf "$TEMPORAL"; "${N8N[@]}" rm -rf "$DESTINO" >/dev/null 2>&1 || true' EXIT

python3 integraciones/n8n/construir_workflows.py >/dev/null 2>&1

python3 -I - "$TEMPORAL" <<'EOF'
import json, os, sys
credenciales = [
    {"id": "GoltimeApiCred01", "name": "Goltime · API del bot", "type": "httpHeaderAuth",
     "data": {"name": "X-Clave-Integracion", "value": os.environ["WHATSAPP_CLAVE_INTEGRACION"]}},
    {"id": "GoltimeEvoCred01", "name": "Goltime · Evolution API", "type": "httpHeaderAuth",
     "data": {"name": "apikey", "value": os.environ["EVOLUTION_CLAVE_API"]}},
    # Mensajes pendientes de agrupar (ADR 0021): una tabla propia en la base de Evolution.
    {"id": "GoltimePgCred01", "name": "Goltime · Buffer de mensajes", "type": "postgres",
     "data": {"host": "evolution-db", "port": 5432, "database": "evolution", "user": "evolution",
              "password": os.environ["EVOLUTION_DB_CLAVE"], "ssl": "disable", "allowUnauthorizedCerts": False,
              "maxConnections": 10, "sshTunnel": False}},
]
json.dump(credenciales, open(os.path.join(sys.argv[1], "credenciales.json"), "w"))
json.dump([{"id": "GoltimeOpenAi001", "name": "Goltime · OpenAI", "type": "openAiApi",
            "data": {"apiKey": os.environ.get("OPENAI_API_KEY") or "pega-aqui-la-api-key-de-openai"}}], open(os.path.join(sys.argv[1], "openai.json"), "w"))
EOF

# Tabla donde el bot junta los mensajes seguidos de una persona (idempotente; no toca las tablas de Evolution).
"${COMPOSE[@]}" exec -T evolution-db psql -q -U evolution -d evolution -v ON_ERROR_STOP=1 <<'SQL'
CREATE TABLE IF NOT EXISTS goltime_buffer_mensajes (
  id      BIGSERIAL PRIMARY KEY,
  chat    TEXT NOT NULL,
  msg_id  TEXT,
  enviado BIGINT NOT NULL DEFAULT 0,
  texto   TEXT NOT NULL,
  creado  TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);
CREATE INDEX IF NOT EXISTS goltime_buffer_mensajes_chat ON goltime_buffer_mensajes (chat, creado);
SQL

"${N8N[@]}" mkdir -p "$DESTINO"
"${COMPOSE[@]}" cp "$TEMPORAL/credenciales.json" "n8n:$DESTINO/credenciales.json" >/dev/null 2>&1
"${COMPOSE[@]}" cp integraciones/n8n/workflows "n8n:$DESTINO/" >/dev/null 2>&1

"${N8N[@]}" n8n import:credentials --input="$DESTINO/credenciales.json" 2>&1 | grep -iE "import|error" || true
if [ -z "${OPENAI_API_KEY:-}" ] && "${N8N[@]}" n8n export:credentials --id=GoltimeOpenAi001 --output="$DESTINO/existe.json" >/dev/null 2>&1; then
  echo "La credencial «Goltime · OpenAI» ya existe: se conserva tu API key."
else
  "${COMPOSE[@]}" cp "$TEMPORAL/openai.json" "n8n:$DESTINO/openai.json" >/dev/null 2>&1
  "${N8N[@]}" n8n import:credentials --input="$DESTINO/openai.json" 2>&1 | grep -iE "import|error" || true
  echo "Creada la credencial «Goltime · OpenAI»: abre n8n > Credentials y pega tu API key."
fi

"${N8N[@]}" n8n import:workflow --separate --input="$DESTINO/workflows" 2>&1 | grep -iE "import|error" || true
for id in GoltimeBotWa0001 GoltimeAvisos001; do
  "${N8N[@]}" n8n publish:workflow --id="$id" 2>&1 | grep -iE "publish|error" || true
done

"${COMPOSE[@]}" restart n8n >/dev/null 2>&1
# Los mensajes que lleguen antes de que n8n termine de arrancar podrían correr con la versión anterior.
for _ in $(seq 1 60); do
  curl -sf "http://localhost:${N8N_PUERTO:-5678}/healthz" >/dev/null 2>&1 && break
  sleep 2
done
sleep 5
echo "Listo. n8n: ${N8N_URL_PUBLICA:-http://localhost:5678/}"
