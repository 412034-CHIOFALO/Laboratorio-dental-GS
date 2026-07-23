# Laboratorio Dental G&S — Sistema de Gestión Integral

> SaaS de gestión integral para un laboratorio dental. Trabajo Integrador de la
> carrera de Programación — UTN. Reemplaza el flujo manual con Excel y WhatsApp
> por una plataforma centralizada con arquitectura de microservicios.

**Stack:** Spring Boot 3.3 + Spring Cloud · Angular 20 PWA · MySQL / H2 ·
MinIO (comprobantes y escaneos) · Bot de WhatsApp (Node + Gemini) ·
Docker Compose (stack completo en un comando) · CI con GitHub Actions.

---

## 📋 Tabla de contenidos

1. [Sobre el proyecto](#-sobre-el-proyecto)
2. [Arquitectura](#-arquitectura)
3. [Levantar el proyecto](#-levantar-el-proyecto)
4. [Estructura del repositorio](#-estructura-del-repositorio)
5. [Estado del desarrollo](#-estado-del-desarrollo)
6. [Convenciones](#-convenciones)
7. [Troubleshooting](#-troubleshooting)
8. [Roadmap](#-roadmap)

---

## 🦷 Sobre el proyecto

El laboratorio **G&S** fabrica trabajos protésicos (coronas, prótesis, férulas,
aparatos de ortodoncia) por encargo de odontólogos. Hoy gestiona todo con
planillas de Excel y mensajes de WhatsApp, lo que produce falta de
trazabilidad, cuellos de botella administrativos y descontrol financiero.

Este sistema implementa:

- **Workflow Kanban** de producción visible en tiempo real
- **Catálogo digital** de tipos de trabajo con precios y categorías
- **Gestión de pedidos** con autocomplete inteligente de odontólogos (por
  nombre, DNI, CUIT o matrícula)
- **Control financiero** con 3 cajas (Física, Bancaria, Compensación), cascada
  automática de sueldos, pagos a proveedores y triangulados
- **Stock de materiales** con descuento automático desde la receta del catálogo
  y alertas de stock bajo por WhatsApp
- **Bot de WhatsApp** (Node + IA Gemini) que lee comprobantes de pago del grupo
  y registra sueldos/proveedores/efectivo automáticamente
- **Scraper de emails** (opcional) que convierte pedidos recibidos por mail en
  pedidos del sistema, interpretando el contenido con IA

---

## 🏗 Arquitectura

Microservicios con **Service Discovery** vía Eureka y enrutamiento centralizado
vía **API Gateway**. Cada microservicio tiene su propia base de datos.

```
                    ┌─────────────────────┐
                    │   Frontend Angular  │
                    │     (puerto 4200)   │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │     API Gateway     │
                    │     (puerto 8080)   │
                    └──────────┬──────────┘
                               │
        ┌──────────────────────┼──────────────────────┐
        ▼                      ▼                      ▼
┌──────────────┐     ┌──────────────┐         ┌──────────────┐
│   ms-auth    │     │ ms-catalogo  │         │  ms-pedidos  │
│   (8081)     │     │   (8083)     │         │   (8082)     │
│              │     │              │         │              │
│ OAuth2 + JWT │     │ Tipos de     │         │ Pedidos +    │
│ Usuarios     │     │ trabajo      │         │ Odontólogos  │
└──────────────┘     └──────────────┘         └──────────────┘

                     ┌──────────────┐         ┌──────────────┐
                     │ ms-finanzas  │         │   ms-stock   │
                     │   (8085)     │         │   (8086)     │
                     │              │         │              │
                     │ 3 Cajas      │         │ Materiales   │
                     │ Cobros       │         │ Movimientos  │
                     └──────────────┘         └──────────────┘

                    ┌─────────────────────┐
                    │ discovery-server    │
                    │  Eureka (8761)      │
                    └─────────────────────┘
```

**Componentes de apoyo:** **nginx** sirve el frontend y hace de reverse-proxy
al gateway · **MinIO** guarda comprobantes y escaneos 3D · el **bot de WhatsApp**
(Node) habla con el gateway por API key y con WhatsApp Web · **Eureka** registra
los servicios y el gateway resuelve por nombre lógico (`lb://ms-*`).

### Patrón en capas

Cada microservicio sigue el mismo patrón:

```
Controller → IService → ServiceImpl → Repository → BD
              │
              ├─ Validación con @Valid (Bean Validation)
              └─ GlobalExceptionHandler (400/403/404/405/409/422/500)
```

### Seguridad

- `ms-auth` actúa como **OAuth2 Authorization Server** y emite JWT firmados con
  RSA (clave en `ms-auth/src/main/resources/keys/gs-auth.p12`)
- Cada microservicio valida el JWT contra el JWK Set de `ms-auth`
- Roles: `ADMIN`, `ADMINISTRATIVO`, `TECNICO`, `ODONTOLOGO`
- El frontend obtiene el JWT al login y lo envía en cada request via interceptor

---

## 🚀 Levantar el proyecto

Hay **3 modos** según para qué lo necesites:

### Modo 1 — Solo frontend con mocks (lo más rápido)

Ideal para demos o desarrollo del frontend sin backend.
`environment.useMocks = true` está activo por defecto.

```bash
cd frontend-app
npm install
npm start            # o: ng serve --open
```

→ `http://localhost:4200` · Login: **admin** / **admin123**

> En modo demo cualquier user/pass funciona. Los services usan datos
> hardcodeados (mocks) en memoria.

### Modo 2 — Full stack en dev (H2 en memoria, sin Docker)

Cada microservicio usa H2 en perfil `dev`. **No requiere MySQL ni Docker.**

**Pre-requisitos:** Java 17+, Maven 3.8+, Node 20+. El keystore JWT lo **genera
el script automáticamente** si no existe (no hay paso manual).

```powershell
# Windows
.\start-dev.ps1
```

El script abre 8 ventanas de PowerShell en el orden correcto:

| # | Servicio | Puerto | Esperar a ver |
|---|----------|--------|---------------|
| 1 | discovery-server | 8761 | `Started DiscoveryServerApplication` |
| 2 | ms-auth | 8081 | `Started MsAuthApplication` |
| 3 | ms-catalogo | 8083 | `Started MsCatalogoApplication` |
| 4 | ms-pedidos | 8082 | `Started MsPedidosApplication` |
| 5 | ms-finanzas | 8085 | `Started MsFinanzasApplication` |
| 6 | ms-stock | 8086 | `Started MsStockApplication` |
| 7 | api-gateway | 8080 | `Started ApiGatewayApplication` |
| 8 | frontend-app | 4200 | `Local: http://localhost:4200` |

**Antes de arrancar el frontend** cambiá `useMocks: false` en
`frontend-app/src/environments/environment.ts` para que apunte al gateway real.

**Verificación:**

- Panel Eureka: http://localhost:8761 — debería listar los 6 servicios registrados (auth, catalogo, pedidos, finanzas, stock, gateway)
- Health Gateway: http://localhost:8080/actuator/health
- Frontend: http://localhost:4200

**Si no querés usar el script** podés arrancar cada microservicio a mano:

```bash
cd discovery-server && mvn spring-boot:run    # esperá a que arranque
cd ms-auth            && mvn spring-boot:run
cd ms-catalogo        && mvn spring-boot:run
cd ms-pedidos         && mvn spring-boot:run
cd ms-finanzas        && mvn spring-boot:run
cd ms-stock           && mvn spring-boot:run
cd api-gateway        && mvn spring-boot:run
cd frontend-app       && ng serve --open
```

> En Linux/Mac usar `./mvnw spring-boot:run`. En Windows con Git Bash, el wrapper
> `mvnw` puede no estar; usar `mvn` directo o el `mvnw.cmd`.

### Modo 3 — Stack completo con Docker (un solo comando)

El modo más parecido a producción: levanta **14 containers** (MySQL, MinIO,
Adminer, backup automático, discovery, ms-auth, los 4 ms de negocio, gateway,
frontend nginx, el bot de WhatsApp y el scraper de mails) con MySQL real.
Funciona sobre un **clone limpio sin pasos manuales** — el keystore JWT se
genera en el build y los `application-prod.properties` están versionados.

```bash
docker compose up -d --build      # primer arranque: compila e inicia todo
docker compose logs -f gs-bot     # mostrar el QR para vincular el WhatsApp del bot
docker compose ps                 # ver el estado (healthy) de cada container
```

**Para rebuildear solo lo que cambió** (mucho más rápido que todo de nuevo):

```bash
docker compose build <servicio>       # ej: gs-bot, gs-frontend, ms-finanzas
docker compose up -d <servicio>
```

`gs-bot` (sesión de WhatsApp) y `gs-mail-scraper` (pedidos por email) son
servicios **separados a propósito** — un cuelgue de la conexión IMAP no puede
afectar la sesión de WhatsApp (mucho más lenta de recuperar), y viceversa. Si
`gs-mail-scraper` falla varios ciclos seguidos se reinicia solo (ver
`MAIL_MAX_FALLOS_CONSECUTIVOS` en `.env.example`).

Accesos desde el host:

| URL | Servicio |
|-----|----------|
| http://localhost | Frontend (nginx) |
| http://localhost:8080 | API Gateway (directo) |
| http://localhost:8761 | Eureka dashboard |
| http://localhost:9001 | MinIO console |

> Variables de entorno opcionales en `.env` (ver `.env.example`): contraseñas de
> MySQL/MinIO, `GEMINI_API_KEY` para la IA del bot, credenciales del scraper de
> mails. Todas tienen defaults de desarrollo, así que `docker compose up` anda sin
> configurar nada.

### Apagar todo

- **Modo 2 (local):** cerrar las ventanas de PowerShell (o `Ctrl+C` en cada una)
- **Modo 3 (Docker):** `docker compose down` (o `docker compose down -v` para
  borrar también las bases y los archivos)

---

## 📁 Estructura del repositorio

```
TRABAJO PRACTICO INTEGRADOR/
│
├── discovery-server/         Eureka Server (Service Discovery)
├── api-gateway/              Spring Cloud Gateway con validación JWT
│
├── ms-auth/                  Authorization Server (OAuth2 + JWT RSA)
├── ms-catalogo/              Tipos de trabajo del laboratorio
├── ms-pedidos/               Pedidos + Odontólogos clientes + Tablero Kanban
├── ms-finanzas/              Comprobantes, cajas, proveedores, sueldos
├── ms-stock/                 Materiales y movimientos de stock
│
├── frontend-app/             Angular 20 PWA
│   └── src/app/pages/dashboard/     Páginas del SaaS
├── gs-bot-whatsapp/          Sesión de WhatsApp (Node + Gemini) y scraper de mails
│   ├── index.js                    Bot: lee comprobantes del grupo, responde por WhatsApp
│   ├── mail-scraper.js             Scraper de mails: corre como servicio Docker aparte
│   ├── Dockerfile                  Imagen del bot (con Chromium/Puppeteer)
│   ├── Dockerfile.mail-scraper     Imagen del scraper (sin Chromium, mucho más liviana)
│   └── wa-web-pinned/              Versión de WhatsApp Web fijada (evita romperse con cada update de WA)
│
├── docker-compose.yml        Stack completo (14 containers: DB, MinIO, ms, frontend, bot, scraper)
├── start-dev.ps1             Arranque local sin Docker (H2, keystore auto)
├── test-e2e.ps1              Suite de pruebas E2E — OJO: crea datos reales (ver Troubleshooting)
├── tunnel-demo.sh            Expone el stack a internet para demos (Cloudflare Quick Tunnel + aviso por ntfy)
├── .env.example              Plantilla de variables de entorno
└── README.md                 Este archivo
```

### Convención de paquetes Java

```
com.gs.ms_<nombre>/
├── config/         Configuración (SecurityConfig, DataInitializer)
├── controller/     REST endpoints
├── service/        Interfaces (IService) e implementaciones
├── repository/     JpaRepository
├── model/          @Entity JPA y enums
├── dto/            Request y Response DTOs
└── exception/      Excepciones de dominio y GlobalExceptionHandler
```

---

## ✅ Estado del desarrollo

### Backend

- [x] discovery-server, api-gateway (CORS centralizado, validación JWT), ms-auth (OAuth2 + JWT RSA, rate-limit de login, auditoría)
- [x] ms-catalogo (tipos de trabajo + receta de materiales)
- [x] ms-pedidos (pedidos, odontólogos con find-or-create + búsqueda inteligente, Kanban, entregas, pedidos atrasados, descuento de stock automático)
- [x] ms-finanzas (3 cajas, cobros, comprobantes, proveedores, deudas, sueldos con devengado, pagos triangulados)
- [x] ms-stock (CRUD de materiales, movimientos, configuración de alertas)
- [x] Patrón en capas + `GlobalExceptionHandler` unificado (400/403/404/405/409/422/500) en todos los ms
- [x] Perfil `dev` con H2 (sin Docker) y perfil `prod` con MySQL
- [x] **Tests con JaCoCo — 80%+ de coverage en los 5 microservicios** (catálogo 100%, stock 86%, pedidos 81%, finanzas 82%, auth 83%)

### Frontend

- [x] Landing pública, Login con AuthGuard, PWA (service worker offline)
- [x] Catálogo, Pedidos (autocomplete inteligente), Odontólogos (vista 360°)
- [x] Producción / Kanban (drag & drop desktop, "avanzar" en mobile), Entregas
- [x] Finanzas (cajas en vivo, cobros, sueldos, proveedores, registros del bot)
- [x] Stock, Reportes, Configuración, Usuarios, Auditoría
- [x] Manual de usuario integrado + tour guiado (driver.js)

### Bot de WhatsApp (Node)

- [x] Lectura de comprobantes del grupo con IA (Gemini) + OCR
- [x] Registro automático de pagos: sueldos, proveedores, triangulados y efectivo
- [x] Alertas de stock bajo al administrador
- [x] Scraper de emails → pedidos (opcional, `MAIL_ENABLED=true`)

### Infraestructura

- [x] Dockerfiles de los 10 servicios propios + `docker-compose.yml` (stack completo, 14 containers)
- [x] Fresh-clone + `docker compose up` funciona out-of-the-box (keystore auto-generado, prod-properties versionados)
- [x] CI con GitHub Actions (build + tests de los 5 ms + frontend + bot)
- [x] Javadoc generable (`mvnw javadoc:javadoc`)

### Pendiente / deuda

- [ ] Migraciones de BD (Flyway) — hoy `ddl-auto=update`
- [ ] CD real (deploy a servidor + registry de imágenes)
- [ ] Tests E2E automatizados del stack vivo (`test-e2e.ps1` existe, falta integrarlo)

---

## 🎨 Sistema de diseño

Tema **dark navy + emerald green**. Variables CSS centralizadas en
`frontend-app/src/styles.css` (prefijo `--gs-*`).

```css
--gs-green:    #22c55e    /* brand principal */
--gs-cyan:     #06b6d4    /* acento tech/médico */
--gs-amber:    #f59e0b    /* warnings, urgencia */
--gs-rose:     #f43f5e    /* errores, peligro */
--gs-bg:       #080d18    /* fondo */
--gs-card:     #111827    /* superficies */
--gs-text:     #e2e8f0    /* texto principal */
```

Tipografía: **Inter** para UI, **JetBrains Mono** para nros de pedido y precios.

---

## 🔧 Convenciones

### Flujo de branches

```
main         Código estable (producción)
develop      Integración de features
feature/X    Una rama por feature (sale de develop → vuelve a develop)
```

```bash
git checkout develop
git pull
git checkout -b feature/vista-stock
# ... trabajar y commitear ...
git push -u origin feature/vista-stock
# Cuando esté listo: PR/merge a develop con --no-ff
```

### Convención de commits

```
feat(ms-pedidos): entidad Odontologo con find-or-create
fix(modal): scroll del modal-body con grid layout
chore: actualizar gitignore para claves criptograficas
refactor(kanban): usar PedidosService en vez de ms-produccion
```

Tipos: `feat`, `fix`, `chore`, `refactor`, `docs`, `test`, `style`.

### Reglas de seguridad

- Claves criptográficas (`*.p12`, `*.jks`, `*.key`, `*.pem`) **nunca** se commitean
  (el keystore JWT se genera en el build / `start-dev.ps1`)
- El `.env` real **nunca** se commitea (solo `.env.example` con placeholders)
- `application-prod.properties` **sí se versiona**: solo tiene placeholders
  `${VAR:default}` sin secretos, para que un clone limpio arranque sin configurar nada

---

## 🐞 Troubleshooting

| Síntoma | Causa | Solución |
|---------|-------|----------|
| `Connection refused` al gateway desde el frontend | Gateway no arrancó | `http://localhost:8080/actuator/health` |
| `JWT signature does not match` | ms-auth se reinició con keystore distinto | Reiniciá **todos** los ms (no solo el frontend) |
| `Unable to connect to MySQL` | Docker no terminó de arrancar | `docker compose ps` debería decir "healthy" |
| `Address already in use: 8081` | Puerto zombie de una corrida anterior | `netstat -ano \| findstr :8081` + matar PID |
| Eureka muestra `UNKNOWN` para un ms | El ms arrancó antes que discovery | Reiniciá solo ese ms |
| Script `.ps1` no ejecuta | ExecutionPolicy bloqueado | `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned` |
| `keytool: command not found` | Java no está en PATH | Agregá `%JAVA_HOME%\bin` al PATH |

### `test-e2e.ps1` — qué hace y cuándo correrlo

Es una suite de smoke tests en PowerShell que pega contra el gateway **real**
(`http://localhost:8080` por defecto) y valida que los endpoints principales
respondan como se espera: login, rutas protegidas, endpoints del bot con/sin
API key, etc.

**Importante — no es de solo lectura.** Además de chequeos `GET`, el script
también hace `POST /api/odontologos` (crea un odontólogo real), `POST` contra
los endpoints del bot (`pago-automatico`, `pago-efectivo`, crea registros
reales) y `PUT /api/stock/configuracion` (modifica configuración real). Pensado
para correr contra el **Modo 2 (dev, H2 en memoria)**, donde no importa
ensuciar los datos — si se corre contra el stack de Docker con MySQL real
(que es persistente), esos datos de prueba quedan ahí de verdad.

```powershell
.\test-e2e.ps1                                    # contra localhost:8080
.\test-e2e.ps1 -GatewayUrl http://mi-server:8080  # contra otro host
```

### `tunnel-demo.sh` — exponer el stack para una demo

Expone el stack corriendo en un server (pensado para Linux/el server de
despliegue, no Windows) a internet mediante un **Cloudflare Quick Tunnel**
(`cloudflared`), sin necesidad de abrir puertos ni configurar DNS. Al conseguir
la URL pública, avisa por [ntfy.sh](https://ntfy.sh) a un topic elegido.

```bash
./tunnel-demo.sh mi-topic-secreto   # suscribite antes en https://ntfy.sh/mi-topic-secreto
```

`Ctrl+C` corta el túnel — la URL deja de funcionar al instante, no queda nada
expuesto después. Requiere `cloudflared` instalado en el server. Pensado para
demos puntuales (ej. la defensa de tesis), no para exposición permanente.

### Keystore de ms-auth

El keystore JWT (`ms-auth/src/main/resources/keys/gs-auth.p12`) **no se versiona**
(es una clave criptográfica) pero se **genera solo**:

- **Docker:** el `Dockerfile` de ms-auth lo crea con `keytool` durante el build.
- **Local:** `start-dev.ps1` lo crea al arrancar si no existe.

Si querés generarlo a mano (raro):

```bash
keytool -genkeypair -alias gs-auth -keyalg RSA -keysize 2048 -validity 3650 \
  -storetype PKCS12 -keystore ms-auth/src/main/resources/keys/gs-auth.p12 \
  -storepass gs_keystore_2025 -dname "CN=gs-auth,OU=Laboratorio GS,O=Tesis,L=BA,C=AR"
```

---

## 🗺 Roadmap

| Sprint | Foco | Estado |
|--------|------|--------|
| Sprint 0 | Kick-off, estructura, monorepo | ✅ Finalizado |
| Sprint 1 | Auth + Frontend base + Landing | ✅ Finalizado |
| Sprint 2 | Catálogo + Dashboard | ✅ Finalizado |
| Sprint 3 | Pedidos + Odontólogos + Entregas | ✅ Finalizado |
| Sprint 4 | Finanzas — Cajas + dev sin Docker | ✅ Finalizado |
| Sprint 5 | Kanban + Finanzas (cobros/sueldos/proveedores) + Stock | ✅ Finalizado |
| Sprint 6 | Bot WhatsApp (comprobantes, triangulados, efectivo, scraper de mails) | ✅ Finalizado |
| Sprint 7 | Reportes + Auditoría + Tests (80% coverage) + Dockerización | ✅ Finalizado |
| Sprint 8 | Despliegue productivo y dominio propio | 🔵 En curso |

---

## 📚 Documentación adicional

La documentación detallada del proyecto (arquitectura, flujos de negocio,
decisiones técnicas, análisis del cliente) está versionada aparte en el vault
de Obsidian del autor (no incluida en este repo público).

---

## 👤 Autor

**Nicolás Mauricio Chiofalo** — Legajo 412034
Trabajo Integrador — Programación, UTN

---

## 📄 Licencia

Proyecto académico. Todos los derechos reservados al autor.
