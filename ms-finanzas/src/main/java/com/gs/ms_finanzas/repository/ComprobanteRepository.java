package com.gs.ms_finanzas.repository;

import com.gs.ms_finanzas.model.Comprobante;
import com.gs.ms_finanzas.model.EstadoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ComprobanteRepository extends JpaRepository<Comprobante, Long> {

    List<Comprobante> findByOdontologoId(Long odontologoId);

    List<Comprobante> findByEstadoPago(EstadoPago estadoPago);

    List<Comprobante> findByOdontologoIdAndEstadoPago(Long odontologoId, EstadoPago estadoPago);

    /** Suma total de cobros pendientes por odontólogo (cuenta corriente). */
    @Query("SELECT COALESCE(SUM(c.monto), 0) FROM Comprobante c WHERE c.odontologoId = :odontologoId AND c.estadoPago = 'PENDIENTE'")
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
               SUM(c.monto),
               COUNT(c),
               MIN(c.fechaEmision)
        FROM Comprobante c
        WHERE c.estadoPago = 'PENDIENTE'
        GROUP BY c.odontologoId
        ORDER BY SUM(c.monto) DESC
    """)
    List<Object[]> rankingDeudoresRaw();
}
