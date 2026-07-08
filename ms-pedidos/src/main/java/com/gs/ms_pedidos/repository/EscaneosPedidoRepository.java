package com.gs.ms_pedidos.repository;

import com.gs.ms_pedidos.model.EscaneosPedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EscaneosPedidoRepository extends JpaRepository<EscaneosPedido, Long> {
    List<EscaneosPedido> findByPedidoIdOrderByFechaSubidaDesc(Long pedidoId);
    long countByPedidoId(Long pedidoId);
}
