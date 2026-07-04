package com.gs.ms_finanzas.repository;

import com.gs.ms_finanzas.model.ReporteMensual;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReporteMensualRepository extends JpaRepository<ReporteMensual, Long> {

    /** Reporte de un mes puntual (hay a lo sumo uno por año/mes). */
    Optional<ReporteMensual> findByAnioAndMes(int anio, int mes);

    /** Todos los reportes, del más reciente al más antiguo. */
    List<ReporteMensual> findAllByOrderByAnioDescMesDesc();
}
