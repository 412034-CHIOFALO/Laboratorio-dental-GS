package com.gys.ms_finanzas.dto;

import com.gys.ms_finanzas.model.CajaMovimiento;
import com.gys.ms_finanzas.model.TipoCaja;
import com.gys.ms_finanzas.model.TipoMovimientoCaja;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CajaMovimientoResponse(
    Long id,
    TipoMovimientoCaja tipo,
    TipoCaja tipoCaja,
    String concepto,
    BigDecimal monto,
    String referencia,
    LocalDate fechaMovimiento,
    String creadoPor
) {
    public static CajaMovimientoResponse from(CajaMovimiento m) {
        return new CajaMovimientoResponse(
            m.getId(),
            m.getTipo(),
            m.getTipoCaja(),
            m.getConcepto(),
            m.getMonto(),
            m.getReferencia(),
            m.getFechaMovimiento(),
            m.getCreadoPor()
        );
    }
}
