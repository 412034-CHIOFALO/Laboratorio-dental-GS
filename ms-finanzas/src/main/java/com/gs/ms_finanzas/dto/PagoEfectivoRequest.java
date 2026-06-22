package com.gs.ms_finanzas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Request para registrar un pago en efectivo declarado en el grupo de WhatsApp.
 * El registro queda en estado PENDIENTE hasta que el administrativo lo confirme
 * o rechace desde el sistema.
 */
@Data
public class PagoEfectivoRequest {

    @NotBlank(message = "El nombre del receptor es obligatorio")
    private String receptorNombre;

    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser mayor a cero")
    private BigDecimal monto;

    private String cargadoPorNombre;
    private String cargadoPorTelefono;
    private String grupoOrigen;
}
