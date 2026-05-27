package com.gys.ms_stock.dto;

import com.gys.ms_stock.model.CategoriaMaterial;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class MaterialRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    private String descripcion;

    @NotNull(message = "La categoría es obligatoria")
    private CategoriaMaterial categoria;

    @NotNull @PositiveOrZero
    private Double stockActual;

    @NotNull @PositiveOrZero
    private Double stockMinimo;

    @NotBlank(message = "La unidad de medida es obligatoria")
    private String unidadMedida;

    @PositiveOrZero
    private BigDecimal precioUnitario;

    private String proveedor;
}
