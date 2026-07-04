package com.gs.ms_finanzas.controller;

import com.gs.ms_finanzas.dto.ReporteMensualResponse;
import com.gs.ms_finanzas.service.ReporteMensualService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Reportes financieros mensuales archivados. Se generan automáticamente el día 1 de cada mes
 * (mes anterior) y también se pueden generar manualmente. Se consultan y descargan desde la
 * sección Documentos del panel. Acceso restringido a ADMIN.
 */
@Tag(name = "Reportes mensuales", description = "Reportes financieros mensuales archivados en el sistema")
@RestController
@RequestMapping("/api/finanzas/reportes")
@RequiredArgsConstructor
public class ReporteMensualController {

    private final ReporteMensualService service;

    @Operation(summary = "Lista los reportes mensuales archivados",
               description = "Devuelve los reportes ordenados del más reciente al más antiguo.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Listado obtenido"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado — se requiere rol ADMIN")
    })
    @GetMapping
    public ResponseEntity<List<ReporteMensualResponse>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @Operation(summary = "Genera (o regenera) el reporte de un mes",
               description = "Produce el PDF del mes indicado y lo archiva. Si ya existía, lo sobrescribe.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Reporte generado y archivado"),
        @ApiResponse(responseCode = "422", description = "Mes inválido o almacenamiento no disponible"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado — se requiere rol ADMIN")
    })
    @PostMapping("/generar")
    public ResponseEntity<ReporteMensualResponse> generar(
            @Parameter(description = "Año (ej: 2026)", required = true) @RequestParam int anio,
            @Parameter(description = "Mes (1-12)", required = true) @RequestParam int mes) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.generar(anio, mes, false));
    }

    @Operation(summary = "Enlace de descarga del PDF de un reporte",
               description = "Devuelve una URL temporal (presigned, 15 min) para descargar el PDF archivado.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "URL de descarga generada"),
        @ApiResponse(responseCode = "422", description = "Reporte inexistente o almacenamiento no disponible"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado — se requiere rol ADMIN")
    })
    @GetMapping("/{id}/descarga")
    public ResponseEntity<Map<String, String>> descarga(
            @Parameter(description = "ID del reporte archivado", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(Map.of("url", service.urlDescarga(id)));
    }
}
