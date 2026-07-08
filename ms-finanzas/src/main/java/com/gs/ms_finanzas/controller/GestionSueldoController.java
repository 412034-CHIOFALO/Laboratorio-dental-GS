package com.gs.ms_finanzas.controller;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;

import com.gs.ms_finanzas.dto.*;
import com.gs.ms_finanzas.exception.BusinessException;
import com.gs.ms_finanzas.model.TipoCaja;
import com.gs.ms_finanzas.service.IGestionSueldoService;
import com.gs.ms_finanzas.service.MinioStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * API de gestión de sueldos del personal (config + devengado + pagos).
 *
 * <p>Rutas bajo /api/finanzas/sueldos/empleados para no colisionar con el
 * SueldoController legacy (/api/finanzas/sueldos?anio&amp;mes).</p>
 *
 * <p>El algoritmo de cascada distribuye los fondos disponibles de las cajas
 * según la configuración de cada empleado (frecuencia y monto base) y maneja
 * automáticamente los sobrantes entre ciclos.</p>
 */
@Tag(name = "Gestión de Sueldos", description = "Configuración de sueldos, registro de pagos y algoritmo de cascada para distribución de fondos")
@RestController
@RequestMapping("/api/finanzas/sueldos")
@RequiredArgsConstructor
@Validated
public class GestionSueldoController {

    private final IGestionSueldoService service;
    private final MinioStorageService minioStorage;

