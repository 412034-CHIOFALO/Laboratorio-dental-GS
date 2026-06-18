package com.gs.ms_finanzas.service;

import com.gs.ms_finanzas.dto.CajaMovimientoRequest;
import com.gs.ms_finanzas.dto.CajaMovimientoResponse;
import com.gs.ms_finanzas.dto.ResumenCajasResponse;
import com.gs.ms_finanzas.model.TipoCaja;

import java.time.LocalDate;
import java.util.List;

/**
 * Servicio de gestión de las tres cajas del laboratorio dental.
 *
 * <p>Cada caja representa un tipo de fondo diferente:
 * <ul>
 *   <li><b>FISICA</b>: efectivo disponible en el laboratorio.</li>
 *   <li><b>BANCARIA</b>: saldo en cuenta bancaria.</li>
 *   <li><b>COMPENSACION</b>: fondo para pagos triangulados (odontólogos que pagan
 *       a proveedores en nombre del laboratorio).</li>
 * </ul>
 * Los saldos se calculan en tiempo real sumando todos los movimientos de cada caja.</p>
 */
public interface ICajaService {

    /**
     * Obtiene el resumen financiero consolidado de las tres cajas.
     *
     * <p>El resumen incluye:
     * <ul>
     *   <li>Saldo actual de cada caja (FISICA, BANCARIA, COMPENSACION).</li>
     *   <li>Total de deuda pendiente a proveedores.</li>
     *   <li>Total de sueldos devengados pendientes de pago.</li>
     *   <li>Alertas o indicadores de salud financiera.</li>
     * </ul></p>
     *
     * @return resumen con saldos y totales consolidados.
     */
    ResumenCajasResponse obtenerResumen();

    /**
     * Lista todos los movimientos de una caja específica, ordenados por fecha descendente.
     *
     * @param tipoCaja la caja a consultar ({@code FISICA}, {@code BANCARIA} o {@code COMPENSACION}).
     * @return lista de movimientos de la caja indicada; vacía si no hay movimientos.
     */
    List<CajaMovimientoResponse> listarMovimientosByCaja(TipoCaja tipoCaja);

    /**
     * Lista los movimientos de todas las cajas dentro de un rango de fechas.
     *
     * @param desde fecha de inicio del período (inclusive).
     * @param hasta fecha de fin del período (inclusive).
     * @return lista de movimientos en el período indicado, ordenados por fecha descendente.
     */
    List<CajaMovimientoResponse> listarMovimientosByPeriodo(LocalDate desde, LocalDate hasta);

    /**
     * Registra un movimiento manual en una caja (ingreso o egreso).
     *
     * <p>Actualiza el saldo de la caja indicada. El usuario autenticado que lo carga
     * queda registrado en el campo {@code creadoPor} del movimiento.</p>
     *
     * @param req       datos del movimiento (tipo, caja, concepto, monto, fecha).
     * @param creadoPor nombre o identificador del usuario que registra el movimiento.
     * @return el movimiento registrado con su ID asignado.
     */
    CajaMovimientoResponse registrarMovimiento(CajaMovimientoRequest req, String creadoPor);
}
