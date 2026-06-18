package com.gs.ms_finanzas.dto;

import com.gs.ms_finanzas.model.ManejoSobrante;
import com.gs.ms_finanzas.model.OrigenPago;
import com.gs.ms_finanzas.model.PagoSueldo;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Item del histórico de pagos de sueldo. */
public record PagoSueldoResponse(
        Long id,
        Long empleadoId,
        String empleadoNombre,
        BigDecimal monto,
        LocalDate fecha,
        OrigenPago origen,
        ManejoSobrante manejoSobrante,
        BigDecimal montoExcedente,
        String nota,
        // ── Datos del bot ──
        String cargadoPorNombre,
        String emisor,
        String comprobanteUrl,
        String grupoOrigen
) {
    public static PagoSueldoResponse from(PagoSueldo p) {
        return new PagoSueldoResponse(
                p.getId(),
                p.getEmpleadoId(),
                p.getEmpleadoNombre(),
                p.getMonto(),
                p.getFecha(),
                p.getOrigen(),
                p.getManejoSobrante(),
                p.getMontoExcedente(),
                p.getNota(),
                p.getCargadoPorNombre(),
                p.getEmisor(),
                p.getComprobanteUrl(),
                p.getGrupoOrigen()
        );
    }
}
