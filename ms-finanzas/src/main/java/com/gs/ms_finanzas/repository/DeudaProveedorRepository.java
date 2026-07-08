package com.gs.ms_finanzas.repository;

import com.gs.ms_finanzas.model.DeudaProveedor;
import com.gs.ms_finanzas.model.EstadoDeuda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface DeudaProveedorRepository extends JpaRepository<DeudaProveedor, Long> {

    List<DeudaProveedor> findByProveedorIdOrderByFechaCreacionDesc(Long proveedorId);

    List<DeudaProveedor> findByEstadoOrderByFechaVencimientoAsc(EstadoDeuda estado);

    @Query("SELECT COALESCE(SUM(d.monto), 0) FROM DeudaProveedor d WHERE d.proveedor.id = :proveedorId AND d.estado = 'PENDIENTE'")
    BigDecimal sumDeudaPendienteByProveedor(Long proveedorId);

    @Query("SELECT COALESCE(SUM(d.monto), 0) FROM DeudaProveedor d WHERE d.estado = 'PENDIENTE'")
    BigDecimal sumTotalDeudaPendiente();
}
