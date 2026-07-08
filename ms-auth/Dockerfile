# ─── Stage 1: Build ───────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B

# ─── Stage 2: Runtime ─────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S gs && adduser -S gs -G gs

COPY --from=build /app/target/ms-auth-0.0.1-SNAPSHOT.jar app.jar
COPY docker-entrypoint.sh docker-entrypoint.sh

# El keystore RSA (clave de firma de los JWT) se genera en el primer arranque
# del container, dentro de /app/keys (montado como volumen persistente en
# docker-compose) — no en el build. Así sobrevive a los rebuilds y los JWT ya
# emitidos siguen siendo validos. /app/keys se crea y se le da dueño ANTES de
# bajar privilegios a 'gs', para que el volumen herede esos permisos al
# inicializarse vacio por primera vez.
RUN mkdir -p keys && \
    chmod +x docker-entrypoint.sh && \
    chown -R gs:gs /app

USER gs

ENV GS_KEYSTORE_PATH=file:/app/keys/gs-auth.p12

EXPOSE 8081

HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["./docker-entrypoint.sh"]
