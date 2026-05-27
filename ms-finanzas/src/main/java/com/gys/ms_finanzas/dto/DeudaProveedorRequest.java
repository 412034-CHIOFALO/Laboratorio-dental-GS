package com.gys.ms_finanzas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DeudaProveedorRequest {

    @NotNull
    private Long proveedorId;

    @NotBlank
    @Size(max = 300)
    private String descripcion;

    @NotNull
    @Positive
    private BigDecimal monto;

    private LocalDate fechaVencimiento;

    @Size(max = 50)
    private String nroFacturaProveedor;

    @Size(max = 300)
    private String observaciones;
}
