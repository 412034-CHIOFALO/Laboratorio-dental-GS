package com.gs.ms_pedidos.model;

/**
 * Nivel de prioridad de un pedido.
 *
 * <ul>
 *   <li><b>NORMAL</b>: prioridad estándar, valor por defecto al crear un pedido.</li>
 *   <li><b>URGENTE</b>: el pedido debe completarse antes que los normales.
 *       Se resalta visualmente en el tablero kanban del laboratorio.</li>
 * </ul>
 */
public enum Prioridad {
    NORMAL,
    URGENTE
}
