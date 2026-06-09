package com.gys.ms_stock.service;

import com.gys.ms_stock.dto.MaterialRequest;
import com.gys.ms_stock.dto.MaterialResponse;
import com.gys.ms_stock.dto.MovimientoRequest;

import java.util.List;

public interface IStockService {
    List<MaterialResponse> listarActivos();
    List<MaterialResponse> listarBajoStock();
    MaterialResponse buscarPorId(Long id);
    MaterialResponse crear(MaterialRequest request);
    MaterialResponse actualizar(Long id, MaterialRequest request);
    MaterialResponse registrarMovimiento(MovimientoRequest request);
    void eliminar(Long id);
}
