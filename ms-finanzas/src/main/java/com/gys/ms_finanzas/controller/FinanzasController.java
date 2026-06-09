package com.gys.ms_finanzas.controller;

import com.gys.ms_finanzas.dto.ComprobanteRequest;
import com.gys.ms_finanzas.dto.ComprobanteResponse;
import com.gys.ms_finanzas.dto.CuentaCorrienteOdontologoResponse;
import com.gys.ms_finanzas.service.IFinanzasService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/finanzas")
@RequiredArgsConstructor
public class FinanzasController {

    private final IFinanzasService service;

    @GetMapping("/comprobantes")
    public ResponseEntity<List<ComprobanteResponse>> listarTodos() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @GetMapping("/comprobantes/pendientes")
    public ResponseEntity<List<ComprobanteResponse>> listarPendientes() {
        return ResponseEntity.ok(service.listarPendientes());
    }

    @GetMapping("/comprobantes/odontologo/{odontologoId}")
    public ResponseEntity<List<ComprobanteResponse>> listarPorOdontologo(
            @PathVariable Long odontologoId) {
        return ResponseEntity.ok(service.listarPorOdontologo(odontologoId));
    }

    @GetMapping("/saldo/odontologo/{odontologoId}")
    public ResponseEntity<BigDecimal> saldoPendiente(@PathVariable Long odontologoId) {
        return ResponseEntity.ok(service.saldoPendienteOdontologo(odontologoId));
    }

    @GetMapping("/comprobantes/{id}")
    public ResponseEntity<ComprobanteResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PostMapping("/comprobantes")
    public ResponseEntity<ComprobanteResponse> emitir(
            @Valid @RequestBody ComprobanteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.emitir(request));
    }

    @PatchMapping("/comprobantes/{id}/cobrar")
    public ResponseEntity<ComprobanteResponse> registrarCobro(@PathVariable Long id) {
        return ResponseEntity.ok(service.registrarCobro(id));
    }

    /**
     * Ranking de odontólogos con deuda pendiente, ordenado de mayor a menor.
     * Una fila por odontólogo (no por comprobante) con saldo total, cantidad
     * de comprobantes, días sin pagar y severidad calculada.
     */
    @GetMapping("/cuentas-corrientes")
    public ResponseEntity<List<CuentaCorrienteOdontologoResponse>> rankingMorosos() {
        return ResponseEntity.ok(service.rankingMorosos());
    }
}
