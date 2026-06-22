package com.gs.ms_pedidos.client.dto;

import java.math.BigDecimal;

/**
 * DTO para deserializar la receta que devuelve ms-catalogo.
 * Solo incluimos los campos que necesitamos para descontar stock.
 */
public record IngredienteRecetaDTO(
        Long materialId,
        String materialNombre,
        BigDecimal cantidad,
        String unidad
) { }
