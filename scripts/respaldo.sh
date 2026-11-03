#!/usr/bin/env bash
# Respaldo de producción: base de datos (pg_dump comprimido) y fotos subidas.
# Uso (en el VPS, desde cualquier carpeta):  ~/goltime/scripts/respaldo.sh
# Variables opcionales: DESTINO_RESPALDOS (por defecto ~/respaldos) y DIAS_RETENCION (por defecto 14).
# Programarlo a diario:  crontab -e  →  30 3 * * * $HOME/goltime/scripts/respaldo.sh >> $HOME/respaldos/respaldo.log 2>&1
set -euo pipefail

RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
DESTINO="${DESTINO_RESPALDOS:-$HOME/respaldos}"
DIAS="${DIAS_RETENCION:-14}"
FECHA="$(date +%Y%m%d-%H%M)"
COMPOSE=(docker compose -f "$RAIZ/docker-compose.produccion.yml")

mkdir -p "$DESTINO"

# Las credenciales se leen dentro del contenedor, así no hay que cargar el .env en el shell.
"${COMPOSE[@]}" exec -T db sh -c 'pg_dump -U "$POSTGRES_USER" "$POSTGRES_DB"' | gzip > "$DESTINO/db-$FECHA.sql.gz"

docker run --rm -v goltime_datos_archivos:/datos:ro -v "$DESTINO":/destino alpine \
  tar czf "/destino/fotos-$FECHA.tar.gz" -C /datos .

find "$DESTINO" \( -name 'db-*.sql.gz' -o -name 'fotos-*.tar.gz' \) -mtime +"$DIAS" -delete
echo "$(date -Is) respaldo listo: db-$FECHA.sql.gz y fotos-$FECHA.tar.gz"
