package com.gs.ms_finanzas.dto;

import com.gs.ms_finanzas.model.MedioPago;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Pago manual a la cuenta corriente de un odontólogo. El monto se imputa a sus
 * deudas pendientes (más viejas primero), pudiendo ser parcial.
 */
public record PagoCuentaCorrienteRequest(
        @NotNull(message = "El monto es obligatorio")
        @Positive(message = "El monto debe ser mayor a cero")
        BigDecimal monto,

        @NotNull(message = "El medio de pago es obligatorio")
        MedioPago medio,

        /** Fecha del pago. Si es null, se usa la fecha actual. */
        LocalDate fecha,

        String nota
) {}
