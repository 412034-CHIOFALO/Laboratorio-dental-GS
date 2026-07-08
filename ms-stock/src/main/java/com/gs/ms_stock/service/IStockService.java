package com.gs.ms_stock.service;

import com.gs.ms_stock.dto.MaterialRequest;
import com.gs.ms_stock.dto.MaterialResponse;
import com.gs.ms_stock.dto.MovimientoRequest;

import java.util.List;

/**
 * Contrato de negocio para la gestión del inventario de materiales del laboratorio dental G&amp;S.
 *
 * <p>Define las operaciones CRUD sobre materiales y el registro de movimientos de stock
 * (entradas, salidas y ajustes). La implementación principal es {@link StockService}.</p>
 *
 * <p>Las salidas de stock por pedidos son iniciadas por ms-pedidos, que llama a
 * {@link #registrarMovimiento(MovimientoRequest)} con {@code tipo = SALIDA} y el {@code pedidoId}
 * correspondiente.</p>
 */
public interface IStockService {

    /**
     * Devuelve todos los materiales en estado activo ({@code activo = true}).
     *
     * <p>Incluye el campo calculado {@code bajoStock}: {@code true} cuando
     * {@code stockActual <= stockMinimo}.</p>
     *
     * @return lista (nunca {@code null}, puede estar vacía) de materiales activos
     */
    List<MaterialResponse> listarActivos();

    /**
     * Devuelve los materiales activos cuyo stock actual está en o por debajo del mínimo.
     *
     * <p>Se usa para mostrar alertas de reposición en el panel de administración.</p>
     *
     * @return lista (nunca {@code null}, puede estar vacía) de materiales con bajo stock
     */
    List<MaterialResponse> listarBajoStock();

    /**
     * Busca un material por su identificador primario.
     *
     * @param id identificador único del material
     * @return DTO con los datos del material
     * @throws com.gs.ms_stock.exception.ResourceNotFoundException si no existe un material con ese ID
     */
    MaterialResponse buscarPorId(Long id);

    /**
     * Crea y persiste un nuevo material en el inventario.
     *
     * <p>El material se crea en estado activo. El campo {@code descuentaStock} es
     * {@code true} por defecto si no se envía en el request.</p>
     *
     * @param request datos del nuevo material (nombre, categoría, stock, unidad de medida, etc.)
     * @return DTO del material recién creado con su ID asignado
     * @throws jakarta.validation.ConstraintViolationException si el request no pasa la validación
     */
    MaterialResponse crear(MaterialRequest request);

    /**
     * Actualiza todos los campos de un material existente.
     *
     * <p>Se reemplazan todos los campos editables; los no provistos deben igualmente
     * enviarse (semántica PUT).</p>
     *
     * @param id      identificador del material a actualizar
     * @param request nuevos datos del material
     * @return DTO del material con los datos actualizados
     * @throws com.gs.ms_stock.exception.ResourceNotFoundException si no existe un material con ese ID
     * @throws jakarta.validation.ConstraintViolationException     si el request no pasa la validación
     */
    MaterialResponse actualizar(Long id, MaterialRequest request);

    /**
     * Registra un movimiento de stock y actualiza el {@code stockActual} del material.
     *
     * <p>Lógica según el tipo de movimiento:</p>
     * <ul>
     *   <li>{@code ENTRADA}: {@code stockActual += cantidad}</li>
     *   <li>{@code SALIDA}: {@code stockActual -= cantidad} (puede resultar negativo;
     *       se emite advertencia en log pero no se lanza excepción)</li>
     *   <li>{@code AJUSTE}: {@code stockActual = cantidad} (reemplazo absoluto)</li>
     * </ul>
     *
     * @param request datos del movimiento (materialId, tipo, cantidad, motivo, pedidoId opcional)
     * @return DTO del material con el stock ya actualizado
     * @throws com.gs.ms_stock.exception.ResourceNotFoundException si no existe el material indicado
     * @throws jakarta.validation.ConstraintViolationException     si la cantidad es nula o no positiva
     */
    MaterialResponse registrarMovimiento(MovimientoRequest request);

    /**
     * Realiza la baja lógica de un material (lo marca como inactivo).
     *
     * <p>El registro no se elimina de la base de datos; se pone {@code activo = false}
     * para preservar el historial de movimientos y referencias desde pedidos históricos.</p>
     *
     * @param id identificador del material a dar de baja
     * @throws com.gs.ms_stock.exception.ResourceNotFoundException si no existe un material con ese ID
     */
    void eliminar(Long id);
}
