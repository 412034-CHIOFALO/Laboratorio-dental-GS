package com.gs.ms_pedidos.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Cálculo de días hábiles entre dos fechas.
 *
 * "Día hábil" = lunes a viernes. NO contempla feriados nacionales/provinciales
 * en esta primera versión; si en el futuro hace falta, se puede agregar una
 * lista de fechas a excluir (probablemente desde una tabla en BD).
 *
 * Uso típico:
 *
 *   int dias = DiasHabiles.entre(pedido.getFechaCreacion(), LocalDateTime.now());
 *   if (dias >= 6) pedido.atrasado = true;
 */
public final class DiasHabiles {

    private DiasHabiles() { /* utility class — no instanciar */ }

    /**
     * Cuenta días hábiles (Lun-Vie) entre dos fechas. La fecha de inicio
     * NO se cuenta; la de fin SÍ si cae en día hábil.
     *
     * Ejemplo: lunes 0 → viernes mismo semana = 4 días hábiles transcurridos.
     */
    public static int entre(LocalDate inicio, LocalDate fin) {
        if (inicio == null || fin == null) return 0;
        if (!fin.isAfter(inicio)) return 0;

        int diasHabiles = 0;
        LocalDate cursor = inicio.plusDays(1); // arrancamos al día siguiente
        while (!cursor.isAfter(fin)) {
            DayOfWeek dow = cursor.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                diasHabiles++;
            }
            cursor = cursor.plusDays(1);
        }
        return diasHabiles;
    }

    /** Overload que recibe LocalDateTime y descarta la hora. */
    public static int entre(LocalDateTime inicio, LocalDateTime fin) {
        if (inicio == null || fin == null) return 0;
        return entre(inicio.toLocalDate(), fin.toLocalDate());
    }

    /** Conveniencia: días hábiles transcurridos desde una fecha hasta hoy. */
    public static int desdeHasta(LocalDateTime inicio) {
        if (inicio == null) return 0;
        return entre(inicio.toLocalDate(), LocalDate.now());
    }
}
