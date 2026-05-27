package com.gys.ms_produccion.repository;

import com.gys.ms_produccion.model.EstadoTarea;
import com.gys.ms_produccion.model.TareaProduccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TareaProduccionRepository extends JpaRepository<TareaProduccion, Long> {

    List<TareaProduccion> findByActivoTrueOrderByPrioridadDescFechaEntregaEstimadaAsc();

    List<TareaProduccion> findByEstadoAndActivoTrueOrderByPrioridadDescFechaEntregaEstimadaAsc(EstadoTarea estado);

    List<TareaProduccion> findByTecnicoNombreAndActivoTrue(String tecnicoNombre);
}
