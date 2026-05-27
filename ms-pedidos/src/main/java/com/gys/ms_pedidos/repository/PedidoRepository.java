package com.gys.ms_pedidos.repository;

import com.gys.ms_pedidos.model.EstadoPedido;
import com.gys.ms_pedidos.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByEstado(EstadoPedido estado);

    List<Pedido> findByOdontologoId(Long odontologoId);

    List<Pedido> findByTecnicoId(Long tecnicoId);

    List<Pedido> findByEstadoNot(EstadoPedido estado);

    Optional<Pedido> findByNroPedido(String nroPedido);

    boolean existsByNroPedido(String nroPedido);
}
