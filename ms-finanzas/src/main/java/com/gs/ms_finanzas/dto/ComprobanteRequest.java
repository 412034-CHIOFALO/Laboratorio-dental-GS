package com.gs.ms_finanzas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ComprobanteRequest {

    @NotNull
    private Long pedidoId;

    @NotBlank
    private String nroPedido;

    @NotNull
    private Long odontologoId;

    @NotBlank
    private String odontologoNombre;

    @NotBlank
    private String trabajo;

    @NotNull @Positive
    private BigDecimal monto;

    @NotNull
    private LocalDate fechaEmision;

    private LocalDate fechaVencimiento;

    private String observaciones;
}
