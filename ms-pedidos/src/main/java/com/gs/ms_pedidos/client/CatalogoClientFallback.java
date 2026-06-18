package com.gs.ms_pedidos.client;

import com.gs.ms_pedidos.client.dto.TipoTrabajoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Fallback de CatalogoClient: se ejecuta cuando el circuit breaker está OPEN
 * o cuando ms-catalogo no responde antes del timeout.
 *
 * ConsumoStockService ya tiene un try-catch que maneja esta excepción con un
 * log warn y deja pasar el cambio de estado del pedido (best-effort).
 */
@Component
public class CatalogoClientFallback implements CatalogoClient {

    private static final Logger log = LoggerFactory.getLogger(CatalogoClientFallback.class);

    @Override
    public TipoTrabajoDTO buscarPorId(Long id) {
        log.warn("[CB] Circuit breaker ABIERTO hacia ms-catalogo. " +
                 "Trabajo id={} no disponible — se omite el descuento de stock.", id);
        throw new RuntimeException("ms-catalogo no disponible (circuit breaker abierto)");
    }
}
