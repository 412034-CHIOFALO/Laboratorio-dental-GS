package com.gys.ms_produccion.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO espejo de PedidoResponse de ms-pedidos.
 * Contiene solo los campos necesarios para la vista de producción.
 */
public record PedidoKanbanDTO(
        Long id,
        String nroPedido,
        String odontologoNombre,
        String paciente,
        String trabajo,
        String tecnicoNombre,
        LocalDate fechaEntrega,
        String estado,
        String prioridad,
        LocalDateTime fechaUltimaModificacion
) {}
