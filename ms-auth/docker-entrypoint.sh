#!/bin/sh
set -e

# Keystore RSA persistente (montado como volumen en /app/keys). Se genera una
# única vez, la primera vez que el volumen está vacío; en los arranques
# siguientes (rebuild o restart del container) ya existe y se reutiliza, así
# los JWT emitidos antes siguen siendo válidos.
KEYSTORE_FILE="/app/keys/gs-auth.p12"

if [ ! -f "$KEYSTORE_FILE" ]; then
  echo "[ms-auth] Keystore RSA no encontrado, generando uno nuevo en $KEYSTORE_FILE..."
  keytool -genkeypair \
    -alias gs-auth \
    -keyalg RSA \
    -keysize 2048 \
    -validity 3650 \
    -storetype PKCS12 \
    -keystore "$KEYSTORE_FILE" \
    -storepass "${GS_KEYSTORE_PASSWORD:-gs_keystore_2025}" \
    -keypass "${GS_KEYSTORE_PASSWORD:-gs_keystore_2025}" \
    -dname "CN=gs-auth,OU=Laboratorio GS,O=Tesis,L=BA,C=AR"
  echo "[ms-auth] Keystore generado."
else
  echo "[ms-auth] Reutilizando keystore RSA persistido en $KEYSTORE_FILE."
fi

exec java -jar app.jar
