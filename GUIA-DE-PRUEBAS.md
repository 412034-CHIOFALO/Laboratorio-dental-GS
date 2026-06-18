# 🧪 Guía de pruebas — ERP Laboratorio GS

Cómo levantar y probar el sistema completo **de punta a punta**, en **desarrollo** (sin Docker, con hot-reload) y en **producción** (Docker), incluyendo el **bot de WhatsApp**.

---

## 0. Pre-requisitos

| Herramienta | Versión | Para qué |
|---|---|---|
| **JDK 17** | 17.x | Todos los microservicios (Java 17) |
| **Maven** | 3.9+ (o el wrapper `mvnw`) | Compilar/levantar los MS |
| **Node.js** | 18+ | Frontend Angular y el bot |
| **Angular CLI** | 20 | `ng serve` del frontend |
| **Docker Desktop** | ≥ 4.30 | Modo producción + MinIO en dev |

> Verificá: `java -version` (debe decir 17), `node -v`, `docker --version`.

### Servicios y puertos

| Servicio | Puerto | Notas |
|---|---|---|
| discovery-server (Eureka) | **8761** | Registro de servicios |
| ms-auth | **8081** | Login + usuarios + JWT |
| ms-pedidos | **8082** | Pedidos + odontólogos |
| ms-catalogo | **8083** | Tipos de trabajo + recetas |
| ms-finanzas | **8085** | Cajas, cobros, sueldos, proveedores, bot |
| ms-stock | **8086** | Materiales + movimientos |
| api-gateway | **8080** | Única puerta de entrada |
| frontend | **4200** (dev) / **80** (prod) | Angular |

---

## 1. ENTORNO DEV (sin Docker, hot-reload)

En dev cada microservicio usa **H2 en memoria** (perfil `dev` activo por defecto — no hace falta pasar ningún flag). Los datos se borran al reiniciar el MS.

### 1.1 (Opcional) MinIO para comprobantes
Solo si vas a probar guardar/ver el archivo de un comprobante del bot:
```powershell
docker compose up -d minio
```
> Consola MinIO: http://localhost:9001

### 1.2 Backend — orden de arranque
Abrí **una terminal por servicio**, en este orden (esperá que cada uno diga `Started ...Application` antes de seguir, salvo los 4 de negocio que pueden ir en paralelo):

```powershell
# 1) Eureka primero — esperá a que esté en http://localhost:8761
cd discovery-server ; .\mvnw.cmd spring-boot:run

# 2) Auth (H2, no necesita MySQL)
cd ms-auth ; .\mvnw.cmd spring-boot:run

# 3) Los 4 de negocio (una terminal cada uno, pueden ir juntos)
cd ms-catalogo ; .\mvnw.cmd spring-boot:run
cd ms-pedidos  ; .\mvnw.cmd spring-boot:run
cd ms-finanzas ; .\mvnw.cmd spring-boot:run
cd ms-stock    ; .\mvnw.cmd spring-boot:run

# 4) Gateway al final (cuando los demás aparezcan en Eureka)
cd api-gateway ; .\mvnw.cmd spring-boot:run
```

✅ Verificá en **http://localhost:8761** que estén registrados los 6 MS + el gateway.

> **Si un MS no arranca por "puerto ocupado"** (zombie de un reinicio): matá el puerto y arrancá con `clean`:
> ```powershell
> $p=(Get-NetTCPConnection -LocalPort 8083 -State Listen -EA SilentlyContinue).OwningProcess; if($p){Stop-Process -Id $p -Force}
> cd ms-catalogo ; .\mvnw.cmd clean spring-boot:run
> ```

### 1.3 Frontend
```powershell
cd frontend-app
npm install        # solo la primera vez
ng serve --open    # → http://localhost:4200
```
- `environment.ts` tiene `useMocks: false` → usa el **backend real**. (Si lo ponés en `true`, anda solo con datos mock, sin backend.)
- El login en dev pega **directo a ms-auth:8081** (`environment.loginUrl`); el resto va por el gateway.

### 1.4 El bot en DEV
```powershell
cd gs-bot-whatsapp
npm install        # solo la primera vez
# Configurá el .env (copialo de .env.example la primera vez):
#   BACKEND_URL=http://localhost:8080
#   BACKEND_ENABLED=true          ← para que registre de verdad (false = solo loguea)
#   BOT_API_KEY=gs-bot-dev-key-cambiar-en-prod   (debe coincidir con ms-finanzas)
#   GEMINI_API_KEY=...   GEMINI_ENABLED=true     (opcional; si falla, ponelo en false)
#   GRUPOS=Comprobantes Transferencias,Comprobantes Efectivo,Prueba
node index.js
```
- Aparece un **QR** en la terminal → escanealo con el WhatsApp del **chip del lab** (Dispositivos vinculados → Vincular dispositivo).
- Queda guardado en `.wwebjs_auth` → no hay que re-escanear en los próximos arranques.

---

## 2. ENTORNO PROD (Docker)

Levanta **todo** (MySQL + RabbitMQ + MinIO + los 6 MS + gateway + frontend con nginx + **el bot**) con un comando.

```powershell
# Asegurate de que Docker Desktop esté corriendo
docker compose up -d        # primer arranque: compila todo, ~5-8 min
docker compose logs -f      # ver el progreso
```

