package com.gys.ms_pedidos.repository;

import com.gys.ms_pedidos.model.Odontologo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OdontologoRepository extends JpaRepository<Odontologo, Long> {

    /** Listado completo de activos para selectores. */
    List<Odontologo> findByActivoTrueOrderByNombreAsc();

    /** Autocomplete por fragmento de nombre (case-insensitive). */
    List<Odontologo> findByActivoTrueAndNombreContainingIgnoreCaseOrderByNombreAsc(String fragmento);

    /** Match exacto por nombre normalizado — usado por el patrón find-or-create. */
    Optional<Odontologo> findByActivoTrueAndNombreIgnoreCase(String nombre);
}
