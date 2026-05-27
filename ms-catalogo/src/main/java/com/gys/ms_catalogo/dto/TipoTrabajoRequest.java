package com.gys.ms_catalogo.dto;

import com.gys.ms_catalogo.model.Categoria;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TipoTrabajoRequest {

    @NotBlank(message = "El nombre del trabajo es obligatorio")
    private String nombre;

    private String descripcion;

    @PositiveOrZero(message = "El precio no puede ser negativo")
    private BigDecimal precio;

    @NotNull(message = "La categoría es obligatoria")
    private Categoria categoria;

    @PositiveOrZero
    private Integer tiempoEstimadoDias = 0;

    private String fotoUrl;
}
