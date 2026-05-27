package com.gys.ms_finanzas.repository;

import com.gys.ms_finanzas.model.EstadoSueldo;
import com.gys.ms_finanzas.model.SueldoEmpleado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SueldoEmpleadoRepository extends JpaRepository<SueldoEmpleado, Long> {

    List<SueldoEmpleado> findByAnioAndMesOrderByEmpleadoNombreAsc(int anio, int mes);

    List<SueldoEmpleado> findByAnioAndMesAndEstadoOrderByEmpleadoNombreAsc(int anio, int mes, EstadoSueldo estado);

    boolean existsByEmpleadoIdAndAnioAndMes(Long empleadoId, int anio, int mes);
}
