package com.gs.ms_pedidos.client;

import com.gs.ms_pedidos.client.dto.MovimientoStockRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Cliente Feign hacia ms-stock.
 *
 * Usado para descontar materiales del stock cuando un pedido entra en
 * producción. La response (MaterialResponse) la deserializamos como Object
 * porque ms-pedidos no la consume — solo nos interesa que no haya error.
 */
@FeignClient(name = "ms-stock", fallback = StockClientFallback.class)
public interface StockClient {

    @PostMapping("/api/stock/movimiento")
    Object registrarMovimiento(@RequestBody MovimientoStockRequest request);
}
