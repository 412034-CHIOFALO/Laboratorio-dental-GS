package com.gs.ms_pedidos.client;

import com.gs.ms_pedidos.client.dto.MovimientoStockRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Fallback de StockClient: se ejecuta cuando el circuit breaker está OPEN
 * o cuando ms-stock no responde antes del timeout.
 *
 * ConsumoStockService itera los ingredientes de la receta con try-catch
 * individual, así que un fallo de stock no bloquea el cambio de estado
 * del pedido.
 */
@Component
public class StockClientFallback implements StockClient {

    private static final Logger log = LoggerFactory.getLogger(StockClientFallback.class);

    @Override
    public Object registrarMovimiento(MovimientoStockRequest request) {
        log.warn("[CB] Circuit breaker ABIERTO hacia ms-stock. " +
                 "Movimiento material id={} omitido — ajustar stock manualmente.", request.materialId());
        throw new RuntimeException("ms-stock no disponible (circuit breaker abierto)");
    }
}
