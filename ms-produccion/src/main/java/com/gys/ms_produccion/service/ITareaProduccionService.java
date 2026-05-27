package com.gys.ms_produccion.service;

import com.gys.ms_produccion.dto.TareaRequest;
import com.gys.ms_produccion.dto.TareaResponse;
import com.gys.ms_produccion.model.EstadoTarea;

import java.util.List;

public interface ITareaProduccionService {
    List<TareaResponse> listarActivas();
    List<TareaResponse> listarPorEstado(EstadoTarea estado);
    TareaResponse buscarPorId(Long id);
    TareaResponse crear(TareaRequest request);
    TareaResponse actualizarEstado(Long id, EstadoTarea nuevoEstado);
    TareaResponse asignarTecnico(Long id, String tecnico);
}