| URL | Servicio |
|---|---|
| http://localhost | **La app** (frontend) |
| http://localhost:8761 | Eureka |
| http://localhost:9001 | MinIO console |

- En prod las URLs son **relativas**: nginx hace de proxy de `/api/*` y `/ms-auth/*` al gateway → **no hay CORS** (mismo origen).
- Apagar: `docker compose down` (mantiene datos) · `docker compose down -v` (borra todo).

### 2.1 El bot en PROD (automatizado)
El bot está **dockerizado** y arranca solo con `docker compose up`. La sesión de WhatsApp se persiste en un volumen (`wpp_auth`), así que **se reconecta solo** en cada reinicio/deploy.

**La única intervención manual es el primer pairing** (WhatsApp obliga a vincular el dispositivo una vez):
```powershell
docker compose logs -f gs-bot      # mostrá el QR del bot
```
→ Escaneá el QR con el celu del lab **una sola vez**. A partir de ahí, el bot:
- arranca con el stack,
- se reconecta solo aunque reinicies el server,
- **no vuelve a pedir QR** (salvo que WhatsApp desvincule el dispositivo — pasa si está 14+ días sin usarse o lo desvinculás a mano).

> **Por qué no se puede 100% sin tocar nada:** WhatsApp Web exige escanear el QR para vincular un dispositivo nuevo. No hay forma de saltarlo. Pero es **una sola vez**; después es totalmente automático.

---

## 3. El flujo end-to-end a probar

Logueate con **`admin` / `admin123`** (o `tecnico1` / `tecnico123`).

1. **Catálogo** → creá un tipo de trabajo con su **receta** de materiales.
2. **Stock** → cargá los materiales de la receta.
3. **Pedidos** → creá un pedido → pasalo a **producción** (Kanban) → verificá el **descuento automático** de stock.
4. **Pedidos atrasados** → un pedido con +6 días hábiles se resalta en **ámbar**.
5. **Finanzas** → cuentas corrientes, morosos, movimientos manuales, sueldos.
6. **Proveedores** → lista + deudas (las saldadas por el bot aparecen en **PAGADO**).
7. **Bot** → mandá un comprobante a un grupo:
   - **Sueldo**: receptor = un empleado (ej. *Carlos López*).
   - **Pago a proveedor**: receptor = un proveedor (ej. *Luciano Giménez*).
   - **Triangulado**: emisor = un odontólogo (ej. *Dra. Laura Sánchez*) + receptor = un proveedor → reduce **ambas deudas** vía Caja Compensación.
   - **Bot WhatsApp** (pantalla) → el historial muestra todo, incluidos rechazos/duplicados.

---

## 4. Credenciales y datos de prueba

- **Login**: `admin` / `admin123` · `tecnico1` / `tecnico123`
- **Empleados seedeados** (para sueldos del bot): Rebeca González, Carlos López, Mario Giménez, Valentina Torres.
- **Proveedores seedeados**: Dental Import SRL, Luciano Giménez, Protésica del Sur.
- **Odontólogos seedeados** (para triangulados): Dr. Martín García, Dra. Laura Sánchez.
- **H2 console** (dev, directo al puerto del MS, no por el gateway):
  - catálogo → http://localhost:8083/h2-console · JDBC `jdbc:h2:mem:gs_catalogo`
  - pedidos → http://localhost:8082/h2-console · JDBC `jdbc:h2:mem:gs_pedidos`
  - finanzas → http://localhost:8085/h2-console · JDBC `jdbc:h2:mem:gs_finanzas`
  - stock → http://localhost:8086/h2-console · JDBC `jdbc:h2:mem:gs_stock`
  - usuario `sa`, contraseña vacía.

---

## 5. Troubleshooting (lo que aprendimos)

| Síntoma | Causa | Solución |
|---|---|---|
| 404 en todo por el gateway | rutas del gateway no cargan | Verificá `spring.cloud.gateway.server.webmvc.routes` (namespace nuevo de SCG 5.0.1) |
| Error de CORS en el navegador | CORS duplicado | CORS vive **solo** en el gateway; los MS lo tienen deshabilitado |
| La sesión se corta al instante | faltaba el token en las llamadas | el `authInterceptor` ya lo adjunta; refrescá el navegador |
| Login 401 | ms-auth caído | levantá ms-auth (puerto 8081) |
| Un MS levanta "viejo" / no toma cambios | zombie en el puerto + build cacheado | matá el puerto + `.\mvnw.cmd clean spring-boot:run` |
| El bot dice "no encontró empleado" | el receptor es un proveedor | ya se resuelve también contra proveedores; verificá que exista el proveedor |
| El bot no registra (solo loguea) | `BACKEND_ENABLED=false` | ponelo en `true` en el `.env` del bot |

---

## 6. Comandos útiles

```powershell
# Matar lo que esté en un puerto (cambiá 8083 por el que sea)
$p=(Get-NetTCPConnection -LocalPort 8083 -State Listen -EA SilentlyContinue).OwningProcess; if($p){Stop-Process -Id $p -Force}

# Docker
docker compose ps                    # estado de los containers
docker compose logs -f gs-bot        # logs del bot (y el QR)
docker compose up -d --build ms-finanzas   # rebuild de un MS
docker compose down                  # apagar (mantiene datos)
```
