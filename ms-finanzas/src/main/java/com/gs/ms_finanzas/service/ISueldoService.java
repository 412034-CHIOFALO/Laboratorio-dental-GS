package com.gs.ms_finanzas.service;

import com.gs.ms_finanzas.dto.SueldoRequest;
import com.gs.ms_finanzas.dto.SueldoResponse;

import java.util.List;

/**
 * Servicio legacy para la gestión de sueldos mensuales devengados por empleados.
 *
 * <p>Opera sobre la entidad {@link com.gs.ms_finanzas.model.SueldoEmpleado},
 * que representa el haber mensual generado por cada empleado en un período determinado.
 * Este servicio es complementario al {@link IGestionSueldoService}, que maneja
 * la configuración de sueldos, el devengado acumulado y los pagos con cascada.</p>
 */
public interface ISueldoService {

    /**
     * Lista todos los sueldos (PENDIENTE y PAGADO) de un mes/año específico.
     *
     * @param anio año del período (ej: 2024).
     * @param mes  mes del período (1 = enero ... 12 = diciembre).
     * @return lista de sueldos del período; vacía si no hay registros.
     */
    List<SueldoResponse> listarPorMes(int anio, int mes);

    /**
     * Lista únicamente los sueldos en estado PENDIENTE de un mes/año específico.
     *
     * <p>Útil para la pantalla de "Pagos pendientes del mes" donde el administrador
     * controla qué sueldos aún no fueron pagados.</p>
     *
     * @param anio año del período (ej: 2024).
     * @param mes  mes del período (1 = enero ... 12 = diciembre).
     * @return lista de sueldos pendientes del período; vacía si todos fueron pagados.
     */
    List<SueldoResponse> listarPendientesMes(int anio, int mes);

    /**
     * Registra el sueldo mensual devengado por un empleado.
     *
     * <p>El estado inicial es {@code PENDIENTE}. Se valida que no exista un registro
     * duplicado para el mismo empleado y período.</p>
     *
     * @param request datos del sueldo (empleado, monto, año, mes).
     * @return el sueldo creado con su ID asignado.
     * @throws com.gs.ms_finanzas.exception.ConflictException si ya existe un registro para ese empleado y período.
     */
    SueldoResponse registrar(SueldoRequest request);
}
