package com.gs.ms_catalogo.service;

import com.gs.ms_catalogo.dto.TipoTrabajoRequest;
import com.gs.ms_catalogo.dto.TipoTrabajoResponse;
import com.gs.ms_catalogo.model.Categoria;

import java.util.List;

/**
 * Contrato de la capa de servicio para la gestión del catálogo de tipos de trabajo dental.
 * <p>
 * Define las operaciones disponibles sobre {@link com.gs.ms_catalogo.model.TipoTrabajo}:
 * consultas de listado, búsquedas, alta, modificación y baja lógica.
 * La implementación concreta es {@link TipoTrabajoService}.
 * </p>
 */
public interface ITipoTrabajoService {

    /**
     * Devuelve todos los tipos de trabajo cuyo campo {@code activo} es {@code true}.
     * <p>
     * Es el listado general del catálogo que se muestra en la UI al consultar
     * sin filtros. Los trabajos dados de baja no aparecen.
     * </p>
     *
     * @return lista (posiblemente vacía) de trabajos activos
     */
    List<TipoTrabajoResponse> listarActivos();

    /**
     * Filtra los tipos de trabajo activos por su categoría odontológica.
     *
     * @param categoria categoría por la que filtrar (FIJA, REMOVIBLE, ORTODONCIA, ATM, PERSONALIZADO)
     * @return lista (posiblemente vacía) de trabajos activos de esa categoría
     */
    List<TipoTrabajoResponse> listarPorCategoria(Categoria categoria);

    /**
     * Busca tipos de trabajo activos cuyo nombre contenga el fragmento indicado
     * (búsqueda parcial, sin distinción de mayúsculas/minúsculas).
     *
     * @param nombre fragmento de texto a buscar en el nombre del trabajo
     * @return lista (posiblemente vacía) de coincidencias activas
     */
    List<TipoTrabajoResponse> buscarPorNombre(String nombre);

    /**
     * Recupera un tipo de trabajo específico por su ID.
     *
     * @param id identificador único del tipo de trabajo
     * @return DTO con todos los datos del trabajo, incluida su receta de materiales
     * @throws com.gs.ms_catalogo.exception.ResourceNotFoundException si no existe un trabajo con ese ID
     */
    TipoTrabajoResponse buscarPorId(Long id);

    /**
     * Crea un nuevo tipo de trabajo dental con su receta de materiales.
     * <p>
     * El nombre debe ser único. Si ya existe un trabajo (activo o no) con el mismo nombre,
     * se lanza {@link com.gs.ms_catalogo.exception.ConflictException}.
     * </p>
     *
     * @param request datos del nuevo trabajo (nombre, precio, categoría, receta, etc.)
     * @return DTO del trabajo recién creado, con el ID asignado por la base de datos
     * @throws com.gs.ms_catalogo.exception.ConflictException si el nombre ya está en uso
     */
    TipoTrabajoResponse crear(TipoTrabajoRequest request);

    /**
     * Actualiza completamente un tipo de trabajo existente y reemplaza su receta.
     * <p>
     * La receta anterior es eliminada en su totalidad y sustituida por la que viene
     * en {@code request}. Si {@code request.getReceta()} está vacía, el trabajo
     * queda sin receta de materiales.
     * </p>
     *
     * @param id      ID del trabajo a modificar
     * @param request nuevos datos del trabajo
     * @return DTO del trabajo actualizado
     * @throws com.gs.ms_catalogo.exception.ResourceNotFoundException si no existe trabajo con ese ID
     * @throws com.gs.ms_catalogo.exception.ConflictException         si el nuevo nombre ya lo usa otro trabajo
     */
    TipoTrabajoResponse actualizar(Long id, TipoTrabajoRequest request);

    /**
     * Realiza una baja lógica del tipo de trabajo: pone {@code activo = false}.
     * <p>
     * El registro permanece en la base de datos para mantener la trazabilidad de
     * órdenes y presupuestos históricos que referencian este trabajo.
     * </p>
     *
     * @param id ID del trabajo a dar de baja
     * @throws com.gs.ms_catalogo.exception.ResourceNotFoundException si no existe trabajo con ese ID
     */
    void eliminar(Long id);
}
