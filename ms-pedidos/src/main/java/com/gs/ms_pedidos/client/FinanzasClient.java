package com.gs.ms_pedidos.client;

import com.gs.ms_pedidos.client.dto.ComprobanteRequestDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Cliente Feign hacia ms-finanzas.
 *
 * Lo usa EmisionComprobanteService para generar la cuenta por cobrar
 * (comprobante de deuda) cuando un pedido se entrega. La response no se consume:
 * solo nos interesa que el comprobante quede creado.
 *
 * Resolución vía Eureka. El JWT del request actual se propaga con
 * {@link FeignAuthConfig}.
 */
@FeignClient(name = "ms-finanzas", fallback = FinanzasClientFallback.class)
public interface FinanzasClient {

    @PostMapping("/api/finanzas/comprobantes")
    Object emitirComprobante(@RequestBody ComprobanteRequestDTO request);
}
