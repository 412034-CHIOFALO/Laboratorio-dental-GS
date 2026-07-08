package com.gs.ms_pedidos.dto;

import java.time.LocalDateTime;

public record EscaneoResponse(
        Long id,
        Long pedidoId,
        String fileName,
        String contentType,
        Long tamanioBytes,
        String descripcion,
        String subidoPor,
        LocalDateTime fechaSubida
) {}
