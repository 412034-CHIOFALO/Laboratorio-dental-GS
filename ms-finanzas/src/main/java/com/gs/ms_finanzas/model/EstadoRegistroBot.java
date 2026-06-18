package com.gs.ms_finanzas.model;

/**
 * Resultado del procesamiento de un comprobante por el bot.
 *  - REGISTRADO → se aplicó el pago (sueldo o proveedor).
 *  - RECHAZADO  → no se pudo resolver al receptor.
 *  - DUPLICADO  → el nro de operación ya había sido registrado.
 */
public enum EstadoRegistroBot {
    REGISTRADO,
    RECHAZADO,
    DUPLICADO
}
