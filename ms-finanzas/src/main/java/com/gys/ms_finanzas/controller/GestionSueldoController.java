package com.gys.ms_finanzas.controller;

import com.gys.ms_finanzas.dto.*;
import com.gys.ms_finanzas.service.IGestionSueldoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * API de gestión de sueldos del personal (config + devengado + pagos).
 *
 * Rutas bajo /api/finanzas/sueldos/empleados para no colisionar con el
 * SueldoController legacy (/api/finanzas/sueldos?anio&mes).
 */
@RestController
@RequestMapping("/api/finanzas/sueldos")
@RequiredArgsConstructor
public class GestionSueldoController {

    private final IGestionSueldoService service;

    /** Lista todos los integrantes con su config de sueldo y devengado. */
    @GetMapping("/empleados")
    public ResponseEntity<List<EmpleadoSueldoResponse>> listarEmpleados() {
        return ResponseEntity.ok(service.listarEmpleados());
    }

    @GetMapping("/empleados/{usuarioId}")
    public ResponseEntity<EmpleadoSueldoResponse> buscarEmpleado(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(service.buscarEmpleado(usuarioId));
    }

    /** Edición manual de la config de sueldo (frecuencia + monto). */
    @PutMapping("/empleados/{usuarioId}/config")
    public ResponseEntity<EmpleadoSueldoResponse> guardarConfig(
            @PathVariable Long usuarioId,
            @Valid @RequestBody ConfigSueldoRequest req) {
        return ResponseEntity.ok(service.guardarConfig(usuarioId, req));
    }

    /** Ajuste manual del devengado (corrección puntual). */
    @PatchMapping("/empleados/{usuarioId}/devengado")
    public ResponseEntity<EmpleadoSueldoResponse> ajustarDevengado(
            @PathVariable Long usuarioId,
            @RequestBody Map<String, BigDecimal> body) {
        return ResponseEntity.ok(service.ajustarDevengado(usuarioId, body.get("devengado")));
    }

    /** Pago manual desde la app. */
    @PostMapping("/pago")
    public ResponseEntity<PagoSueldoResponse> registrarPago(@Valid @RequestBody PagoSueldoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarPago(req));
    }

    /**
     * Pago detectado por el bot de WhatsApp.
     * El bot identifica al receptor por teléfono y manda el comprobante.
     */
    @PostMapping("/pago-automatico")
    public ResponseEntity<PagoSueldoResponse> registrarPagoAutomatico(
            @Valid @RequestBody PagoAutomaticoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarPagoAutomatico(req));
    }

    /** Histórico de pagos de un empleado. */
    @GetMapping("/empleados/{usuarioId}/pagos")
    public ResponseEntity<List<PagoSueldoResponse>> historialPagos(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(service.historialPagos(usuarioId));
    }

    /** Histórico global de pagos. */
    @GetMapping("/pagos")
    public ResponseEntity<List<PagoSueldoResponse>> historialGlobal() {
        return ResponseEntity.ok(service.historialPagosGlobal());
    }

    /** URL temporal para ver/descargar el comprobante de un pago. */
    @GetMapping("/pagos/{pagoId}/comprobante")
    public ResponseEntity<Map<String, String>> urlComprobante(@PathVariable Long pagoId) {
        return ResponseEntity.ok(Map.of("url", service.urlComprobante(pagoId)));
    }
}
