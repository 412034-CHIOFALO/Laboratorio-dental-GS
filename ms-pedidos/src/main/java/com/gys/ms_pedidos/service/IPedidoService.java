package com.gys.ms_pedidos.service;

import com.gys.ms_pedidos.dto.EntregaRequest;
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
    /** Transición LISTO → ENTREGADO con datos de quién retiró y observaciones. */
    PedidoResponse marcarEntregado(Long id, EntregaRequest request);
    void eliminar(Long id);

    /** Lista todos los pedidos atrasados (no entregados, no cancelados, >= umbral configurado). */
    List<PedidoResponse> listarAtrasados();
}
