package com.gs.ms_finanzas.dto;

import com.gs.ms_finanzas.model.TipoCaja;

import java.math.BigDecimal;
import java.util.List;

/**
 * Distribución sugerida de un cobro por el algoritmo de cascada: primero cubre
 * el saldo devengado pendiente de los empleados activos (en orden alfabético,
 * el que sigue solo recibe si sobra después del anterior), y lo que sobra
 * ({@code remanente}) queda propuesto para {@code cajaRemanente}.
 *
 * <p>Es solo una sugerencia de cálculo: no registra nada por sí sola. El
 * administrativo la revisa/ajusta y confirma cada línea con los endpoints
 * habituales (pago de sueldo por empleado, movimiento de caja por el resto).</p>
 */
public record DistribucionCascadaResponse(
    List<LineaDistribucionResponse> empleados,
    BigDecimal remanente,
    TipoCaja cajaRemanente
) {}
