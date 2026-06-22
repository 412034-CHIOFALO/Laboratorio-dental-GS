# ====================================================================
# start-dev.ps1 -- Levanta todo el stack del ERP G&S en modo dev
#
# Por defecto: usa H2 in-memory (no necesita MySQL ni Docker).
# Con -WithDocker: ademas arranca MySQL/MinIO via docker compose.
#
# Requisitos:
#   - Java 17+, Maven 3.8+
#   - Node 20+ y Angular CLI (npm install -g @angular/cli)
#   - .env presente en la raiz (para Docker, si se usa)
#   - ms-auth/src/main/resources/keys/gys-auth.p12 presente
#
# Uso:
#   .\start-dev.ps1                # solo backend H2 + frontend
#   .\start-dev.ps1 -WithDocker    # idem + Docker (MySQL/Minio)
#   .\start-dev.ps1 -NoFrontend    # solo backend
# ====================================================================

param(
    [switch]$WithDocker,
    [switch]$NoFrontend
)

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot

Write-Host ""
Write-Host "Levantando ERP G&S en modo dev (H2 en memoria)" -ForegroundColor Green
Write-Host ""

# -- 1. (Opcional) Docker ------------------------------------------
if ($WithDocker) {
    Write-Host "Paso 0: Docker (MySQL + MinIO)" -ForegroundColor Cyan
    Push-Location $root
    docker compose up -d
    Pop-Location
    Write-Host "Esperando 10s para MySQL..." -ForegroundColor Yellow
    Start-Sleep -Seconds 10
}

# -- 2. Helper para abrir cada servicio en una ventana --------------
function Start-Service-Window {
    param([string]$Title, [string]$Path, [string]$Command)
    $argList = "-NoExit -Command `"`$Host.UI.RawUI.WindowTitle='$Title'; cd '$Path'; $Command`""
    Start-Process powershell -ArgumentList $argList
}

# -- 3. Backend ----------------------------------------------------
Write-Host "Paso 1: Discovery Server (Eureka)" -ForegroundColor Cyan
Start-Service-Window "1.discovery" "$root\discovery-server" "mvn spring-boot:run"
Write-Host "  -> esperando 20s para que Eureka este listo..." -ForegroundColor Yellow
Start-Sleep -Seconds 20

Write-Host ""
Write-Host "Paso 2: ms-auth (JWT)" -ForegroundColor Cyan
Start-Service-Window "2.ms-auth" "$root\ms-auth" "mvn spring-boot:run"
Write-Host "  -> esperando 15s para que ms-auth exponga el JWK Set..." -ForegroundColor Yellow
Start-Sleep -Seconds 15

Write-Host ""
Write-Host "Paso 3: 4 ms de negocio en paralelo" -ForegroundColor Cyan
Start-Service-Window "3.catalogo"   "$root\ms-catalogo"   "mvn spring-boot:run"
Start-Service-Window "4.pedidos"    "$root\ms-pedidos"    "mvn spring-boot:run"
Start-Service-Window "5.finanzas"   "$root\ms-finanzas"   "mvn spring-boot:run"
Start-Service-Window "6.stock"      "$root\ms-stock"      "mvn spring-boot:run"
Write-Host "  -> esperando 25s para que los ms se registren en Eureka..." -ForegroundColor Yellow
Start-Sleep -Seconds 25

Write-Host ""
Write-Host "Paso 4: API Gateway" -ForegroundColor Cyan
Start-Service-Window "7.gateway" "$root\api-gateway" "mvn spring-boot:run"
Start-Sleep -Seconds 10

# -- 4. Frontend ---------------------------------------------------
if (-not $NoFrontend) {
    Write-Host ""
    Write-Host "Paso 5: Frontend Angular" -ForegroundColor Cyan
    Start-Service-Window "8.frontend" "$root\frontend-app" "Write-Host 'Recorda cambiar useMocks:false en environment.ts si queres usar el backend real' -ForegroundColor Yellow; ng serve --open"
}

# -- 5. Info final -------------------------------------------------
Write-Host ""
Write-Host "Listo. URLs principales:" -ForegroundColor Green
Write-Host ""
Write-Host "  Eureka:       http://localhost:8761"
Write-Host "  Gateway:      http://localhost:8080/actuator/health"
Write-Host "  Frontend:     http://localhost:4200"
Write-Host ""
Write-Host "  H2 consoles (en cada ms):"
Write-Host "    ms-auth      http://localhost:8081/h2-console (jdbc:h2:mem:authdb)"
Write-Host "    ms-catalogo  http://localhost:8083/h2-console (jdbc:h2:mem:gys_catalogo)"
Write-Host "    ms-pedidos   http://localhost:8082/h2-console (jdbc:h2:mem:gys_pedidos)"
Write-Host "    ms-finanzas  http://localhost:8085/h2-console (jdbc:h2:mem:gys_finanzas)"
Write-Host "    ms-stock     http://localhost:8086/h2-console (jdbc:h2:mem:gys_stock)"
Write-Host ""
if ($WithDocker) {
    Write-Host "  Docker:"
    Write-Host "    MySQL     localhost:3306"
    Write-Host "    MinIO     http://localhost:9001"
    Write-Host ""
}
Write-Host "Para apagar:" -ForegroundColor Yellow
Write-Host "  1) Cerra las ventanas de PowerShell que se abrieron"
if ($WithDocker) { Write-Host "  2) docker compose down" }
Write-Host ""
