package com.gs.ms_stock.dto;

import com.gs.ms_stock.model.CategoriaMaterial;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class MaterialRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
    private String nombre;

    @Size(max = 1000)
    private String descripcion;

    @NotNull(message = "La categoría es obligatoria")
    private CategoriaMaterial categoria;

    @NotNull @PositiveOrZero
    private Double stockActual;

    @NotNull @PositiveOrZero
    private Double stockMinimo;

    @NotBlank(message = "La unidad de medida es obligatoria")
    @Size(max = 30, message = "La unidad de medida no puede superar los 30 caracteres")
    private String unidadMedida;

    @PositiveOrZero
    private BigDecimal precioUnitario;

    @Size(max = 200)
    private String proveedor;

    /**
     * true = se descuenta del stock al usarlo (caso normal).
     * false = solo se verifica (esmaltes, pinceles — "uso por pinceladas").
     */
    private Boolean descuentaStock = true;
}