    @Operation(summary = "Lista todos los empleados con su estado de sueldo",
               description = "Devuelve todos los integrantes del laboratorio con su configuración de sueldo actual (frecuencia, monto base) y el saldo devengado pendiente de pago.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Listado de empleados obtenido"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado — se requiere rol ADMIN")
    })
    @GetMapping("/empleados")
    public ResponseEntity<List<EmpleadoSueldoResponse>> listarEmpleados() {
        return ResponseEntity.ok(service.listarEmpleados());
    }

    @Operation(summary = "Busca un empleado por su ID de usuario",
               description = "Devuelve la configuración de sueldo y estado de cuenta de un empleado específico.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Empleado encontrado"),
        @ApiResponse(responseCode = "404", description = "Empleado no encontrado"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado — se requiere rol ADMIN")
    })
    @GetMapping("/empleados/{usuarioId}")
    public ResponseEntity<EmpleadoSueldoResponse> buscarEmpleado(
            @Parameter(description = "ID del usuario en ms-auth", required = true)
            @PathVariable @Positive Long usuarioId) {
        return ResponseEntity.ok(service.buscarEmpleado(usuarioId));
    }

    @Operation(summary = "Configura o actualiza el sueldo de un empleado",
               description = "Edición manual de la configuración de sueldo: frecuencia de pago (DIARIO/SEMANAL/QUINCENAL/MENSUAL) y monto base por ciclo.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Configuración guardada correctamente"),
        @ApiResponse(responseCode = "400", description = "Datos del request inválidos"),
        @ApiResponse(responseCode = "404", description = "Empleado no encontrado"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado — se requiere rol ADMIN")
    })
    @PutMapping("/empleados/{usuarioId}/config")
    public ResponseEntity<EmpleadoSueldoResponse> guardarConfig(
            @Parameter(description = "ID del usuario en ms-auth", required = true)
            @PathVariable @Positive Long usuarioId,
            @Valid @RequestBody ConfigSueldoRequest req) {
        return ResponseEntity.ok(service.guardarConfig(usuarioId, req));
    }

    @Operation(summary = "Ajusta manualmente el saldo devengado de un empleado",
               description = "Corrección puntual del saldo devengado, sin generar un pago real. Útil para reconciliaciones contables.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Devengado ajustado correctamente"),
        @ApiResponse(responseCode = "404", description = "Empleado no encontrado"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado — se requiere rol ADMIN")
    })
    @PatchMapping("/empleados/{usuarioId}/devengado")
    public ResponseEntity<EmpleadoSueldoResponse> ajustarDevengado(
            @Parameter(description = "ID del usuario en ms-auth", required = true)
            @PathVariable @Positive Long usuarioId,
            @RequestBody Map<String, BigDecimal> body) {
        BigDecimal devengado = body.get("devengado");
        if (devengado == null) {
            throw new com.gs.ms_finanzas.exception.BusinessException("El campo 'devengado' es obligatorio.");
        }
        return ResponseEntity.ok(service.ajustarDevengado(usuarioId, devengado));
    }

    @Operation(summary = "Registra un pago de sueldo manual",
               description = "Crea un registro de pago desde la aplicación web. Descuenta del saldo devengado del empleado y aplica la política de manejo de sobrante configurada.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Pago registrado correctamente"),
        @ApiResponse(responseCode = "400", description = "Datos del request inválidos"),
        @ApiResponse(responseCode = "404", description = "Empleado no encontrado"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado — se requiere rol ADMIN")
    })
    @PostMapping("/pago")
    public ResponseEntity<PagoSueldoResponse> registrarPago(@Valid @RequestBody PagoSueldoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarPago(req));
    }

    @Operation(summary = "Registra un pago detectado por el bot de WhatsApp",
               description = "Endpoint exclusivo para el bot. El bot identifica al receptor (empleado o proveedor) por el número de teléfono extraído del comprobante " +
                             "y llama a este endpoint con el resultado. Siempre retorna 200; el resultado real (REGISTRADO/RECHAZADO/DUPLICADO) viene en el body.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Procesamiento completado (ver campo estado en el body)"),
        @ApiResponse(responseCode = "400", description = "Request del bot inválido"),
        @ApiResponse(responseCode = "403", description = "API key del bot inválida o ausente")
    })
    @PostMapping("/pago-automatico")
    public ResponseEntity<RegistroPagoBotResponse> registrarPagoAutomatico(
            @Valid @RequestBody PagoAutomaticoRequest req) {
        // Siempre 200: el resultado (registrado/rechazado/duplicado) viene en el body.
        return ResponseEntity.ok(service.registrarPagoAutomatico(req));
    }

    @Operation(summary = "Historial de pagos de un empleado",
               description = "Lista todos los pagos realizados a un empleado específico, ordenados por fecha descendente.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Historial obtenido"),
        @ApiResponse(responseCode = "404", description = "Empleado no encontrado"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado — se requiere rol ADMIN")
    })
    @GetMapping("/empleados/{usuarioId}/pagos")
    public ResponseEntity<List<PagoSueldoResponse>> historialPagos(
            @Parameter(description = "ID del usuario en ms-auth", required = true)
            @PathVariable @Positive Long usuarioId) {
        return ResponseEntity.ok(service.historialPagos(usuarioId));
    }

    @Operation(summary = "Historial global de todos los pagos de sueldos",
               description = "Lista todos los pagos registrados (manuales y del bot) de todos los empleados, ordenados por fecha descendente.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Historial global obtenido"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado — se requiere rol ADMIN")
    })
    @GetMapping("/pagos")
    public ResponseEntity<List<PagoSueldoResponse>> historialGlobal() {
        return ResponseEntity.ok(service.historialPagosGlobal());
    }

    @Operation(summary = "Sugiere cómo distribuir un cobro (algoritmo de cascada)",
               description = "Cubre primero el saldo devengado pendiente de los empleados activos (orden alfabético) "
                   + "y propone asignar lo que sobra a la caja indicada. Es solo un cálculo: no registra nada — "
                   + "el administrativo confirma cada línea con los endpoints habituales de pago/movimiento.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Distribución sugerida calculada"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado — se requiere rol ADMIN")
    })
    @GetMapping("/cascada/sugerir")
    public ResponseEntity<DistribucionCascadaResponse> sugerirCascada(
            @Parameter(description = "Monto del cobro a distribuir", required = true)
            @RequestParam @Positive BigDecimal monto,
            @Parameter(description = "Caja a la que se propone asignar el remanente", required = true)
            @RequestParam TipoCaja cajaRemanente) {
        return ResponseEntity.ok(service.sugerirCascada(monto, cajaRemanente));
    }

    @Operation(summary = "Archivo del comprobante de un pago",
               description = "Sirve el comprobante almacenado en MinIO de un pago específico, en streaming a través del propio backend (sin exponer MinIO al navegador).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Archivo servido correctamente"),
        @ApiResponse(responseCode = "404", description = "Pago no encontrado o sin comprobante"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado — se requiere rol ADMIN")
    })
    @GetMapping("/pagos/{pagoId}/comprobante/archivo")
    public ResponseEntity<InputStreamResource> archivoComprobante(
            @Parameter(description = "ID del pago de sueldo", required = true)
            @PathVariable @Positive Long pagoId) {
        return streamComprobante(service.objectKeyComprobante(pagoId));
    }

    @Operation(summary = "Historial de todos los registros procesados por el bot",
               description = "Lista TODO lo que el bot procesó: pagos de sueldo exitosos, pagos a proveedores, rechazos por receptor desconocido y duplicados. " +
                             "Útil para auditoría y diagnóstico.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Registros del bot obtenidos"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado — se requiere rol ADMIN")
    })
    @GetMapping("/registros-bot")
    public ResponseEntity<List<RegistroPagoBotResponse>> registrosBot() {
        return ResponseEntity.ok(service.listarRegistrosBot());
    }

    @Operation(summary = "Archivo del comprobante de un registro del bot",
               description = "Sirve el comprobante asociado a un registro del bot (imagen enviada al grupo de WhatsApp), en streaming a través del propio backend.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Archivo servido correctamente"),
        @ApiResponse(responseCode = "404", description = "Registro no encontrado o sin comprobante"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado — se requiere rol ADMIN")
    })
    @GetMapping("/registros-bot/{registroId}/comprobante/archivo")
    public ResponseEntity<InputStreamResource> archivoComprobanteRegistro(
            @Parameter(description = "ID del registro del bot", required = true)
            @PathVariable @Positive Long registroId) {
        return streamComprobante(service.objectKeyComprobanteRegistro(registroId));
    }

    private ResponseEntity<InputStreamResource> streamComprobante(String objectKey) {
        InputStream in = minioStorage.descargar(objectKey);
        if (in == null) {
            throw new BusinessException("No se pudo obtener el comprobante (MinIO no disponible)");
        }
        return ResponseEntity.ok()
                .contentType(mediaTypeDe(objectKey))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(new InputStreamResource(in));
    }

    private MediaType mediaTypeDe(String objectKey) {
        String key = objectKey.toLowerCase();
        if (key.endsWith(".pdf"))                        return MediaType.APPLICATION_PDF;
        if (key.endsWith(".png"))                         return MediaType.IMAGE_PNG;
        if (key.endsWith(".jpg") || key.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        if (key.endsWith(".gif"))                          return MediaType.IMAGE_GIF;
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    // ── Efectivo: borrador + confirmación ──────────────────────────────

    @Operation(summary = "Registra un pago en efectivo declarado en el grupo",
               description = "El bot llama a este endpoint cuando detecta 'efectivo NNN (Receptor)' en el grupo. " +
                             "El registro queda en PENDIENTE con fuente=EFECTIVO hasta que el administrativo lo confirme o rechace.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Registro PENDIENTE creado"),
        @ApiResponse(responseCode = "400", description = "Request inválido"),
        @ApiResponse(responseCode = "403", description = "API key del bot inválida")
    })
    @PostMapping("/pago-efectivo")
    public ResponseEntity<RegistroPagoBotResponse> registrarPagoEfectivo(
            @Valid @RequestBody PagoEfectivoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarPagoEfectivo(req));
    }

    @Operation(summary = "Lista los pagos en efectivo pendientes de confirmación",
               description = "Devuelve todos los registros del bot con estado=PENDIENTE y fuente=EFECTIVO, " +
                             "para que el administrativo los confirme o rechace desde la UI.")
    @GetMapping("/pendientes-efectivo")
    public ResponseEntity<List<RegistroPagoBotResponse>> listarPendientesEfectivo() {
        return ResponseEntity.ok(service.listarPendientesEfectivo());
    }

    @Operation(summary = "Confirma un pago en efectivo pendiente",
               description = "El administrativo confirma el pago: se aplica al sueldo del empleado (o a la deuda del proveedor) " +
                             "y se registra el egreso de la caja física.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Efectivo confirmado y aplicado"),
        @ApiResponse(responseCode = "404", description = "Registro no encontrado"),
        @ApiResponse(responseCode = "400", description = "El registro no está en estado PENDIENTE")
    })
    @PostMapping("/registros-bot/{id}/confirmar")
    public ResponseEntity<RegistroPagoBotResponse> confirmarEfectivo(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(service.confirmarEfectivo(id));
    }

    @Operation(summary = "Rechaza un pago en efectivo pendiente",
               description = "El administrativo rechaza el pago con un motivo opcional. " +
                             "El registro pasa a RECHAZADO y queda en el historial.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Efectivo rechazado"),
        @ApiResponse(responseCode = "404", description = "Registro no encontrado"),
        @ApiResponse(responseCode = "400", description = "El registro no está en estado PENDIENTE")
    })
    @PostMapping("/registros-bot/{id}/rechazar")
    public ResponseEntity<RegistroPagoBotResponse> rechazarEfectivo(
            @PathVariable @Positive Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String motivo = body != null ? body.get("motivo") : null;
        return ResponseEntity.ok(service.rechazarEfectivo(id, motivo));
    }
}
