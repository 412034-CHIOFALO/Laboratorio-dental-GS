package com.gs.ms_stock.repository;

import com.gs.ms_stock.model.CategoriaMaterial;
import com.gs.ms_stock.model.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Long> {

    List<Material> findByActivoTrue();

    List<Material> findByCategoriaAndActivoTrue(CategoriaMaterial categoria);

    /** Materiales cuyo stock actual es menor o igual al mínimo. */
    @Query("SELECT m FROM Material m WHERE m.activo = true AND m.stockActual <= m.stockMinimo")
    List<Material> findBajoStock();
}
