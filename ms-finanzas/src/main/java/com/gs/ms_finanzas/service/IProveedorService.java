package com.gs.ms_finanzas.service;

import com.gs.ms_finanzas.dto.ProveedorRequest;
import com.gs.ms_finanzas.dto.ProveedorResponse;

import java.util.List;

/**
 * Servicio de ABM de proveedores de materiales dentales del laboratorio.
 *
 * <p>Los proveedores no se eliminan físicamente del sistema; se dan de baja lógica
 * ({@code activo = false}) para preservar el historial de deudas y pagos.
 * Solo los proveedores activos aparecen en el ABM y en la lista de receptores
 * que el bot de WhatsApp puede identificar.</p>
 */
public interface IProveedorService {

    /**
     * Devuelve todos los proveedores activos ({@code activo = true}).
     *
     * @return lista de proveedores activos; vacía si no hay ninguno.
     */
    List<ProveedorResponse> listarActivos();

    /**
     * Busca un proveedor por su ID interno, activo o no.
     *
     * @param id ID interno del proveedor.
     * @return los datos del proveedor.
     * @throws com.gs.ms_finanzas.exception.ResourceNotFoundException si no existe.
     */
    ProveedorResponse buscarPorId(Long id);

    /**
     * Crea un nuevo proveedor de materiales.
     *
     * <p>El proveedor queda activo por defecto. Se valida unicidad de CUIT si se provee.</p>
     *
     * @param request datos del proveedor (nombre, CUIT, email, teléfono, dirección).
     * @return el proveedor creado con su ID asignado.
     * @throws com.gs.ms_finanzas.exception.ConflictException si ya existe un proveedor con ese CUIT.
     */
    ProveedorResponse crear(ProveedorRequest request);

    /**
     * Actualiza los datos de contacto de un proveedor existente.
     *
     * @param id      ID del proveedor a actualizar.
     * @param request nuevos datos del proveedor.
     * @return el proveedor actualizado.
     * @throws com.gs.ms_finanzas.exception.ResourceNotFoundException si el proveedor no existe.
     */
    ProveedorResponse actualizar(Long id, ProveedorRequest request);

    /**
     * Realiza la baja lógica de un proveedor (marca {@code activo = false}).
     *
     * <p>El proveedor deja de aparecer en listados y el bot ya no lo reconoce
     * como receptor válido, pero su historial de deudas se conserva.</p>
     *
     * @param id ID del proveedor a desactivar.
     * @throws com.gs.ms_finanzas.exception.ResourceNotFoundException si el proveedor no existe.
     */
    void desactivar(Long id);
}
