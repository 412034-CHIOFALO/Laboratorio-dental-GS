package com.gys.ms_pedidos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * Datos que se capturan al pasar un pedido de LISTO a ENTREGADO.
 * Todos opcionales menos {@link #retiradoPor}, que es lo mínimo para auditar.
 */
@Data
public class EntregaRequest {

    @NotBlank(message = "Indicar quién retiró el trabajo es obligatorio")
    @Size(max = 150)
    private String retiradoPor;

    /** Si no viene, se asume hoy. */
    @PastOrPresent(message = "La fecha de entrega no puede ser futura")
    private LocalDate fechaEntregaReal;

    private String observacionesEntrega;
}
