package com.gs.ms_pedidos.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests del cálculo de días hábiles (base del "pedido atrasado").
 * 2024-01-01 es lunes; lo usamos de ancla para las fechas.
 */
class DiasHabilesTest {

    @Test
    @DisplayName("Lunes a viernes de la misma semana = 4 días hábiles")
    void lunesAViernes() {
        assertEquals(4, DiasHabiles.entre(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 5)));
    }

    @Test
    @DisplayName("Viernes a lunes salta el fin de semana = 1 día hábil")
    void viernesALunes() {
        assertEquals(1, DiasHabiles.entre(LocalDate.of(2024, 1, 5), LocalDate.of(2024, 1, 8)));
    }

    @Test
    @DisplayName("Lunes al lunes siguiente = 5 días hábiles")
    void lunesALunesSiguiente() {
        assertEquals(5, DiasHabiles.entre(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 8)));
    }

    @Test
    @DisplayName("Umbral de atraso: 6 días hábiles (lunes al martes siguiente)")
    void umbralDeAtraso() {
        assertEquals(6, DiasHabiles.entre(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 9)));
    }

    @Test
    @DisplayName("Mismo día = 0")
    void mismoDia() {
        assertEquals(0, DiasHabiles.entre(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1)));
    }

    @Test
    @DisplayName("Fecha nula = 0 (defensivo)")
    void fechaNula() {
        assertEquals(0, DiasHabiles.entre((LocalDate) null, LocalDate.of(2024, 1, 1)));
    }
}
