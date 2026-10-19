#!/usr/bin/env bash
# Deja la base de datos vacía para hacer pruebas: borra TODOS los datos (reservas, sedes, torneos, usuarios, fotos…)
# y deja solo el administrador de .env (ADMIN_CORREO / ADMIN_CLAVE) y un cliente de prueba
# (CLIENTE_PRUEBA_CORREO / CLIENTE_PRUEBA_CLAVE, que se agregan a .env si no existen).
#
# Uso:  ./scripts/reiniciar-base.sh        (pide confirmación)
#       ./scripts/reiniciar-base.sh --si   (sin preguntar)
set -euo pipefail
cd "$(dirname "$0")/.."

[ -f .env ] || { echo "Falta el archivo .env (copia .env.example)."; exit 1; }

# Cliente de prueba: si no está en .env, se crea con una clave aleatoria y se guarda ahí.
if ! grep -q '^CLIENTE_PRUEBA_CORREO=' .env; then
  {
    echo ""
    echo "# --- Cliente de prueba (lo crea scripts/reiniciar-base.sh) ---"
    echo "CLIENTE_PRUEBA_CORREO=cliente@goltime.local"
    echo "CLIENTE_PRUEBA_CLAVE=Cliente$(openssl rand -hex 3)9"
  } >> .env
fi

set -a
# shellcheck disable=SC1091
. ./.env
set +a
DB_USUARIO=${DB_USUARIO:-goltime}
DB_NOMBRE=${DB_NOMBRE:-goltime}
API=http://localhost:8080/api/v1

if [ "${1:-}" != "--si" ]; then
  read -r -p "Se borrarán TODOS los datos de Goltime. Escribe SI para continuar: " respuesta
  [ "$respuesta" = "SI" ] || { echo "Cancelado."; exit 0; }
fi

echo "1/5 Deteniendo la API…"
docker compose up -d db >/dev/null 2>&1
docker compose stop api >/dev/null 2>&1

echo "2/5 Vaciando la base de datos…"
docker compose exec -T db psql -q -U "$DB_USUARIO" -d "$DB_NOMBRE" -v ON_ERROR_STOP=1 \
  -c "SET client_min_messages TO WARNING; DROP SCHEMA public CASCADE; CREATE SCHEMA public AUTHORIZATION \"$DB_USUARIO\";"

echo "3/5 Borrando fotos subidas…"
docker compose run --rm --no-deps --entrypoint sh api -c 'rm -rf /app/uploads/* 2>/dev/null; true' >/dev/null 2>&1 || true

echo "4/5 Arrancando la API (recrea las tablas y el administrador)…"
docker compose up -d api web >/dev/null 2>&1
for _ in $(seq 1 60); do
  estado=$(docker inspect -f '{{.State.Health.Status}}' "$(docker compose ps -q api)" 2>/dev/null || echo "?")
  [ "$estado" = "healthy" ] && break
  sleep 3
done
[ "$estado" = "healthy" ] || { echo "La API no arrancó. Revisa: docker compose logs api"; exit 1; }

echo "5/5 Creando el cliente de prueba…"
codigo=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$API/autenticacion/registrar" -H 'Content-Type: application/json' -d "{
  \"nombres\": \"Cliente\", \"apellidos\": \"De Prueba\", \"correo\": \"$CLIENTE_PRUEBA_CORREO\", \"telefono\": \"3001234567\",
  \"clave\": \"$CLIENTE_PRUEBA_CLAVE\", \"confirmacionClave\": \"$CLIENTE_PRUEBA_CLAVE\", \"aceptaTerminos\": true }")
[ "$codigo" = "201" ] || [ "$codigo" = "200" ] || { echo "No se pudo registrar el cliente (HTTP $codigo)."; exit 1; }
# Correo verificado y habilitado para inscribir equipos, para probar todo sin pasos extra.
docker compose exec -T db psql -q -U "$DB_USUARIO" -d "$DB_NOMBRE" \
  -c "UPDATE usuarios SET correo_verificado = TRUE, puede_inscribir_equipos = TRUE WHERE correo = '$CLIENTE_PRUEBA_CORREO';"

echo ""
echo "Listo. Base vacía con dos usuarios:"
echo "  Administrador: $ADMIN_CORREO   (clave: ADMIN_CLAVE en .env)"
echo "  Cliente:       $CLIENTE_PRUEBA_CORREO   (clave: CLIENTE_PRUEBA_CLAVE en .env)"
echo "Abre http://localhost:${WEB_PUERTO:-8081}"
