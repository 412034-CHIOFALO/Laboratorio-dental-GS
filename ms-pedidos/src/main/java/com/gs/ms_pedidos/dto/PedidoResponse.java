package com.gs.ms_pedidos.dto;

import com.gs.ms_pedidos.model.EstadoPedido;
import com.gs.ms_pedidos.model.Pedido;
import com.gs.ms_pedidos.model.Prioridad;
import com.gs.ms_pedidos.util.DiasHabiles;

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
        LocalDateTime fechaUltimaModificacion,
        // ── Estado de atraso (calculado en el servidor) ──
        /** Días hábiles transcurridos desde la creación hasta hoy (o hasta la entrega). */
        int diasHabilesTranscurridos,
        /** True si supera el umbral configurado de días hábiles sin entregar. */
        boolean atrasado
) {
    public static PedidoResponse from(Pedido p, int diasLimiteAtraso) {
        // Si ya está entregado/cancelado, calculamos hasta la entrega/cancelación
        // (o hasta la última modificación si no hay fechaEntregaReal).
        LocalDateTime ref = (p.getEstado() == EstadoPedido.ENTREGADO || p.getEstado() == EstadoPedido.CANCELADO)
                ? (p.getFechaUltimaModificacion() != null ? p.getFechaUltimaModificacion() : LocalDateTime.now())
                : LocalDateTime.now();

        int dias = DiasHabiles.entre(p.getFechaCreacion(), ref);

        // Sólo está "atrasado" si está activo (no entregado, no cancelado)
        boolean estaAtrasado = (p.getEstado() != EstadoPedido.ENTREGADO
                             && p.getEstado() != EstadoPedido.CANCELADO)
                             && dias >= diasLimiteAtraso;

        return new PedidoResponse(
                p.getId(), p.getNroPedido(),
                p.getOdontologoId(), p.getOdontologoNombre(),
                p.getPaciente(),
                p.getCatalogoTrabajoId(), p.getTrabajo(),
                p.getTecnicoId(), p.getTecnicoNombre(),
                p.getFechaEntrega(), p.getEstado(), p.getPrioridad(),
                p.getPrecioAcordado(), p.getObservaciones(),
                p.getFechaEntregaReal(), p.getRetiradoPor(), p.getObservacionesEntrega(),
                p.getFechaCreacion(), p.getFechaUltimaModificacion(),
                dias, estaAtrasado
        );
    }

    /** Overload con umbral por defecto (6 días hábiles). */
    public static PedidoResponse from(Pedido p) {
        return from(p, 6);
    }
}
