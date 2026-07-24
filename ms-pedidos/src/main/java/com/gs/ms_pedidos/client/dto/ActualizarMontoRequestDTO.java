package com.gs.ms_pedidos.client.dto;

import java.math.BigDecimal;

/**
 * Payload para sincronizar el monto del comprobante de un pedido ya entregado
 * en ms-finanzas. El nombre de campo coincide con ms-finanzas ActualizarMontoRequest.
 */
public record ActualizarMontoRequestDTO(BigDecimal monto) {}
