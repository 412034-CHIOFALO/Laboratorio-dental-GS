package com.gys.ms_catalogo.dto;

import com.gys.ms_catalogo.model.IngredienteReceta;

import java.math.BigDecimal;

public record IngredienteRecetaResponse(
        Long id,
        Long materialId,
        String materialNombre,
        BigDecimal cantidad,
        String unidad,
        String notas
) {
    public static IngredienteRecetaResponse from(IngredienteReceta i) {
        return new IngredienteRecetaResponse(
                i.getId(),
                i.getMaterialId(),
                i.getMaterialNombre(),
                i.getCantidad(),
                i.getUnidad(),
                i.getNotas()
        );
    }
}
