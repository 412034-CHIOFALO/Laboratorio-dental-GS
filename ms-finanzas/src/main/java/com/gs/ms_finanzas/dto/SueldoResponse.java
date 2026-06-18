package com.gs.ms_finanzas.dto;

import com.gs.ms_finanzas.model.EstadoSueldo;
import com.gs.ms_finanzas.model.SueldoEmpleado;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SueldoResponse(
    Long id,
    Long empleadoId,
    String empleadoNombre,
    BigDecimal monto,
    int mes,
    int anio,
    EstadoSueldo estado,
    LocalDate fechaPago,
    String referenciaComprobante,
    String observaciones
) {
    public static SueldoResponse from(SueldoEmpleado s) {
        return new SueldoResponse(
            s.getId(),
            s.getEmpleadoId(),
            s.getEmpleadoNombre(),
            s.getMonto(),
            s.getMes(),
            s.getAnio(),
            s.getEstado(),
            s.getFechaPago(),
            s.getReferenciaComprobante(),
            s.getObservaciones()
        );
    }
}
