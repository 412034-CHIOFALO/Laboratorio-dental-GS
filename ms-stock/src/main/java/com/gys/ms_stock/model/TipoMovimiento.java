package com.gys.ms_stock.model;

public enum TipoMovimiento {
    ENTRADA,   // Compra / reposición
    SALIDA,    // Consumo por pedido o uso interno
    AJUSTE     // Corrección manual de inventario
}
