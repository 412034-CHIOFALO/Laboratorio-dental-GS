package com.gs.ms_finanzas.service;

import com.gs.ms_finanzas.dto.DeudaProveedorRequest;
import com.gs.ms_finanzas.dto.DeudaProveedorResponse;

import java.util.List;

/**
 * Servicio de gestión de deudas del laboratorio con sus proveedores de materiales.
 *
 * <p>Las deudas se generan cuando el laboratorio compra materiales dentales
 * a un proveedor y no paga al contado. Pueden cancelarse manualmente desde la app
 * o ser detectadas por el bot de WhatsApp cuando alguien del grupo comparte
 * un comprobante de pago al proveedor.</p>
 */
public interface IDeudaProveedorService {

    /**
     * Lista todas las deudas asociadas a un proveedor específico.
     *
     * @param proveedorId ID interno del proveedor.
     * @return lista de deudas (PENDIENTE y PAGADO) del proveedor; vacía si no tiene ninguna.
     * @throws com.gs.ms_finanzas.exception.ResourceNotFoundException si el proveedor no existe.
     */
    List<DeudaProveedorResponse> listarPorProveedor(Long proveedorId);

    /**
     * Lista todas las deudas en estado PENDIENTE de todos los proveedores.
     *
     * <p>Útil para la pantalla de "Pagos a realizar" donde el administrador
     * prioriza qué deudas cancelar con los fondos disponibles.</p>
     *
     * @return lista de deudas pendientes de todos los proveedores; vacía si no hay ninguna.
     */
    List<DeudaProveedorResponse> listarPendientes();

    /**
     * Busca una deuda por su ID interno.
     *
     * @param id ID de la deuda.
     * @return la deuda encontrada.
     * @throws com.gs.ms_finanzas.exception.ResourceNotFoundException si no existe.
     */
    DeudaProveedorResponse buscarPorId(Long id);

    /**
     * Registra una nueva deuda con un proveedor por compra de materiales.
     *
     * <p>El estado inicial de la deuda es {@code PENDIENTE}.
     * El proveedor indicado debe existir y estar activo.</p>
     *
     * @param request datos de la deuda (proveedor, descripción, monto, vencimiento, nro factura).
     * @return la deuda creada con su ID asignado.
     * @throws com.gs.ms_finanzas.exception.ResourceNotFoundException si el proveedor no existe.
     * @throws com.gs.ms_finanzas.exception.BusinessException si el proveedor está inactivo.
     */
    DeudaProveedorResponse registrar(DeudaProveedorRequest request);
}
