# ─── Stage 1: Build ───────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src

# Keystore RSA del Authorization Server. NO se versiona (clave criptografica),
# asi que lo generamos al buildear si no esta presente. Alias/pass deben coincidir
# con gs.auth.keystore.* (default gs_keystore_2025). Override con --build-arg.
ARG KEYSTORE_PASSWORD=gs_keystore_2025
RUN mkdir -p src/main/resources/keys && \
    [ -f src/main/resources/keys/gs-auth.p12 ] || \
    keytool -genkeypair -alias gs-auth -keyalg RSA -keysize 2048 -validity 3650 \
      -storetype PKCS12 -keystore src/main/resources/keys/gs-auth.p12 \
      -storepass "$KEYSTORE_PASSWORD" -keypass "$KEYSTORE_PASSWORD" \
      -dname "CN=gs-auth,OU=Laboratorio GS,O=Tesis,L=BA,C=AR"

RUN mvn package -DskipTests -B

# ─── Stage 2: Runtime ─────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S gs && adduser -S gs -G gs
USER gs

COPY --from=build /app/target/ms-auth-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081

HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
