#!/usr/bin/env bash
# Instala en n8n los workflows del bot de WhatsApp (ADR 0018) y sus credenciales.
#
#   scripts/whatsapp/n8n-importar.sh
#
# - Genera los workflows desde integraciones/n8n/construir_workflows.py.
# - Crea o actualiza las credenciales de la API de Goltime y de Evolution con los secretos de .env
#   (pasan por un archivo temporal que se borra al terminar; nunca quedan en el repo).
# - Crea la credencial «Goltime · OpenAI» vacía SOLO si no existe: la API key la pegas tú en n8n.
# - Importa, publica los workflows y reinicia n8n para que registre los webhooks.
# Se puede correr las veces que haga falta (por ejemplo después de cambiar el generador).
set -euo pipefail
cd "$(dirname "$0")/../.."
set -a; . ./.env; set +a

COMPOSE=(docker compose -f docker-compose.yml -f docker-compose.whatsapp.yml)
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
]
json.dump(credenciales, open(os.path.join(sys.argv[1], "credenciales.json"), "w"))
json.dump([{"id": "GoltimeOpenAi001", "name": "Goltime · OpenAI", "type": "openAiApi",
            "data": {"apiKey": "pega-aqui-la-api-key-de-openai"}}], open(os.path.join(sys.argv[1], "openai.json"), "w"))
EOF

"${N8N[@]}" mkdir -p "$DESTINO"
"${COMPOSE[@]}" cp "$TEMPORAL/credenciales.json" "n8n:$DESTINO/credenciales.json" >/dev/null 2>&1
"${COMPOSE[@]}" cp integraciones/n8n/workflows "n8n:$DESTINO/" >/dev/null 2>&1

"${N8N[@]}" n8n import:credentials --input="$DESTINO/credenciales.json" 2>&1 | grep -iE "import|error" || true
if "${N8N[@]}" n8n export:credentials --id=GoltimeOpenAi001 --output="$DESTINO/existe.json" >/dev/null 2>&1; then
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
echo "Listo. n8n: ${N8N_URL_PUBLICA:-http://localhost:5678/}"
