package com.gs.ms_stock.repository;

import com.gs.ms_stock.model.ConfiguracionAlerta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfiguracionAlertaRepository extends JpaRepository<ConfiguracionAlerta, Long> {}
