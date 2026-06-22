package com.gs.ms_finanzas.dto;

import com.gs.ms_finanzas.model.TipoCaja;
import com.gs.ms_finanzas.model.TipoMovimientoCaja;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CajaMovimientoRequest(
    @NotNull TipoMovimientoCaja tipo,
    @NotNull TipoCaja tipoCaja,
    @NotBlank String concepto,
    @NotNull @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero") BigDecimal monto,
    String referencia,
    LocalDate fechaMovimiento,
    String creadoPor
) {}
