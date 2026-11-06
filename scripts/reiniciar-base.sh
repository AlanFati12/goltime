#!/usr/bin/env bash
# Deja la base de datos vacía para hacer pruebas: borra TODOS los datos (reservas, sedes, torneos, usuarios, fotos…)
# y deja solo el administrador de .env (ADMIN_CORREO) y un cliente de prueba (CLIENTE_PRUEBA_CORREO, que se agrega
# a .env si no existe). Nadie tiene contraseña: se entra con Google (en local, con GOOGLE_MODO_PRUEBA=true).
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
# Se inserta directo: ya no hay registro con formulario. Perfil completo, habilitado para inscribir equipos, y se
# vincula con su cuenta de Google la primera vez que entra con ese correo.
docker compose exec -T db psql -q -U "$DB_USUARIO" -d "$DB_NOMBRE" \
  -c "INSERT INTO usuarios (nombres, apellidos, correo, telefono, rol, correo_verificado, acepto_terminos_en, puede_inscribir_equipos)
      VALUES ('Cliente', 'De Prueba', '$CLIENTE_PRUEBA_CORREO', '+573001234567', 'CLIENTE', TRUE, now(), TRUE)
      ON CONFLICT DO NOTHING;"

echo ""
echo "Listo. Base vacía con dos usuarios:"
echo "  Administrador: $ADMIN_CORREO"
echo "  Cliente:       $CLIENTE_PRUEBA_CORREO"
echo "Se entra con Google; en local (GOOGLE_MODO_PRUEBA=true) basta escribir el correo."
echo "Abre http://localhost:${WEB_PUERTO:-8081}"
