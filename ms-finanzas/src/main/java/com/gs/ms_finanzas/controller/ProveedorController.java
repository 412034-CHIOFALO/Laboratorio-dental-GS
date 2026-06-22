package com.gs.ms_finanzas.controller;

import com.gs.ms_finanzas.dto.*;
import com.gs.ms_finanzas.service.IDeudaProveedorService;
import com.gs.ms_finanzas.service.IProveedorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints para el ABM de proveedores de materiales del laboratorio
 * y el registro/consulta de sus deudas pendientes de pago.
 *
 * <p>Los pagos a proveedores pueden originarse manualmente desde la app
 * o ser detectados automáticamente por el bot de WhatsApp cuando un integrante
 * del laboratorio comparte un comprobante de transferencia en el grupo.</p>
 */
@Tag(name = "Proveedores", description = "ABM de proveedores de materiales y registro de deudas")
@RestController
@RequestMapping("/api/finanzas/proveedores")
@RequiredArgsConstructor
public class ProveedorController {

    private final IProveedorService provService;
    private final IDeudaProveedorService deudaService;

    @Operation(summary = "Lista todos los proveedores activos",
               description = "Devuelve la lista de proveedores no desactivados (activo = true), con sus datos de contacto.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @GetMapping
    public ResponseEntity<List<ProveedorResponse>> listar() {
        return ResponseEntity.ok(provService.listarActivos());
    }

    @Operation(summary = "Busca un proveedor por ID",
               description = "Devuelve los datos completos de un proveedor dado su identificador interno.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Proveedor encontrado"),
        @ApiResponse(responseCode = "404", description = "Proveedor no encontrado"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProveedorResponse> buscar(
            @Parameter(description = "ID interno del proveedor", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(provService.buscarPorId(id));
    }

    @Operation(summary = "Crea un nuevo proveedor",
               description = "Registra un nuevo proveedor de materiales con nombre, CUIT, teléfono, email y dirección.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Proveedor creado correctamente"),
        @ApiResponse(responseCode = "400", description = "Datos del request inválidos"),
        @ApiResponse(responseCode = "409", description = "Ya existe un proveedor con ese CUIT"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @PostMapping
    public ResponseEntity<ProveedorResponse> crear(@Valid @RequestBody ProveedorRequest req) {
        return ResponseEntity.status(201).body(provService.crear(req));
    }

    @Operation(summary = "Actualiza los datos de un proveedor",
               description = "Modifica los datos de contacto y nombre de un proveedor existente.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Proveedor actualizado correctamente"),
        @ApiResponse(responseCode = "400", description = "Datos del request inválidos"),
        @ApiResponse(responseCode = "404", description = "Proveedor no encontrado"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ProveedorResponse> actualizar(
            @Parameter(description = "ID interno del proveedor", required = true)
            @PathVariable Long id,
            @Valid @RequestBody ProveedorRequest req) {
        return ResponseEntity.ok(provService.actualizar(id, req));
    }

    @Operation(summary = "Desactiva (baja lógica) un proveedor",
               description = "Marca el proveedor como inactivo (activo = false). No elimina el registro para conservar el historial de deudas.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Proveedor desactivado correctamente"),
        @ApiResponse(responseCode = "404", description = "Proveedor no encontrado"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(
            @Parameter(description = "ID interno del proveedor", required = true)
            @PathVariable Long id) {
        provService.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Deudas de un proveedor específico",
               description = "Lista todas las deudas (PENDIENTE y PAGADO) asociadas a un proveedor.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Deudas obtenidas correctamente"),
        @ApiResponse(responseCode = "404", description = "Proveedor no encontrado"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @GetMapping("/{id}/deudas")
    public ResponseEntity<List<DeudaProveedorResponse>> deudas(
            @Parameter(description = "ID interno del proveedor", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(deudaService.listarPorProveedor(id));
    }

    @Operation(summary = "Lista todas las deudas pendientes a proveedores",
               description = "Devuelve las deudas en estado PENDIENTE de todos los proveedores, para gestión de pagos.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Deudas pendientes obtenidas"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @GetMapping("/deudas/pendientes")
    public ResponseEntity<List<DeudaProveedorResponse>> deudasPendientes() {
        return ResponseEntity.ok(deudaService.listarPendientes());
    }

    @Operation(summary = "Registra una deuda con un proveedor",
               description = "Crea un registro de deuda por compra de materiales al proveedor indicado. El estado inicial es PENDIENTE.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Deuda registrada correctamente"),
        @ApiResponse(responseCode = "400", description = "Datos del request inválidos"),
        @ApiResponse(responseCode = "404", description = "Proveedor no encontrado"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @PostMapping("/deudas")
    public ResponseEntity<DeudaProveedorResponse> registrarDeuda(@Valid @RequestBody DeudaProveedorRequest req) {
        return ResponseEntity.status(201).body(deudaService.registrar(req));
    }
}
