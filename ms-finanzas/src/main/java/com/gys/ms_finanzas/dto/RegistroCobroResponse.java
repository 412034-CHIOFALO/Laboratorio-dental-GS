package com.gys.ms_finanzas.dto;

import com.gys.ms_finanzas.model.TipoCaja;

import java.math.BigDecimal;
import java.util.List;

public record RegistroCobroResponse(
    String nroComprobante,
    BigDecimal montoRecibido,
    BigDecimal montoPagoSueldos,
    BigDecimal montoIngresadoCaja,
    TipoCaja cajaDestino,
    List<String> sueldosCubiertos,
    String mensaje
) {}
