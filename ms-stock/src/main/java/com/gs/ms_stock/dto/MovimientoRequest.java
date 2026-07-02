package com.gs.ms_stock.dto;

import com.gs.ms_stock.model.TipoMovimiento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MovimientoRequest {

    @NotNull
    private Long materialId;

    @NotNull
    private TipoMovimiento tipo;

    @NotNull @Positive(message = "La cantidad debe ser mayor que cero")
    private Double cantidad;

    @Size(max = 300)
    private String motivo;

    private Long pedidoId;
}
