package com.gys.ms_finanzas.dto;

import com.gys.ms_finanzas.model.TipoCobro;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CobroRequest(
    @NotNull Long comprobanteId,
    @NotNull @Positive BigDecimal monto,
    @NotNull TipoCobro tipoCobro,
    Long proveedorId,
    Long deudaProveedorId,
    String observaciones
) {}
