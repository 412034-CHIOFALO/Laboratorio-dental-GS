package com.gys.ms_stock.controller;

import com.gys.ms_stock.dto.MaterialRequest;
import com.gys.ms_stock.dto.MaterialResponse;
import com.gys.ms_stock.dto.MovimientoRequest;
import com.gys.ms_stock.service.IStockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {

    private final IStockService service;

    @GetMapping
    public ResponseEntity<List<MaterialResponse>> listar() {
        return ResponseEntity.ok(service.listarActivos());
    }

    @GetMapping("/alertas")
    public ResponseEntity<List<MaterialResponse>> bajoStock() {
        return ResponseEntity.ok(service.listarBajoStock());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaterialResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<MaterialResponse> crear(@Valid @RequestBody MaterialRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request));
    }

    @PostMapping("/movimiento")
    public ResponseEntity<MaterialResponse> registrarMovimiento(
            @Valid @RequestBody MovimientoRequest request) {
        return ResponseEntity.ok(service.registrarMovimiento(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
