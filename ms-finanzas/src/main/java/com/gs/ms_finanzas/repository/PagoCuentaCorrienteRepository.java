package com.gs.ms_finanzas.repository;

import com.gs.ms_finanzas.model.PagoCuentaCorriente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PagoCuentaCorrienteRepository extends JpaRepository<PagoCuentaCorriente, Long> {

    List<PagoCuentaCorriente> findByOdontologoIdOrderByFechaDescIdDesc(Long odontologoId);

    List<PagoCuentaCorriente> findAllByOrderByFechaDescIdDesc();
}
