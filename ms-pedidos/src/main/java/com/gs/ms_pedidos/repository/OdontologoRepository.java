package com.gs.ms_pedidos.repository;

import com.gs.ms_pedidos.model.Odontologo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OdontologoRepository extends JpaRepository<Odontologo, Long> {

    /** Listado completo de activos para selectores. */
    List<Odontologo> findByActivoTrueOrderByNombreAsc();

    /** Listado completo, activos + inactivos — solo para el panel de gestión. */
    List<Odontologo> findAllByOrderByNombreAsc();

    /** Autocomplete por fragmento de nombre (case-insensitive). */
    List<Odontologo> findByActivoTrueAndNombreContainingIgnoreCaseOrderByNombreAsc(String fragmento);

    /** Match exacto por nombre normalizado — usado por el patrón find-or-create. */
    Optional<Odontologo> findByActivoTrueAndNombreIgnoreCase(String nombre);

    /** Búsqueda exacta por documentos únicos. */
    Optional<Odontologo> findByActivoTrueAndDni(String dni);
    Optional<Odontologo> findByActivoTrueAndCuit(String cuit);
    Optional<Odontologo> findByActivoTrueAndMatriculaIgnoreCase(String matricula);

    /** Validación de unicidad al crear. */
    boolean existsByDni(String dni);
    boolean existsByCuit(String cuit);
}
