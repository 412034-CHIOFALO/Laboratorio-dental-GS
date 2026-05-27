package com.gys.ms_catalogo.dto;

import com.gys.ms_catalogo.model.Categoria;
import com.gys.ms_catalogo.model.TipoTrabajo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TipoTrabajoResponse(
        Long id,
        String nombre,
        String descripcion,
        BigDecimal precio,
        Categoria categoria,
        Integer tiempoEstimadoDias,
        String fotoUrl,
        boolean activo,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaModificacion
) {
    public static TipoTrabajoResponse from(TipoTrabajo t) {
        return new TipoTrabajoResponse(
                t.getId(), t.getNombre(), t.getDescripcion(),
                t.getPrecio(), t.getCategoria(), t.getTiempoEstimadoDias(),
                t.getFotoUrl(), t.isActivo(),
                t.getFechaCreacion(), t.getFechaModificacion()
        );
    }
}
