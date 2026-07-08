package com.gs.ms_pedidos.repository;

import com.gs.ms_pedidos.model.EstadoPedido;
import com.gs.ms_pedidos.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    /**
     * Fecha del último pedido de cada odontólogo. Sirve para calcular su
     * "estado de actividad" sin necesidad de desactivarlos a mano.
     * Cada row: [0] odontologoId (Long), [1] última fecha de creación.
     */
    @Query("SELECT p.odontologoId, MAX(p.fechaCreacion) FROM Pedido p GROUP BY p.odontologoId")
    List<Object[]> ultimaActividadPorOdontologo();
}
