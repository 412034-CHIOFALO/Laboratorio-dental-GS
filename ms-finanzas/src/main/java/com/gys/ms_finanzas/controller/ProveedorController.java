package com.gys.ms_finanzas.controller;

import com.gys.ms_finanzas.dto.*;
import com.gys.ms_finanzas.service.IDeudaProveedorService;
import com.gys.ms_finanzas.service.IProveedorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finanzas/proveedores")
@RequiredArgsConstructor
public class ProveedorController {

    private final IProveedorService provService;
    private final IDeudaProveedorService deudaService;

    @GetMapping
    public ResponseEntity<List<ProveedorResponse>> listar() {
        return ResponseEntity.ok(provService.listarActivos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProveedorResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(provService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<ProveedorResponse> crear(@Valid @RequestBody ProveedorRequest req) {
        return ResponseEntity.status(201).body(provService.crear(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProveedorResponse> actualizar(@PathVariable Long id, @Valid @RequestBody ProveedorRequest req) {
        return ResponseEntity.ok(provService.actualizar(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        provService.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/deudas")
    public ResponseEntity<List<DeudaProveedorResponse>> deudas(@PathVariable Long id) {
        return ResponseEntity.ok(deudaService.listarPorProveedor(id));
    }

    @GetMapping("/deudas/pendientes")
    public ResponseEntity<List<DeudaProveedorResponse>> deudasPendientes() {
        return ResponseEntity.ok(deudaService.listarPendientes());
    }

    @PostMapping("/deudas")
    public ResponseEntity<DeudaProveedorResponse> registrarDeuda(@Valid @RequestBody DeudaProveedorRequest req) {
        return ResponseEntity.status(201).body(deudaService.registrar(req));
    }
}
