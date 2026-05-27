package com.gys.ms_catalogo.service;

import com.gys.ms_catalogo.dto.TipoTrabajoRequest;
import com.gys.ms_catalogo.dto.TipoTrabajoResponse;
import com.gys.ms_catalogo.model.Categoria;

import java.util.List;

public interface ITipoTrabajoService {
    List<TipoTrabajoResponse> listarActivos();
    List<TipoTrabajoResponse> listarPorCategoria(Categoria categoria);
    List<TipoTrabajoResponse> buscarPorNombre(String nombre);
    TipoTrabajoResponse buscarPorId(Long id);
    TipoTrabajoResponse crear(TipoTrabajoRequest request);
    TipoTrabajoResponse actualizar(Long id, TipoTrabajoRequest request);
    void eliminar(Long id);
}
