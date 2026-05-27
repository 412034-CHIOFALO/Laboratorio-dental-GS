package com.gys.ms_stock.dto;

import com.gys.ms_stock.model.CategoriaMaterial;
import com.gys.ms_stock.model.Material;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MaterialResponse(
        Long id,
        String nombre,
        String descripcion,
        CategoriaMaterial categoria,
        Double stockActual,
        Double stockMinimo,
        String unidadMedida,
        BigDecimal precioUnitario,
        String proveedor,
        boolean activo,
        boolean bajoStock,
        LocalDateTime fechaModificacion
) {
    public static MaterialResponse from(Material m) {
        return new MaterialResponse(
                m.getId(), m.getNombre(), m.getDescripcion(),
                m.getCategoria(), m.getStockActual(), m.getStockMinimo(),
                m.getUnidadMedida(), m.getPrecioUnitario(), m.getProveedor(),
                m.isActivo(),
                m.getStockActual() <= m.getStockMinimo(),   // flag de alerta automático
                m.getFechaModificacion()
        );
    }
}
