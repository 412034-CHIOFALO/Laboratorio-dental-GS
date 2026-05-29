package com.gys.ms_pedidos.model;

/**
 * Estados por los que pasa un pedido en su ciclo de vida.
 *
 * Flujo normal:
 *   RECIBIDO → EN_PROCESO → CONTROL → LISTO → ENTREGADO
 *
 * Estados especiales:
 *   CANCELADO — se puede cancelar desde cualquier estado anterior a ENTREGADO.
 */
public enum EstadoPedido {
    RECIBIDO,
    EN_PROCESO,
    CONTROL,
    LISTO,
    ENTREGADO,
    CANCELADO
}
