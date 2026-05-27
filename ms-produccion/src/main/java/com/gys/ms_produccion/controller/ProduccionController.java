package com.gys.ms_produccion.controller;

import com.gys.ms_produccion.dto.TareaRequest;
import com.gys.ms_produccion.dto.TareaResponse;
import com.gys.ms_produccion.model.EstadoTarea;
import com.gys.ms_produccion.service.ITareaProduccionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/produccion")
@RequiredArgsConstructor
public class ProduccionController {

    private final ITareaProduccionService service;

    /** GET /api/produccion/kanban — todas las tareas activas para el tablero */
    @GetMapping("/kanban")
    public ResponseEntity<List<TareaResponse>> kanban() {
        return ResponseEntity.ok(service.listarActivas());
    }

    /** GET /api/produccion/kanban/{estado} — tareas de una columna específica */
    @GetMapping("/kanban/{estado}")
    public ResponseEntity<List<TareaResponse>> kanbanPorEstado(@PathVariable EstadoTarea estado) {
        return ResponseEntity.ok(service.listarPorEstado(estado));
    }

    /** GET /api/produccion/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<TareaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    /** POST /api/produccion — crear tarea manualmente */
    @PostMapping
    public ResponseEntity<TareaResponse> crear(@Valid @RequestBody TareaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(req));
    }

    /** PATCH /api/produccion/{id}/estado?nuevoEstado=EN_PROCESO — avanzar en el flujo */
    @PatchMapping("/{id}/estado")
    public ResponseEntity<TareaResponse> moverEstado(
            @PathVariable Long id,
            @RequestParam EstadoTarea nuevoEstado) {
        return ResponseEntity.ok(service.actualizarEstado(id, nuevoEstado));
    }

    /** PATCH /api/produccion/{id}/tecnico?nombre=Juan — asignar técnico */
    @PatchMapping("/{id}/tecnico")
    public ResponseEntity<TareaResponse> asignarTecnico(
            @PathVariable Long id,
            @RequestParam String nombre) {
        return ResponseEntity.ok(service.asignarTecnico(id, nombre));
    }
}
