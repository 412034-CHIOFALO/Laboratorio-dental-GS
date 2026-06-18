package com.gs.ms_finanzas.controller;

import com.gs.ms_finanzas.dto.SueldoRequest;
import com.gs.ms_finanzas.dto.SueldoResponse;
import com.gs.ms_finanzas.service.ISueldoService;
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
 * Endpoints legacy para la consulta y registro de sueldos mensuales devengados
 * por los empleados del laboratorio.
 *
 * <p>Este controller opera sobre la entidad {@code SueldoEmpleado} que representa
 * el sueldo mensual generado (devengado) por empleado/mes. Para la gestión avanzada
 * (configuración, pagos con cascada, bot) ver {@code GestionSueldoController}.</p>
 */
@Tag(name = "Sueldos", description = "Consulta del estado de sueldos mensuales de los empleados del laboratorio")
@RestController
@RequestMapping("/api/finanzas/sueldos")
@RequiredArgsConstructor
public class SueldoController {

    private final ISueldoService service;

    @Operation(summary = "Lista sueldos de un mes/año específico",
               description = "Devuelve todos los sueldos devengados (PENDIENTE y PAGADO) para el período indicado.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sueldos del período obtenidos"),
        @ApiResponse(responseCode = "400", description = "Parámetros de año o mes inválidos"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado — se requiere rol ADMIN")
    })
    @GetMapping
    public ResponseEntity<List<SueldoResponse>> listar(
            @Parameter(description = "Año del período (ej: 2024)", required = true)
            @RequestParam int anio,
            @Parameter(description = "Mes del período (1-12)", required = true)
            @RequestParam int mes) {
        return ResponseEntity.ok(service.listarPorMes(anio, mes));
    }

    @Operation(summary = "Lista sueldos pendientes de un mes/año",
               description = "Filtra únicamente los sueldos en estado PENDIENTE para el período indicado.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sueldos pendientes obtenidos"),
        @ApiResponse(responseCode = "400", description = "Parámetros de año o mes inválidos"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado — se requiere rol ADMIN")
    })
    @GetMapping("/pendientes")
    public ResponseEntity<List<SueldoResponse>> pendientes(
            @Parameter(description = "Año del período (ej: 2024)", required = true)
            @RequestParam int anio,
            @Parameter(description = "Mes del período (1-12)", required = true)
            @RequestParam int mes) {
        return ResponseEntity.ok(service.listarPendientesMes(anio, mes));
    }

    @Operation(summary = "Registra un sueldo devengado",
               description = "Crea el registro de sueldo mensual para un empleado en un período dado. El estado inicial es PENDIENTE.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Sueldo registrado correctamente"),
        @ApiResponse(responseCode = "400", description = "Datos del request inválidos"),
        @ApiResponse(responseCode = "409", description = "Ya existe un registro de sueldo para ese empleado y período"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado — se requiere rol ADMIN")
    })
    @PostMapping
    public ResponseEntity<SueldoResponse> registrar(@Valid @RequestBody SueldoRequest req) {
        return ResponseEntity.status(201).body(service.registrar(req));
    }
}
