package com.gs.ms_pedidos.client.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload para emitir un comprobante de deuda en ms-finanzas cuando se entrega
 * un pedido. Los nombres de campo coinciden con ms-finanzas ComprobanteRequest.
 */
public record ComprobanteRequestDTO(
        Long pedidoId,
        String nroPedido,
        Long odontologoId,
        String odontologoNombre,
        String trabajo,
        BigDecimal monto,
        LocalDate fechaEmision,
        LocalDate fechaVencimiento,
        String observaciones
) {}
