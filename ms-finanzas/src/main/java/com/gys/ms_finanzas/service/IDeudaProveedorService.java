package com.gys.ms_finanzas.service;

import com.gys.ms_finanzas.dto.DeudaProveedorRequest;
import com.gys.ms_finanzas.dto.DeudaProveedorResponse;

import java.util.List;

public interface IDeudaProveedorService {
    List<DeudaProveedorResponse> listarPorProveedor(Long proveedorId);
    List<DeudaProveedorResponse> listarPendientes();
    DeudaProveedorResponse buscarPorId(Long id);
    DeudaProveedorResponse registrar(DeudaProveedorRequest request);
}
