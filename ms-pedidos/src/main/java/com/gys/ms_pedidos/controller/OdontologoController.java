package com.gys.ms_pedidos.controller;

import com.gys.ms_pedidos.dto.OdontologoRequest;
import com.gys.ms_pedidos.dto.OdontologoResponse;
import com.gys.ms_pedidos.service.IOdontologoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/odontologos")
@RequiredArgsConstructor
public class OdontologoController {

    private final IOdontologoService service;

    /**
     * GET /api/odontologos          → listado completo de activos
     * GET /api/odontologos?q=garcia → autocomplete por fragmento de nombre
     */
    @GetMapping
    public ResponseEntity<List<OdontologoResponse>> listar(
            @RequestParam(name = "q", required = false) String query) {
        if (query != null && !query.isBlank()) {
            return ResponseEntity.ok(service.buscarPorNombre(query));
        }
        return ResponseEntity.ok(service.listarActivos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OdontologoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<OdontologoResponse> crear(@Valid @RequestBody OdontologoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OdontologoResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody OdontologoRequest request) {
        return ResponseEntity.ok(service.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        service.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
