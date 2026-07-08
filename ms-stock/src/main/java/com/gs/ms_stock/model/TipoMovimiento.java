package com.gs.ms_stock.model;

/**
 * Tipos de movimiento que pueden afectar el stock de un {@link Material}.
 *
 * <p>Cada tipo determina cómo se calcula el nuevo {@code stockActual}:</p>
 * <ul>
 *   <li>{@link #ENTRADA}: {@code stockActual += cantidad}</li>
 *   <li>{@link #SALIDA}: {@code stockActual -= cantidad}</li>
 *   <li>{@link #AJUSTE}: {@code stockActual = cantidad} (reemplazo absoluto)</li>
 * </ul>
 */
public enum TipoMovimiento {

    /**
     * Ingreso de mercadería al inventario.
     * Corresponde a compras a proveedores, devoluciones o reposiciones internas.
     * Incrementa el {@code stockActual} del material.
     */
    ENTRADA,

    /**
     * Egreso de material del inventario.
     * Se origina por consumo en la producción de un pedido o uso interno del laboratorio.
     * Cuando proviene de un pedido, el campo {@code pedidoId} en {@link MovimientoStock}
     * debe estar informado para mantener la trazabilidad.
     * Decrementa el {@code stockActual} del material.
     */
    SALIDA,

    /**
     * Corrección manual del inventario.
     * Se usa al realizar un recuento físico que no coincide con el stock del sistema.
     * A diferencia de {@code ENTRADA} y {@code SALIDA}, este tipo reemplaza el
     * {@code stockActual} con el valor indicado en {@code cantidad} (no es un delta).
     */
    AJUSTE
}
