package com.gs.ms_catalogo.repository;

import com.gs.ms_catalogo.model.Categoria;
import com.gs.ms_catalogo.model.TipoTrabajo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TipoTrabajoRepository extends JpaRepository<TipoTrabajo, Long> {

    List<TipoTrabajo> findByActivoTrue();

    List<TipoTrabajo> findByCategoriaAndActivoTrue(Categoria categoria);

    List<TipoTrabajo> findByNombreContainingIgnoreCaseAndActivoTrue(String nombre);
}
