package com.gys.ms_finanzas.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SueldoRequest {

    @NotNull
    private Long empleadoId;

    @NotBlank
    private String empleadoNombre;

    @NotNull
    @Positive
    private BigDecimal monto;

    @NotNull
    @Min(1)
    @Max(12)
    private Integer mes;

    @NotNull
    @Min(2020)
    private Integer anio;

    private String observaciones;
}
