# 🐳 ERP G&S — Levantamiento con Docker

Esta guía cubre cómo levantar el stack completo (8 microservicios + frontend
+ infra) en producción local con un solo comando.

---

## 📋 Pre-requisitos

- **Docker Desktop** ≥ 4.30 (con Compose v2 incluido)
- **8 GB de RAM libres** (el stack consume ~3-4 GB con todos arriba)
- **Puertos libres** en el host:
  - `80` (frontend)
  - `8080` (gateway), `8081` (auth), `8761` (eureka)
  - `3306` (mysql), `5672` y `15672` (rabbit), `9000`/`9001` (minio)

> Los puertos están bindeados a `127.0.0.1` para que no queden expuestos a la
> red. Si querés acceder desde otro equipo en tu LAN, sacá el `127.0.0.1:` en
> `docker-compose.yml`.

---

## 🚀 Primer arranque

```bash
# 1. Copiar plantilla de credenciales
cp .env.example .env

# 2. (Opcional) Editar .env con contraseñas reales
#    Si no lo hacés, se usan los defaults del docker-compose.

# 3. Levantar todo (compila los 8 jars + Angular + arma containers)
docker compose up -d

# 4. Mirar el progreso de boot
docker compose logs -f
```

**Tiempo del primer arranque**: ~5-8 minutos (depende de la PC).
El compose hace los siguientes pasos automáticamente:

1. Levanta **MySQL**, **RabbitMQ**, **MinIO** y espera healthy
2. Compila y levanta **discovery-server** (Eureka), espera que esté healthy
3. Compila y levanta **ms-auth**, espera que se registre en Eureka y esté healthy
4. Compila y levanta los 5 MS de negocio (en paralelo)
5. Compila y levanta **api-gateway**
6. Compila Angular en producción y arranca **nginx**

Una vez que termine, abrís el navegador:

| URL | Servicio |
|---|---|
| http://localhost | **Frontend** (la app) |
| http://localhost:8761 | Eureka dashboard (ver todos los MS registrados) |
| http://localhost:8080/actuator/health | API Gateway health |
| http://localhost:15672 | RabbitMQ management (admin/admin) |
| http://localhost:9001 | MinIO console |

---

## 🛠 Comandos útiles

```bash
# Ver estado de todos los containers
docker compose ps

# Logs en vivo de un servicio específico
docker compose logs -f ms-pedidos

# Reiniciar un solo servicio (sin tocar los demás)
docker compose restart ms-catalogo

# Reconstruir un servicio (después de cambiar código)
docker compose up -d --build ms-catalogo

# Reconstruir TODO desde cero (descarta cache)
docker compose build --no-cache

# Apagar todo (mantiene BD y archivos MinIO)
docker compose down

# Apagar y BORRAR datos persistidos (volúmenes)
docker compose down -v

# Ejecutar un comando dentro de un container
docker compose exec mysql mysql -uroot -p
docker compose exec ms-pedidos sh
```

---

## 🐛 Troubleshooting

### "Cannot find module 'X'" al compilar el frontend
El build del frontend necesita instalar deps con `--legacy-peer-deps` por el
mismatch ngx-toastr 19 vs Angular 20. El Dockerfile ya lo hace.
Si seguís viendo errores: `docker compose build --no-cache frontend`.

### El MS arranca pero no aparece en Eureka
Mirá los logs del MS:
```bash
docker compose logs ms-catalogo | grep -i eureka
```
Suele ser que arrancó antes de que discovery esté listo. El compose tiene
`depends_on: { condition: service_healthy }` para evitarlo, pero si el
healthcheck de discovery falla, los MS arrancan igual y no se registran.
Solución: `docker compose restart ms-catalogo`.

### MySQL no inicializa
Si cambiás la password después del primer arranque, Docker NO actualiza la
del volumen. Hay que borrarlo:
```bash
docker compose down
docker volume rm gys_mysql_data
docker compose up -d
```

### Puerto 80 ocupado en Windows
Suele ser IIS o el servicio "World Wide Web Publishing Service".
- Desactivar IIS desde Panel de Control → Programas → Características de Windows
- O cambiar el mapeo en docker-compose: `"8000:80"` y abrir `http://localhost:8000`

### Java 21 vs 17
- `discovery-server` y `api-gateway` usan Spring Boot 4.x → Java 21 (en los Dockerfiles)
- Los demás (ms-*) usan Spring Boot 3.3 → Java 17
- No mezclar: cada Dockerfile ya tiene la versión correcta

---

## 🧪 Modo DESARROLLO (sin Docker)

Si querés iterar rápido sobre código Java o TypeScript:

```bash
# Solo infra (mysql, rabbit, minio) en Docker
docker compose up -d mysql rabbitmq minio

# Cada MS en su propia terminal con perfil dev (H2 in-memory, sin MySQL)
cd discovery-server && mvn spring-boot:run -Dspring-boot.run.profiles=dev
cd ms-auth          && mvn spring-boot:run -Dspring-boot.run.profiles=dev
cd ms-catalogo      && mvn spring-boot:run -Dspring-boot.run.profiles=dev
# ... (idem para pedidos, produccion, finanzas, stock)
cd api-gateway      && mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Frontend con hot reload
cd frontend-app && ng serve --open
```

En este modo:
- Cada MS usa H2 en memoria (datos se pierden al apagar)
- El frontend corre en `:4200` y apunta al gateway en `:8080`
- Reiniciás un MS sin afectar al resto

---

## 📊 Recursos consumidos

| Container | RAM idle | RAM activa |
|---|---|---|
| MySQL | ~400 MB | ~600 MB |
| RabbitMQ | ~150 MB | ~250 MB |
| MinIO | ~100 MB | ~150 MB |
| Cada Spring Boot MS | ~300 MB | ~500 MB |
| nginx | ~10 MB | ~30 MB |
| **Total estimado** | **~2.8 GB** | **~4.5 GB** |

Si tu PC tiene 8 GB en total, va apretada pero anda. Con 16 GB, sobra.

---

## 🚢 Deploy a un servidor real

Lo que falta para pasar de "Docker local" a "Docker en VPS":

1. **HTTPS**: agregar Caddy o Traefik con Let's Encrypt como front
2. **Issuer real de OAuth2**: en `ms-auth/application-prod.properties`,
   cambiar `AUTH_ISSUER` al dominio público (ej: `https://api.gys.com`)
3. **Keystore real**: generar uno nuevo con `keytool` y montarlo como secret,
   no usar el de desarrollo del repo
4. **Backups de MySQL**: `docker compose exec mysql mysqldump ...` programado
5. **CORS strict**: en `api-gateway/application-prod.properties`,
   `ALLOWED_ORIGINS` debe ser solo el dominio del frontend (no `*`)
6. **Logs centralizados**: agregar driver de logs a un servicio externo

Pero para tesis/demo local, lo que está ya alcanza.
