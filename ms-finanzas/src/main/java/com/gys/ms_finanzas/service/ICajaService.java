package com.gys.ms_finanzas.service;

import com.gys.ms_finanzas.dto.CajaMovimientoResponse;
import com.gys.ms_finanzas.dto.ResumenCajasResponse;
import com.gys.ms_finanzas.model.TipoCaja;

import java.time.LocalDate;
import java.util.List;

public interface ICajaService {
    ResumenCajasResponse obtenerResumen();
    List<CajaMovimientoResponse> listarMovimientosByCaja(TipoCaja tipoCaja);
    List<CajaMovimientoResponse> listarMovimientosByPeriodo(LocalDate desde, LocalDate hasta);
}
