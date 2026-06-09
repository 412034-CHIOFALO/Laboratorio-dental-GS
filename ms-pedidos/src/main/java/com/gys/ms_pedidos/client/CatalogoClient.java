package com.gys.ms_pedidos.client;

import com.gys.ms_pedidos.client.dto.TipoTrabajoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Cliente Feign hacia ms-catalogo.
 *
 * Lo usa PedidoService para obtener la receta del trabajo cuando un pedido
 * pasa a EN_PROCESO, y así descontar los materiales correspondientes.
 *
 * Resolución vía Eureka: "ms-catalogo" se traduce al hostname interno.
 */
@FeignClient(name = "ms-catalogo")
public interface CatalogoClient {

    /** Trae un tipo de trabajo con su receta completa. */
    @GetMapping("/api/catalogo/{id}")
    TipoTrabajoDTO buscarPorId(@PathVariable("id") Long id);
}
