# ====================================================================
# test-e2e.ps1 -- Pruebas E2E del stack ERP G&S
#
# Requisitos: stack levantado con .\start-dev.ps1 (o Docker)
# Uso:
#   .\test-e2e.ps1
#   .\test-e2e.ps1 -GatewayUrl http://localhost:8080
#   .\test-e2e.ps1 -BotApiKey mi-clave-personalizada
# ====================================================================

param(
    [string]$GatewayUrl   = "http://localhost:8080",
    [string]$AdminUser    = "admin",
    [string]$AdminPass    = "admin123",
    [string]$BotApiKey    = "gs-bot-dev-key-cambiar-en-prod"
)

$ErrorActionPreference = "SilentlyContinue"

# ── Contadores ──────────────────────────────────────────────────────
$pass = 0; $fail = 0; $warn = 0

# ── Helpers ─────────────────────────────────────────────────────────
function Test-Endpoint {
    param(
        [string]$Name,
        [string]$Method = "GET",
        [string]$Url,
        [hashtable]$Headers = @{},
        [string]$Body = $null,
        [int[]]$ExpectedCodes = @(200),
        [string]$Tag = ""
    )

    try {
        $params = @{
            Method  = $Method
            Uri     = $Url
            Headers = $Headers
            TimeoutSec = 10
            UseBasicParsing = $true
        }
        if ($Body) {
            $params.Body        = $Body
            $params.ContentType = "application/json"
        }

        $resp = Invoke-WebRequest @params
        $code = $resp.StatusCode
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        if (-not $code) { $code = 0 }
    }

    $ok = $ExpectedCodes -contains $code
    $script:pass += $ok ? 1 : 0
    $script:fail += $ok ? 0 : 1

    $icon   = $ok ? "✅" : "❌"
    $color  = $ok ? "Green" : "Red"
    $expect = $ExpectedCodes -join "/"
    $tagStr = if ($Tag) { "  [$Tag]" } else { "" }

    Write-Host "  $icon $Name" -ForegroundColor $color -NoNewline
    Write-Host "$tagStr → HTTP $code (esperado: $expect)" -ForegroundColor DarkGray

    return $code
}

function Write-Section { param([string]$Title)
    Write-Host ""
    Write-Host "── $Title " -ForegroundColor Cyan -NoNewline
    Write-Host ("─" * (50 - $Title.Length)) -ForegroundColor DarkGray
}

function Write-Warn { param([string]$Msg)
    $script:warn++
    Write-Host "  ⚠️  $Msg" -ForegroundColor Yellow
}

# ── Banner ──────────────────────────────────────────────────────────
Write-Host ""
Write-Host "╔══════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║      ERP G&S — Test Suite E2E                    ║" -ForegroundColor Cyan
Write-Host "╚══════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host "  Gateway: $GatewayUrl"
Write-Host "  Fecha:   $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"

# ════════════════════════════════════════════════════════════════════
# 1. HEALTH CHECKS — servicios directos
# ════════════════════════════════════════════════════════════════════
Write-Section "1. Health Checks (acceso directo por puerto)"

Test-Endpoint "Eureka Dashboard"     -Url "http://localhost:8761"                -ExpectedCodes 200
Test-Endpoint "ms-auth health"       -Url "http://localhost:8081/actuator/health" -ExpectedCodes 200
Test-Endpoint "ms-catalogo health"   -Url "http://localhost:8083/actuator/health" -ExpectedCodes 200
Test-Endpoint "ms-pedidos health"    -Url "http://localhost:8082/actuator/health" -ExpectedCodes 200
Test-Endpoint "ms-finanzas health"   -Url "http://localhost:8085/actuator/health" -ExpectedCodes 200
Test-Endpoint "ms-stock health"      -Url "http://localhost:8086/actuator/health" -ExpectedCodes 200
Test-Endpoint "api-gateway health"   -Url "$GatewayUrl/actuator/health"           -ExpectedCodes 200

