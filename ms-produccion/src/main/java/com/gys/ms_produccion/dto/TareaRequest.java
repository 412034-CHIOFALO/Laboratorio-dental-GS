package com.gys.ms_produccion.dto;

import com.gys.ms_produccion.model.Prioridad;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record TareaRequest(
        @NotBlank String nroPedido,
        @NotBlank String paciente,
        @NotBlank String odontologoNombre,
        @NotBlank String trabajo,
        String tecnicoNombre,
        @NotNull Prioridad prioridad,
        @NotNull LocalDate fechaIngreso,
        LocalDate fechaEntregaEstimada,
        String observaciones
) {}
