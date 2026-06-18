package com.gs.ms_auth.repository;

import com.gs.ms_auth.model.AuditoriaEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditoriaEventoRepository extends JpaRepository<AuditoriaEvento, Long> {
    List<AuditoriaEvento> findAllByOrderByTimestampDesc();
}
