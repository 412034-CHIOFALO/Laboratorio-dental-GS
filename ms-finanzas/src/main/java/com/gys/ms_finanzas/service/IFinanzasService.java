package com.gys.ms_finanzas.service;

import com.gys.ms_finanzas.dto.ComprobanteRequest;
import com.gys.ms_finanzas.dto.ComprobanteResponse;
import com.gys.ms_finanzas.dto.CuentaCorrienteOdontologoResponse;

import java.math.BigDecimal;
import java.util.List;

public interface IFinanzasService {
    List<ComprobanteResponse> listarTodos();
    List<ComprobanteResponse> listarPendientes();
    List<ComprobanteResponse> listarPorOdontologo(Long odontologoId);
    BigDecimal saldoPendienteOdontologo(Long odontologoId);
    ComprobanteResponse buscarPorId(Long id);
    ComprobanteResponse emitir(ComprobanteRequest request);
    ComprobanteResponse registrarCobro(Long id);

    /**
     * Ranking de odontólogos con deuda pendiente, ordenado de mayor a menor.
     * Solo incluye los que tienen al menos un comprobante PENDIENTE.
     */
    List<CuentaCorrienteOdontologoResponse> rankingMorosos();
}
