package com.gys.ms_produccion.dto;

import com.gys.ms_produccion.model.EstadoTarea;
import com.gys.ms_produccion.model.Prioridad;
import com.gys.ms_produccion.model.TareaProduccion;

import java.time.LocalDate;

public record TareaResponse(
        Long id,
        String nroPedido,
        String paciente,
        String odontologo,
        String trabajo,
        String tecnico,
        EstadoTarea estado,
        Prioridad prioridad,
        LocalDate fechaIngreso,
        LocalDate fechaEntrega,
        String observaciones
) {
    public static TareaResponse from(TareaProduccion t) {
        return new TareaResponse(
                t.getId(),
                t.getNroPedido(),
                t.getPaciente(),
                t.getOdontologoNombre(),
                t.getTrabajo(),
                t.getTecnicoNombre(),
                t.getEstado(),
                t.getPrioridad(),
                t.getFechaIngreso(),
                t.getFechaEntregaEstimada(),
                t.getObservaciones()
        );
    }
}
