package com.gys.ms_pedidos.dto;

import com.gys.ms_pedidos.model.EstadoPedido;
import com.gys.ms_pedidos.model.Pedido;
import com.gys.ms_pedidos.model.Prioridad;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PedidoResponse(
        Long id,
        String nroPedido,
        Long odontologoId,
        String odontologoNombre,
        String paciente,
        Long catalogoTrabajoId,
        String trabajo,
        Long tecnicoId,
        String tecnicoNombre,
        LocalDate fechaEntrega,
        EstadoPedido estado,
        Prioridad prioridad,
        BigDecimal precioAcordado,
        String observaciones,
        // ── Entrega ──
        LocalDate fechaEntregaReal,
        String retiradoPor,
        String observacionesEntrega,
        // ── Timestamps ──
        LocalDateTime fechaCreacion,
        LocalDateTime fechaUltimaModificacion
) {
    public static PedidoResponse from(Pedido p) {
        return new PedidoResponse(
                p.getId(), p.getNroPedido(),
                p.getOdontologoId(), p.getOdontologoNombre(),
                p.getPaciente(),
                p.getCatalogoTrabajoId(), p.getTrabajo(),
                p.getTecnicoId(), p.getTecnicoNombre(),
                p.getFechaEntrega(), p.getEstado(), p.getPrioridad(),
                p.getPrecioAcordado(), p.getObservaciones(),
                p.getFechaEntregaReal(), p.getRetiradoPor(), p.getObservacionesEntrega(),
                p.getFechaCreacion(), p.getFechaUltimaModificacion()
        );
    }
}
