package com.gys.ms_finanzas.service;

import com.gys.ms_finanzas.dto.ComprobanteRequest;
import com.gys.ms_finanzas.dto.ComprobanteResponse;

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
}