# ════════════════════════════════════════════════════════════════════
# 2. AUTH — login y JWT
# ════════════════════════════════════════════════════════════════════
Write-Section "2. Autenticación"

$loginBody = "{`"username`":`"$AdminUser`",`"password`":`"$AdminPass`"}"
$jwt = $null

try {
    $loginResp = Invoke-WebRequest -Method POST `
        -Uri "$GatewayUrl/ms-auth/api/auth/login" `
        -Body $loginBody `
        -ContentType "application/json" `
        -UseBasicParsing `
        -TimeoutSec 10

    if ($loginResp.StatusCode -eq 200) {
        $jwt = ($loginResp.Content | ConvertFrom-Json).access_token
        Write-Host "  ✅ Login admin/admin123 → JWT obtenido ($($jwt.Length) chars)" -ForegroundColor Green
        $pass++
    } else {
        Write-Host "  ❌ Login falló → HTTP $($loginResp.StatusCode)" -ForegroundColor Red
        $fail++
    }
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    Write-Host "  ❌ Login falló → HTTP $code" -ForegroundColor Red
    $fail++
}

# Intentar login con credenciales inválidas (debe dar 401)
Test-Endpoint "Login credenciales inválidas" -Method POST `
    -Url "$GatewayUrl/ms-auth/api/auth/login" `
    -Body '{"username":"admin","password":"wrong"}' `
    -ExpectedCodes 401 -Tag "seguridad"

if (-not $jwt) {
    Write-Host ""
    Write-Host "  🛑 Sin JWT — omitiendo tests que requieren autenticación." -ForegroundColor Red
    Write-Host "     Verificá que el stack esté levantado y que admin/admin123 exista." -ForegroundColor Yellow
    goto :summary
}

$authHeader = @{ Authorization = "Bearer $jwt" }

# ════════════════════════════════════════════════════════════════════
# 3. ENDPOINTS PROTEGIDOS — requieren JWT válido
# ════════════════════════════════════════════════════════════════════
Write-Section "3. Endpoints protegidos (JWT válido)"

Test-Endpoint "GET /api/catalogo"                     -Url "$GatewayUrl/api/catalogo"                         -Headers $authHeader
Test-Endpoint "GET /api/odontologos"                  -Url "$GatewayUrl/api/odontologos"                      -Headers $authHeader
Test-Endpoint "GET /api/pedidos"                      -Url "$GatewayUrl/api/pedidos"                          -Headers $authHeader
Test-Endpoint "GET /api/pedidos/atrasados"            -Url "$GatewayUrl/api/pedidos/atrasados"                 -Headers $authHeader
Test-Endpoint "GET /api/finanzas/cajas/resumen"       -Url "$GatewayUrl/api/finanzas/cajas/resumen"           -Headers $authHeader
Test-Endpoint "GET /api/finanzas/cajas/movimientos"   -Url "$GatewayUrl/api/finanzas/cajas/movimientos"       -Headers $authHeader
Test-Endpoint "GET /api/stock"                        -Url "$GatewayUrl/api/stock"                            -Headers $authHeader
Test-Endpoint "GET /api/stock/configuracion"          -Url "$GatewayUrl/api/stock/configuracion"              -Headers $authHeader
Test-Endpoint "GET /api/finanzas/sueldos/empleados"   -Url "$GatewayUrl/api/finanzas/sueldos/empleados"       -Headers $authHeader
Test-Endpoint "GET /api/finanzas/sueldos/pagos"       -Url "$GatewayUrl/api/finanzas/sueldos/pagos"           -Headers $authHeader
Test-Endpoint "GET /api/finanzas/sueldos/registros-bot" -Url "$GatewayUrl/api/finanzas/sueldos/registros-bot" -Headers $authHeader
Test-Endpoint "GET /api/finanzas/sueldos/pendientes-efectivo" -Url "$GatewayUrl/api/finanzas/sueldos/pendientes-efectivo" -Headers $authHeader
Test-Endpoint "GET /api/finanzas/cobros/comprobantes" -Url "$GatewayUrl/api/finanzas/cobros/comprobantes"     -Headers $authHeader
Test-Endpoint "GET /api/finanzas/proveedores"         -Url "$GatewayUrl/api/finanzas/proveedores"             -Headers $authHeader

# ════════════════════════════════════════════════════════════════════
# 4. SEGURIDAD — sin token debe dar 401
# ════════════════════════════════════════════════════════════════════
Write-Section "4. Seguridad — sin JWT debe rechazar (401)"

Test-Endpoint "GET /api/catalogo sin token"         -Url "$GatewayUrl/api/catalogo"                   -ExpectedCodes 401 -Tag "seguridad"
Test-Endpoint "GET /api/pedidos sin token"          -Url "$GatewayUrl/api/pedidos"                    -ExpectedCodes 401 -Tag "seguridad"
Test-Endpoint "GET /api/stock sin token"            -Url "$GatewayUrl/api/stock"                      -ExpectedCodes 401 -Tag "seguridad"
Test-Endpoint "GET /api/finanzas/cajas sin token"   -Url "$GatewayUrl/api/finanzas/cajas/resumen"     -ExpectedCodes 401 -Tag "seguridad"

# ════════════════════════════════════════════════════════════════════
# 5. BOT ENDPOINTS — las rutas corregidas (BUG-1)
#    Usan X-Bot-Api-Key, NO JWT
# ════════════════════════════════════════════════════════════════════
Write-Section "5. Bot endpoints — X-Bot-Api-Key (corrección BUG-1)"

$botHeader = @{ "X-Bot-Api-Key" = $BotApiKey }

# 5a: pago-automatico SIN api key → debe dar 401/403 (no 500)
Test-Endpoint "pago-automatico SIN api key → rechaza" -Method POST `
    -Url "$GatewayUrl/api/finanzas/sueldos/pago-automatico" `
    -Body '{"monto":100}' `
    -ExpectedCodes 401,403 -Tag "seguridad"

