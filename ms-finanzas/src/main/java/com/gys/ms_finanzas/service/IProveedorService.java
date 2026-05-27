package com.gys.ms_finanzas.service;

import com.gys.ms_finanzas.dto.ProveedorRequest;
import com.gys.ms_finanzas.dto.ProveedorResponse;

import java.util.List;

public interface IProveedorService {
    List<ProveedorResponse> listarActivos();
    ProveedorResponse buscarPorId(Long id);
    ProveedorResponse crear(ProveedorRequest request);
    ProveedorResponse actualizar(Long id, ProveedorRequest request);
    void desactivar(Long id);
}
