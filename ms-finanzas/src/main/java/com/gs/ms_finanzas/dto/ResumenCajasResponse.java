package com.gs.ms_finanzas.dto;

import java.math.BigDecimal;
import java.util.List;

public record ResumenCajasResponse(
    BigDecimal saldoFisica,
    BigDecimal saldoBancaria,
    BigDecimal saldoCompensacion,
    BigDecimal totalDeudaProveedores,
    BigDecimal totalSueldosPendientes,
    List<String> alertas
) {}
