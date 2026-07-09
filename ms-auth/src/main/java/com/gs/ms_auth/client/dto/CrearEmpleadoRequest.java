package com.gs.ms_auth.client.dto;

import java.math.BigDecimal;

/** Espejo del DTO de alta de empleado en ms-finanzas (POST /api/finanzas/sueldos/empleados). */
public record CrearEmpleadoRequest(
    Long usuarioId,
    String nombre,
    String rol,
    String telefono,
    String frecuencia,
    BigDecimal montoBase
) {}
