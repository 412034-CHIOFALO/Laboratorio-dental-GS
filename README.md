# Laboratorio Dental G&S — Sistema de Gestión Integral

Sistema SaaS para la gestión integral de un laboratorio dental. Proyecto de tesis universitaria.

**Stack:** Spring Boot 4.x + Spring Cloud (microservicios) + Angular 20 (frontend PWA) + MySQL/H2 + RabbitMQ + MinIO.

---

## 🚀 Levantar el Frontend (modo demo, sin backend)

El frontend tiene un **modo demo** activado por default que mockea todos los datos (login, catálogo, kanban, usuarios). Útil para presentaciones o desarrollo sin levantar los microservicios Java.

### Requisitos

- Node.js 18+
- npm 9+

### Pasos

```bash
git clone https://github.com/412034-CHIOFALO/Laboratorio-dental-GS.git
cd Laboratorio-dental-GS/frontend-app
npm install
npm start
```

Abrir `http://localhost:4200`.

### Credenciales de prueba (modo demo)

| Usuario | Contraseña |
|---|---|
| `admin` | `admin123` |

> En modo demo cualquier user/pass funciona. Para volver al modo real con backends Java, cambiar `useMocks: false` en `src/environments/environment.ts`.

---

## 🏗️ Levantar los Microservicios (modo real)

### Requisitos extra

- Java 17
- Maven 3.9+ (o usar el `mvnw` incluido en cada MS)

### Orden de arranque (con perfil dev = H2 en memoria)

```bash
# Cada uno en una terminal distinta
cd ms-auth        && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # 8081
cd ms-catalogo    && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # 8083
cd ms-produccion  && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # 8084
```

Y desactivar mocks en el frontend: `useMocks: false` en `environment.ts`.

---

## 📁 Estructura

```
.
├── docker-compose.yml          ← MySQL + RabbitMQ + MinIO (opcional)
├── discovery-server/           ← Eureka (puerto 8761)
├── api-gateway/                ← Spring Cloud Gateway (puerto 8080)
├── ms-auth/                    ← Auth + JWT RSA (puerto 8081)
├── ms-catalogo/                ← Tipos de trabajo (puerto 8083)
├── ms-produccion/              ← Tablero Kanban (puerto 8084)
└── frontend-app/               ← Angular 20 PWA
```

---

## 🎨 Sistema de diseño

Tema dark navy + emerald green. Variables CSS centralizadas en `frontend-app/src/styles.css` (`--gs-*`).

---

## 📚 Documentación completa

Ver el vault de Obsidian en `G:\Mi unidad\Tesis\DRIVE\Laboratorio G y S\` para la documentación detallada del proyecto, arquitectura, flujos de negocio y módulos.
