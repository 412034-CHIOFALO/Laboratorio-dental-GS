package com.gs.ms_finanzas.dto;

import com.gs.ms_finanzas.model.TipoCaja;
import com.gs.ms_finanzas.model.TipoMovimientoCaja;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CajaMovimientoRequest(
    @NotNull TipoMovimientoCaja tipo,
    @NotNull TipoCaja tipoCaja,
    @NotBlank @Size(max = 200) String concepto,
    @NotNull @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero") BigDecimal monto,
    @Size(max = 100) String referencia,
    LocalDate fechaMovimiento,
    @Size(max = 100) String creadoPor
) {}
