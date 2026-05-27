package com.gys.ms_produccion.client;

import com.gys.ms_produccion.dto.PedidoKanbanDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Cliente Feign para comunicarse con ms-pedidos a través de Eureka.
 * Usa el nombre del servicio registrado en el discovery, no la URL directa.
 */
@FeignClient(name = "ms-pedidos")
public interface PedidoClient {

    @GetMapping("/api/pedidos")
    List<PedidoKanbanDTO> listarTodos();

    @GetMapping("/api/pedidos/activos")
    List<PedidoKanbanDTO> listarActivos();

    @GetMapping("/api/pedidos/estado/{estado}")
    List<PedidoKanbanDTO> listarPorEstado(@PathVariable String estado);

    @PatchMapping("/api/pedidos/{id}/estado")
    PedidoKanbanDTO actualizarEstado(@PathVariable Long id, @RequestParam String nuevoEstado);
}
