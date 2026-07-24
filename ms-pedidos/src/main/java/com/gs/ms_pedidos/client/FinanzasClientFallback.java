package com.gs.ms_pedidos.client;

import com.gs.ms_pedidos.client.dto.ActualizarMontoRequestDTO;
import com.gs.ms_pedidos.client.dto.ComprobanteRequestDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Fallback de FinanzasClient: se ejecuta si ms-finanzas no responde.
 * EmisionComprobanteService maneja la excepción (best-effort): la entrega del
 * pedido no se bloquea, pero el comprobante queda sin generar (reintentable).
 */
@Component
public class FinanzasClientFallback implements FinanzasClient {

    private static final Logger log = LoggerFactory.getLogger(FinanzasClientFallback.class);

    @Override
    public Object emitirComprobante(ComprobanteRequestDTO request) {
        log.warn("[CB] ms-finanzas no disponible. No se generó el comprobante del pedido {}.",
                request.nroPedido());
        throw new RuntimeException("ms-finanzas no disponible (circuit breaker abierto)");
    }

    @Override
    public void actualizarMontoComprobante(Long pedidoId, ActualizarMontoRequestDTO request) {
        log.warn("[CB] ms-finanzas no disponible. No se sincronizó el monto del pedido {}.", pedidoId);
        throw new RuntimeException("ms-finanzas no disponible (circuit breaker abierto)");
    }
}
