package com.gys.ms_finanzas.controller;

import com.gys.ms_finanzas.dto.CajaMovimientoResponse;
import com.gys.ms_finanzas.dto.ResumenCajasResponse;
import com.gys.ms_finanzas.model.TipoCaja;
import com.gys.ms_finanzas.service.ICajaService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/finanzas/cajas")
@RequiredArgsConstructor
public class CajaController {

    private final ICajaService service;

    @GetMapping("/resumen")
    public ResponseEntity<ResumenCajasResponse> resumen() {
        return ResponseEntity.ok(service.obtenerResumen());
    }

    @GetMapping("/{tipoCaja}/movimientos")
    public ResponseEntity<List<CajaMovimientoResponse>> movimientosByCaja(@PathVariable TipoCaja tipoCaja) {
        return ResponseEntity.ok(service.listarMovimientosByCaja(tipoCaja));
    }

    @GetMapping("/movimientos")
    public ResponseEntity<List<CajaMovimientoResponse>> movimientosPeriodo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(service.listarMovimientosByPeriodo(desde, hasta));
    }
}
