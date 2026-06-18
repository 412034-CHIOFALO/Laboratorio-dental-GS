package com.gs.ms_finanzas.repository;

import com.gs.ms_finanzas.model.EstadoRegistroBot;
import com.gs.ms_finanzas.model.RegistroPagoBot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegistroPagoBotRepository extends JpaRepository<RegistroPagoBot, Long> {

    List<RegistroPagoBot> findAllByOrderByFechaHoraDesc();

    /** Anti-duplicado: ya existe un registro EXITOSO con ese nro de operación. */
    boolean existsByIdOperacionAndEstado(String idOperacion, EstadoRegistroBot estado);
}
