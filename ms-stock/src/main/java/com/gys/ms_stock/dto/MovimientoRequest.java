package com.gys.ms_stock.dto;

import com.gys.ms_stock.model.TipoMovimiento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class MovimientoRequest {

    @NotNull
    private Long materialId;

    @NotNull
    private TipoMovimiento tipo;

    @NotNull @Positive(message = "La cantidad debe ser mayor que cero")
    private Double cantidad;

    private String motivo;

    private Long pedidoId;
}