# 5b: pago-efectivo SIN api key → debe dar 401/403 (no 500)
Test-Endpoint "pago-efectivo SIN api key → rechaza" -Method POST `
    -Url "$GatewayUrl/api/finanzas/sueldos/pago-efectivo" `
    -Body '{"receptorNombre":"Test","monto":100}' `
    -ExpectedCodes 401,403 -Tag "seguridad"

# 5c: pago-automatico CON api key válida — receptor inexistente → 200 con estado RECHAZADO
$pagoAutoBody = '{
  "receptorNombre": "Juan NoExiste",
  "receptorTelefono": "5491100000000",
  "monto": 1500.00,
  "emisor": "Dr. Test",
  "grupoOrigen": "Test E2E",
  "idOperacion": "TEST-E2E-001",
  "cargadoPorNombre": "Script E2E"
}'
$codeAutoBot = Test-Endpoint "pago-automatico CON api key (receptor desconocido → RECHAZADO)" -Method POST `
    -Url "$GatewayUrl/api/finanzas/sueldos/pago-automatico" `
    -Body $pagoAutoBody `
    -Headers $botHeader `
    -ExpectedCodes 200 -Tag "bug-fix"

if ($codeAutoBot -eq 200) {
    Write-Warn "Verificá que el body diga estado=RECHAZADO (receptor desconocido es lo esperado en dev)"
}

# 5d: pago-efectivo CON api key válida → 201 PENDIENTE
$pagoEfecBody = '{
  "receptorNombre": "Test E2E Efectivo",
  "monto": 500.00,
  "cargadoPorNombre": "Script E2E",
  "grupoOrigen": "Test E2E"
}'
Test-Endpoint "pago-efectivo CON api key (queda PENDIENTE → 201)" -Method POST `
    -Url "$GatewayUrl/api/finanzas/sueldos/pago-efectivo" `
    -Body $pagoEfecBody `
    -Headers $botHeader `
    -ExpectedCodes 201 -Tag "bug-fix"

# ════════════════════════════════════════════════════════════════════
# 6. FLUJO DE NEGOCIO — crear y consultar un pedido
# ════════════════════════════════════════════════════════════════════
Write-Section "6. Flujo de negocio — Pedido end-to-end"

# 6a: Buscar o crear un odontólogo
$odontoBody = '{
  "nombre": "Test",
  "apellido": "E2E",
  "email": "test.e2e@lab.com",
  "telefono": "1100000001",
  "matricula": "MAT-E2E-001",
  "cuit": "20-00000001-9"
}'
$codeOdonto = Test-Endpoint "POST /api/odontologos (crear)" -Method POST `
    -Url "$GatewayUrl/api/odontologos" `
    -Body $odontoBody `
    -Headers $authHeader `
    -ExpectedCodes 201,409  # 409 si ya existe del run anterior

# 6b: buscar odontólogos (autocomplete)
Test-Endpoint "GET /api/odontologos/activos" `
    -Url "$GatewayUrl/api/odontologos/activos" `
    -Headers $authHeader `
    -ExpectedCodes 200

