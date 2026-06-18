package com.gs.ms_finanzas.dto;

import com.gs.ms_finanzas.model.ManejoSobrante;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Pago manual cargado desde la app. */
@Data
public class PagoSueldoRequest {

    @NotNull(message = "El empleado es obligatorio")
    private Long usuarioId;

    @NotNull @Positive(message = "El monto debe ser mayor a cero")
    private BigDecimal monto;

    /** Qué hacer si se paga de más. Default DESCONTAR_PROXIMO. */
    private ManejoSobrante manejoSobrante = ManejoSobrante.DESCONTAR_PROXIMO;

    private LocalDate fecha;

    private String nota;
}
