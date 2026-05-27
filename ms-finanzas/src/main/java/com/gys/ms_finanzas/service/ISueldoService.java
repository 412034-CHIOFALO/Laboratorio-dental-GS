package com.gys.ms_finanzas.service;

import com.gys.ms_finanzas.dto.SueldoRequest;
import com.gys.ms_finanzas.dto.SueldoResponse;

import java.util.List;

public interface ISueldoService {
    List<SueldoResponse> listarPorMes(int anio, int mes);
    List<SueldoResponse> listarPendientesMes(int anio, int mes);
    SueldoResponse registrar(SueldoRequest request);
}