# 6c: Listar tipos de trabajo
Test-Endpoint "GET /api/catalogo (tipos de trabajo)" `
    -Url "$GatewayUrl/api/catalogo" `
    -Headers $authHeader

# 6d: Listar pedidos filtrados
Test-Endpoint "GET /api/pedidos?estado=PENDIENTE" `
    -Url "$GatewayUrl/api/pedidos?estado=PENDIENTE" `
    -Headers $authHeader

# ════════════════════════════════════════════════════════════════════
# 7. STOCK — alertas y configuración
# ════════════════════════════════════════════════════════════════════
Write-Section "7. Stock — materiales y alertas"

Test-Endpoint "GET /api/stock (materiales)"         -Url "$GatewayUrl/api/stock"             -Headers $authHeader
Test-Endpoint "GET /api/stock/configuracion"        -Url "$GatewayUrl/api/stock/configuracion" -Headers $authHeader

# Actualizar umbral de alertas (config singleton)
$configBody = '{"umbralMinimo": 5, "habilitado": true}'
Test-Endpoint "PUT /api/stock/configuracion" -Method PUT `
    -Url "$GatewayUrl/api/stock/configuracion" `
    -Body $configBody `
    -Headers $authHeader `
    -ExpectedCodes 200

# ════════════════════════════════════════════════════════════════════
# RESUMEN FINAL
# ════════════════════════════════════════════════════════════════════
:summary
Write-Host ""
Write-Host "══════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  RESULTADO FINAL" -ForegroundColor Cyan
Write-Host "══════════════════════════════════════════════════" -ForegroundColor Cyan

$total = $pass + $fail
$pct   = if ($total -gt 0) { [math]::Round($pass / $total * 100) } else { 0 }

Write-Host "  ✅ Pasaron:  $pass / $total  ($pct%)" -ForegroundColor Green
if ($fail -gt 0) {
    Write-Host "  ❌ Fallaron: $fail / $total" -ForegroundColor Red
}
if ($warn -gt 0) {
    Write-Host "  ⚠️  Avisos:  $warn" -ForegroundColor Yellow
}
Write-Host ""

if ($fail -eq 0) {
    Write-Host "  🎉 Todo verde. El stack funciona correctamente." -ForegroundColor Green
} elseif ($pct -ge 80) {
    Write-Host "  ⚠️  La mayoría pasa, revisá los fallos arriba." -ForegroundColor Yellow
} else {
    Write-Host "  🛑 Varios fallos. Verificá que el stack esté completamente levantado." -ForegroundColor Red
    Write-Host "     Consejo: esperá 30-60s más y volvé a correr." -ForegroundColor DarkGray
}
Write-Host ""
