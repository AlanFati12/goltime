#!/usr/bin/env bash
# Conecta el número del bot a Evolution API (ADR 0018): crea la instancia si no existe, con su webhook
# hacia n8n, y deja un QR fresco en qr-whatsapp.png (vence en ~40 s) más un código para vincular por número.
#
#   scripts/whatsapp/conectar.sh 573239085762
#
# En el celular: WhatsApp > Dispositivos vinculados > Vincular un dispositivo (QR)
# o «Vincular con el número de teléfono» (código). Lee la configuración de .env.
set -euo pipefail
cd "$(dirname "$0")/../.."
set -a; . ./.env; set +a

NUMERO="${1:?Uso: $0 <número con indicativo, sin +; ej. 573239085762>}"
URL="http://localhost:${EVOLUTION_PUERTO:-8082}"
INSTANCIA="${EVOLUTION_INSTANCIA:-goltime}"
CLAVE="${EVOLUTION_CLAVE_API:?Falta EVOLUTION_CLAVE_API en .env}"
SALIDA="${2:-qr-whatsapp.png}"

estado=$(curl -s "$URL/instance/connectionState/$INSTANCIA" -H "apikey: $CLAVE" || true)
if echo "$estado" | grep -q '"state":"open"'; then
  echo "La instancia $INSTANCIA ya está conectada."; exit 0
fi
if ! echo "$estado" | grep -q '"instance"'; then
  curl -sf -X POST "$URL/instance/create" -H "apikey: $CLAVE" -H 'Content-Type: application/json' -d "{
    \"instanceName\": \"$INSTANCIA\", \"integration\": \"WHATSAPP-BAILEYS\", \"qrcode\": true, \"number\": \"$NUMERO\",
    \"rejectCall\": true, \"msgCall\": \"Hola, soy el bot de Goltime: solo leo mensajes de texto. Escríbeme por aquí.\",
    \"groupsIgnore\": true, \"syncFullHistory\": false,
    \"webhook\": {\"url\": \"http://n8n:5678/webhook/goltime-whatsapp\", \"byEvents\": false, \"base64\": false, \"events\": [\"MESSAGES_UPSERT\"]}
  }" > /dev/null
fi
curl -sf "$URL/instance/connect/$INSTANCIA?number=$NUMERO" -H "apikey: $CLAVE" | python3 -I -c "
import base64, json, sys
r = json.load(sys.stdin)
codigo = r.get('pairingCode')
if r.get('base64'):
    open(sys.argv[1], 'wb').write(base64.b64decode(r['base64'].split(',', 1)[1]))
    print('QR guardado en', sys.argv[1])
print('Código para vincular por número:', codigo or '(no disponible)')
" "$SALIDA"
