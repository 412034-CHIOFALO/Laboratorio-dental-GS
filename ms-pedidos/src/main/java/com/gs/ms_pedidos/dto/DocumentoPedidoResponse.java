package com.gs.ms_pedidos.dto;

import java.time.LocalDateTime;

public record DocumentoPedidoResponse(
        Long id,
        Long pedidoId,
        String fileName,
        String contentType,
        Long tamanioBytes,
        String subidoPor,
        LocalDateTime fechaSubida,
        String urlTemporal
) {}
