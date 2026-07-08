## Claves de firma JWT — ms-auth

Este directorio aloja el keystore PKCS12 (`gs-auth.p12`) que `ms-auth` usa
para firmar los tokens JWT con RSA.

**El keystore NO se commitea al repo** (está en `.gitignore`). Cada desarrollador
genera el suyo localmente.

### Generar el keystore para desarrollo

Desde la raíz del proyecto:

```bash
keytool -genkeypair \
  -alias gs-auth \
  -keyalg RSA \
  -keysize 2048 \
  -validity 3650 \
  -storetype PKCS12 \
  -keystore ms-auth/src/main/resources/keys/gs-auth.p12 \
  -storepass gs_keystore_2025 \
  -dname "CN=gs-auth,OU=Laboratorio GS,O=Tesis,L=BA,C=AR"
```

La contraseña por defecto (`gs_keystore_2025`) está en
`application.properties` como fallback de desarrollo.

### Producción

En producción (Docker) se usa un keystore distinto al de desarrollo, generado
automáticamente y persistido en un volumen — **no** el que va dentro del JAR.

`docker-entrypoint.sh` genera el `.p12` en `/app/keys/gs-auth.p12` la primera
vez que arranca el container (si el volumen `ms_auth_keys` está vacío) y lo
reutiliza en cada rebuild/restart siguiente. Esto es importante: si el
keystore cambiara en cada deploy, los JWT ya emitidos quedarían inválidos y
todos los usuarios logueados tendrían que volver a iniciar sesión.

Variables involucradas:

- `GS_KEYSTORE_PASSWORD` — contraseña real del keystore productivo
- `GS_KEYSTORE_PATH` — ruta al `.p12` (default en el Dockerfile: `file:/app/keys/gs-auth.p12`)
