package com.gys.ms_finanzas.repository;

import com.gys.ms_finanzas.model.Comprobante;
import com.gys.ms_finanzas.model.EstadoPago;
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
}
