package com.gys.ms_pedidos.controller;

import com.gys.ms_pedidos.dto.EntregaRequest;
import com.gys.ms_pedidos.dto.PedidoRequest;
import com.gys.ms_pedidos.dto.PedidoResponse;
import com.gys.ms_pedidos.model.EstadoPedido;
import com.gys.ms_pedidos.service.IPedidoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final IPedidoService pedidoService;

    @GetMapping
    public ResponseEntity<List<PedidoResponse>> listarTodos() {
        return ResponseEntity.ok(pedidoService.listarTodos());
    }

    @GetMapping("/activos")
    public ResponseEntity<List<PedidoResponse>> listarActivos() {
        return ResponseEntity.ok(pedidoService.listarActivos());
    }

    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<PedidoResponse>> listarPorEstado(@PathVariable EstadoPedido estado) {
        return ResponseEntity.ok(pedidoService.listarPorEstado(estado));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<PedidoResponse> crear(@Valid @RequestBody PedidoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PedidoResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody PedidoRequest request) {
        return ResponseEntity.ok(pedidoService.actualizar(id, request));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<PedidoResponse> actualizarEstado(
            @PathVariable Long id,
            @RequestParam EstadoPedido nuevoEstado) {
        return ResponseEntity.ok(pedidoService.actualizarEstado(id, nuevoEstado));
    }

    /** Marca el pedido como ENTREGADO (solo válido si estaba en LISTO). */
    @PatchMapping("/{id}/entregar")
    public ResponseEntity<PedidoResponse> entregar(
            @PathVariable Long id,
            @Valid @RequestBody EntregaRequest request) {
        return ResponseEntity.ok(pedidoService.marcarEntregado(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        pedidoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
