package com.gs.ms_finanzas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ComprobanteRequest {

    @NotNull
    private Long pedidoId;

    @NotBlank
    @Size(max = 50)
    private String nroPedido;

    @NotNull
    private Long odontologoId;

    @NotBlank
    @Size(max = 150)
    private String odontologoNombre;

    @NotBlank
    @Size(max = 200)
    private String trabajo;

    @NotNull @Positive
    private BigDecimal monto;

    @NotNull
    private LocalDate fechaEmision;

    private LocalDate fechaVencimiento;

    @Size(max = 1000)
    private String observaciones;
}
