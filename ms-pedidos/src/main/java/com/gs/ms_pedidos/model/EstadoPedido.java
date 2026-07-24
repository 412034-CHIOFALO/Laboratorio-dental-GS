package com.gs.ms_pedidos.model;

/**
 * Estados por los que pasa un pedido en su ciclo de vida.
 *
 * <h3>Flujo normal</h3>
 * <pre>
 *   RECIBIDO → EN_PROCESO → CONTROL → LISTO → ENTREGADO
 * </pre>
 *
 * <h3>Descripciones</h3>
 * <ul>
 *   <li><b>RECIBIDO</b>: el pedido fue registrado pero todavía no inició producción.</li>
 *   <li><b>EN_PROCESO</b>: el técnico comenzó el trabajo. Al entrar en este estado
 *       se descuenta el stock de materiales de forma automática (solo una vez).</li>
 *   <li><b>CONTROL</b>: el trabajo terminó la producción y está en revisión de calidad.</li>
 *   <li><b>LISTO</b>: aprobado por control de calidad, listo para ser retirado.</li>
 *   <li><b>ENTREGADO</b>: el odontólogo (o mensajero) retiró el trabajo del laboratorio.
 *       Estado terminal positivo.</li>
 *   <li><b>CANCELADO</b>: el pedido fue cancelado. Puede cancelarse desde cualquier estado
 *       anterior a ENTREGADO. Estado terminal negativo.</li>
 * </ul>
 */
public enum EstadoPedido {
    RECIBIDO,
    EN_PROCESO,
    CONTROL,
    LISTO,
    ENTREGADO,
    CANCELADO;

    /**
     * true si este estado ya alcanzó o superó EN_PROCESO dentro del flujo normal.
     *
     * El Kanban de Producción permite arrastrar una tarjeta directamente a
     * cualquier columna (no solo a la adyacente), por lo que un pedido puede
     * pasar de RECIBIDO a CONTROL o LISTO sin pisar nunca el valor literal
     * EN_PROCESO. Comparar solo con {@code == EN_PROCESO} deja ese salto sin
     * disparar el descuento de stock — por eso el gatillo usa este método en
     * vez de una igualdad exacta.
     */
    public boolean alcanzoProduccion() {
        return this == EN_PROCESO || this == CONTROL || this == LISTO || this == ENTREGADO;
    }
}
