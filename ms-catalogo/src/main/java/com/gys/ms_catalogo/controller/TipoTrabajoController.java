package com.gys.ms_catalogo.controller;

import com.gys.ms_catalogo.dto.TipoTrabajoRequest;
import com.gys.ms_catalogo.dto.TipoTrabajoResponse;
import com.gys.ms_catalogo.model.Categoria;
import com.gys.ms_catalogo.service.ITipoTrabajoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/catalogo")
@RequiredArgsConstructor
public class TipoTrabajoController {

    private final ITipoTrabajoService service;

    @GetMapping
    public ResponseEntity<List<TipoTrabajoResponse>> listar(
            @RequestParam(required = false) Categoria categoria,
            @RequestParam(required = false) String nombre) {

        if (nombre != null && !nombre.isBlank()) {
            return ResponseEntity.ok(service.buscarPorNombre(nombre));
        }
        if (categoria != null) {
            return ResponseEntity.ok(service.listarPorCategoria(categoria));
        }
        return ResponseEntity.ok(service.listarActivos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipoTrabajoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<TipoTrabajoResponse> crear(@Valid @RequestBody TipoTrabajoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TipoTrabajoResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody TipoTrabajoRequest request) {
        return ResponseEntity.ok(service.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
