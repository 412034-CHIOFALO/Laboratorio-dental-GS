package com.gs.ms_finanzas.repository;

import com.gs.ms_finanzas.model.PagoSueldo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PagoSueldoRepository extends JpaRepository<PagoSueldo, Long> {

    /** Histórico de pagos de un empleado, más recientes primero. */
    List<PagoSueldo> findByEmpleadoIdOrderByFechaDescIdDesc(Long empleadoId);

    /** Histórico global de pagos, más recientes primero. */
    List<PagoSueldo> findAllByOrderByFechaDescIdDesc();

    /** Anti-duplicado: ¿ya se registró un pago con este nro de operación? */
    boolean existsByIdOperacion(String idOperacion);
}
