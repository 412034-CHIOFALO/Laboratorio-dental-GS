package com.gs.ms_pedidos.client.dto;

/**
 * Request body para POST /api/stock/movimiento de ms-stock.
 *
 *   tipo:
 *     ENTRADA  → compra / reposición
 *     SALIDA   → consumo por producción (nuestro caso)
 *     AJUSTE   → corrección manual
 */
public record MovimientoStockRequest(
        Long materialId,
        String materialNombre,
        String tipo,
        Double cantidad,
        String motivo,
        Long pedidoId
) { }
