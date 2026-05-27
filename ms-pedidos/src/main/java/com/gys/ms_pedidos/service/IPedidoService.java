package com.gys.ms_pedidos.service;

import com.gys.ms_pedidos.dto.PedidoRequest;
import com.gys.ms_pedidos.dto.PedidoResponse;
import com.gys.ms_pedidos.model.EstadoPedido;

import java.util.List;

public interface IPedidoService {
    List<PedidoResponse> listarTodos();
    List<PedidoResponse> listarActivos();
    List<PedidoResponse> listarPorEstado(EstadoPedido estado);
    PedidoResponse buscarPorId(Long id);
    PedidoResponse crear(PedidoRequest request);
    PedidoResponse actualizar(Long id, PedidoRequest request);
    PedidoResponse actualizarEstado(Long id, EstadoPedido nuevoEstado);
    void eliminar(Long id);
}
