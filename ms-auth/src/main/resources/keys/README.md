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

En producción se usa un keystore distinto, montado fuera del JAR y referenciado
por las variables de entorno:

- `GS_KEYSTORE_PASSWORD` — contraseña real del keystore productivo
- `gs.auth.keystore.path` — ruta absoluta al `.p12` montado
