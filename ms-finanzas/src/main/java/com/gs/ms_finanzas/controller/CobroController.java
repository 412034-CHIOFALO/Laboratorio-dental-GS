package com.gs.ms_finanzas.controller;

import com.gs.ms_finanzas.dto.CobroRequest;
import com.gs.ms_finanzas.dto.RegistroCobroResponse;
import com.gs.ms_finanzas.service.ICobroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoint para el registro de cobros efectuados a odontólogos.
 * Un cobro puede ser en efectivo, por transferencia o triangulado (el odontólogo
 * paga a un proveedor directamente en nombre del laboratorio).
 */
@Tag(name = "Cobros", description = "Registro de cobros a odontólogos para saldar comprobantes pendientes")
@RestController
@RequestMapping("/api/finanzas/cobros")
@RequiredArgsConstructor
public class CobroController {

    private final ICobroService service;

    @Operation(summary = "Registra un cobro a un odontólogo",
               description = "Procesa un cobro (EFECTIVO, TRANSFERENCIA o TRIANGULADO) y actualiza la cuenta corriente del odontólogo, " +
                             "saldando los comprobantes pendientes según el monto recibido.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cobro registrado correctamente"),
        @ApiResponse(responseCode = "400", description = "Datos del request inválidos o monto incorrecto"),
        @ApiResponse(responseCode = "404", description = "Odontólogo o comprobante no encontrado"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado — se requiere rol ADMIN")
    })
    @PostMapping
    public ResponseEntity<RegistroCobroResponse> registrarCobro(@Valid @RequestBody CobroRequest request) {
        return ResponseEntity.ok(service.registrarCobro(request));
    }
}
