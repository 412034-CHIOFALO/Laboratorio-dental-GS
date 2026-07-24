package com.gs.ms_finanzas.repository;

import com.gs.ms_finanzas.model.Comprobante;
import com.gs.ms_finanzas.model.EstadoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ComprobanteRepository extends JpaRepository<Comprobante, Long> {

    /** Usado para sincronizar el monto del comprobante si se corrige el precio del pedido ya entregado. */
    Optional<Comprobante> findByPedidoId(Long pedidoId);

    List<Comprobante> findByOdontologoId(Long odontologoId);

    List<Comprobante> findByEstadoPago(EstadoPago estadoPago);

    List<Comprobante> findByOdontologoIdAndEstadoPago(Long odontologoId, EstadoPago estadoPago);

    /** Comprobantes con saldo pendiente (PENDIENTE o PARCIAL) de un odontólogo. */
    List<Comprobante> findByOdontologoIdAndEstadoPagoIn(Long odontologoId, List<EstadoPago> estados);

    /** Saldo pendiente total de un odontólogo (cuenta corriente) = Σ(monto − pagado). */
    @Query("SELECT COALESCE(SUM(c.monto - c.montoPagado), 0) FROM Comprobante c " +
           "WHERE c.odontologoId = :odontologoId AND c.estadoPago IN ('PENDIENTE', 'PARCIAL')")
    BigDecimal sumMontosPendientesByOdontologo(Long odontologoId);

    /**
     * Ranking de morosos: una fila por odontólogo con saldo pendiente, cantidad
     * de comprobantes y fecha del comprobante más viejo (para calcular días
     * sin pagar).
     *
     * Proyectado como array de objetos por simplicidad (sin DTO en JPQL).
     * Estructura de cada row:
     *   [0] odontologoId    (Long)
     *   [1] odontologoNombre (String)
     *   [2] totalDeuda      (BigDecimal)
     *   [3] comprobantesPendientes (Long)
     *   [4] fechaMasAntigua (LocalDate)
     */
    @Query("""
        SELECT c.odontologoId,
               MAX(c.odontologoNombre),
               SUM(c.monto - c.montoPagado),
               COUNT(c),
               MIN(c.fechaEmision)
        FROM Comprobante c
        WHERE c.estadoPago IN ('PENDIENTE', 'PARCIAL')
        GROUP BY c.odontologoId
        ORDER BY SUM(c.monto - c.montoPagado) DESC
    """)
    List<Object[]> rankingDeudoresRaw();
}
