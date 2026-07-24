package com.gs.ms_finanzas.repository;

import com.gs.ms_finanzas.model.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    List<Proveedor> findByActivoTrue();

    Optional<Proveedor> findByCuit(String cuit);

    boolean existsByCuit(String cuit);

    /** Usado por el seed inicial para no duplicar proveedores ya cargados. */
    boolean existsByNombreIgnoreCase(String nombre);
}
