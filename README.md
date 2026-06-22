# Laboratorio Dental G&S — Sistema de Gestión Integral

> SaaS de gestión integral para un laboratorio dental. Trabajo Integrador de la
> carrera de Programación — UTN. Reemplaza el flujo manual con Excel y WhatsApp
> por una plataforma centralizada con arquitectura de microservicios.

**Stack:** Spring Boot 3.5 + Spring Cloud · Angular 20 PWA · MySQL / H2 ·
Docker Compose (RabbitMQ, MinIO para features futuras).

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
- **Control financiero** con 3 cajas (Física, Bancaria, Compensación) y
  cascada automática de sueldos
- **Bot de WhatsApp** (próximamente) para recibir pedidos de odontólogos
  directamente desde chat

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

**Pre-requisitos:** Java 17+, Maven 3.8+, Node 20+, y el keystore JWT en
`ms-auth/src/main/resources/keys/gs-auth.p12` (ver [Troubleshooting](#-troubleshooting)
si te falta).

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

### Modo 3 — Full stack con Docker (MySQL + RabbitMQ + MinIO)

Si querés probar contra MySQL real (más parecido a producción):

```powershell
.\start-dev.ps1 -WithDocker
```

Arranca primero `docker compose up -d` (MySQL, RabbitMQ, MinIO) y después los
microservicios. Para forzar que los ms usen MySQL en vez de H2, definir
`SPRING_PROFILES_ACTIVE=prod` antes de cada `mvn spring-boot:run`.

### Apagar todo

- **Backend / Frontend:** cerrar las ventanas de PowerShell (o `Ctrl+C` en
  cada una)
- **Docker:** `docker compose down` (o `docker compose down -v` para borrar
  también los volúmenes)

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
│
├── docker-compose.yml        MySQL + RabbitMQ + MinIO
├── start-dev.ps1             Script de arranque del stack completo
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

### Hecho

**Backend** (todos los microservicios compilando y testeados manualmente):

- [x] discovery-server, api-gateway, ms-auth (con JWT/RSA)
- [x] ms-catalogo, ms-pedidos (con entidad `Odontologo` y find-or-create)
- [x] ms-finanzas, ms-stock (el Kanban de producción quedó integrado en ms-pedidos)
- [x] Patrón en capas en todos los ms
- [x] GlobalExceptionHandler unificado (400/403/404/405/409/422/500)
- [x] Perfil `dev` con H2 en todos los ms (no requiere MySQL)

**Frontend** (páginas implementadas):

- [x] Landing pública, Login con AuthGuard
- [x] Dashboard shell (sidebar responsive, PWA)
- [x] Catálogo (CRUD con foto, precio rápido inline)
- [x] Pedidos (lista filtrable + modal con autocomplete inteligente)
- [x] Odontólogos (CRUD completo, grid de cards)
- [x] Producción / Kanban (drag & drop desktop, botón "avanzar" en mobile)
- [x] Entregas (pendientes + historial + mensaje WhatsApp para cadetería)
- [x] Finanzas — Tab Cajas (saldos en vivo + movimientos + alertas)
- [x] Usuarios (CRUD básico, solo ADMIN)

### En progreso

- [ ] Refactor del Kanban para que use `Pedido` directamente (en lugar de la
      entidad separada `TareaProduccion`) — sincroniza el workflow end-to-end

### Pendiente

- [ ] Finanzas — Cuentas / Cobros / Proveedores / Sueldos
- [ ] Stock (CRUD de materiales y movimientos)
- [ ] Reportes (KPIs, gráficos, exportar PDF)
- [ ] Documentos, Escaneos 3D, Auditoría
- [ ] Bot de WhatsApp
- [ ] CI/CD y despliegue productivo

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
- El `.env` real **nunca** se commitea (solo `.env.example` con placeholders)
- `application-prod.properties` está en `.gitignore`

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

### Generar el keystore de ms-auth (si falta tras clonar)

Si después de clonar el repo no tenés `ms-auth/src/main/resources/keys/gs-auth.p12`:

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

---

## 🗺 Roadmap

| Sprint | Foco | Estado |
|--------|------|--------|
| Sprint 0 | Kick-off, estructura, monorepo | ✅ Finalizado |
| Sprint 1 | Auth + Frontend base + Landing | ✅ Finalizado |
| Sprint 2 | Catálogo + Dashboard | ✅ Finalizado |
| Sprint 3 | Pedidos + Odontólogos + Entregas | ✅ Finalizado |
| Sprint 4 | Finanzas — Cajas + dev sin Docker | ✅ Finalizado |
| Sprint 5 | Refactor Kanban + Finanzas (resto) + Stock | 🔵 En curso |
| Sprint 6 | Bot WhatsApp | 🔴 Pendiente |
| Sprint 7 | Reportes + Auditoría | 🔴 Pendiente |
| Sprint 8 | Despliegue y dominio propio | 🔴 Pendiente |

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
