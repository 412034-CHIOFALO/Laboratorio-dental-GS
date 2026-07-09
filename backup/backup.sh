#!/bin/bash
set -euo pipefail

FECHA=$(date +%Y-%m-%d_%H-%M)
TMP_DIR="/backups/tmp/$FECHA"
BASES="gs_auth gs_catalogo gs_pedidos gs_finanzas gs_stock"
REMOTE="gdrive:gs-backups/$FECHA"

echo "[$(date)] === Iniciando backup $FECHA ==="
mkdir -p "$TMP_DIR"

for DB in $BASES; do
  echo "[$(date)] Dump de $DB..."
  mysqldump -h mysql -uroot -p"$MYSQL_ROOT_PASSWORD" \
    --single-transaction --routines --triggers "$DB" \
    | gzip > "$TMP_DIR/$DB.sql.gz"
done

echo "[$(date)] Empaquetando archivos de MinIO (escaneos/comprobantes)..."
tar czf "$TMP_DIR/minio-data.tar.gz" -C /minio_data .

echo "[$(date)] Subiendo a Google Drive ($REMOTE)..."
rclone copy "$TMP_DIR" "$REMOTE" --create-empty-src-dirs

echo "[$(date)] Backup subido OK. Limpiando temporales locales..."
rm -rf "$TMP_DIR"

echo "[$(date)] === Backup $FECHA completado ==="
