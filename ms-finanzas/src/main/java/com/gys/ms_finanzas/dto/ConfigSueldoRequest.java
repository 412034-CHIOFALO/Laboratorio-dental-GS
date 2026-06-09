package com.gys.ms_finanzas.dto;

import com.gys.ms_finanzas.model.FrecuenciaPago;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ConfigSueldoRequest {

    @NotNull(message = "La frecuencia es obligatoria")
    private FrecuenciaPago frecuencia;

    @NotNull(message = "El monto base es obligatorio")
    @PositiveOrZero(message = "El monto base no puede ser negativo")
    private BigDecimal montoBase;
}
