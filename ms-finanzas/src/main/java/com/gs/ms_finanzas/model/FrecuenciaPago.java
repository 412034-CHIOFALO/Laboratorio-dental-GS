package com.gs.ms_finanzas.model;

/**
 * Frecuencia con la que cobra un integrante del laboratorio.
 * Define el "ciclo" sobre el que se calcula el monto base.
 */
public enum FrecuenciaPago {
    DIARIO,     // cobra por día trabajado
    SEMANAL,    // una vez por semana
    QUINCENAL,  // cada 15 días
    MENSUAL     // una vez al mes
}
