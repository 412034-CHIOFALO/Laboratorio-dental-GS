package com.gs.ms_finanzas.service;

import com.gs.ms_finanzas.dto.ComprobanteRequest;
import com.gs.ms_finanzas.dto.ComprobanteResponse;
import com.gs.ms_finanzas.dto.CuentaCorrienteOdontologoResponse;
import com.gs.ms_finanzas.dto.PagoCuentaCorrienteRequest;
import com.gs.ms_finanzas.dto.PagoCuentaCorrienteResponse;

import java.math.BigDecimal;
import java.util.List;

/**
 * Servicio de gestión de comprobantes de deuda emitidos a odontólogos
 * y seguimiento de sus cuentas corrientes.
 *
 * <p>Un comprobante se emite al entregar un trabajo terminado al odontólogo
 * y representa la deuda hasta que el pago sea registrado.
 * El ciclo de vida es: {@code PENDIENTE} → {@code COBRADO} (o {@code VENCIDO}).</p>
 */
public interface IFinanzasService {

    /**
     * Devuelve todos los comprobantes emitidos, sin filtrar por estado.
     *
     * @return lista de todos los comprobantes (PENDIENTE, COBRADO, VENCIDO); vacía si no hay ninguno.
     */
    List<ComprobanteResponse> listarTodos();

    /**
     * Devuelve únicamente los comprobantes en estado PENDIENTE (sin cobrar).
     *
     * @return lista de comprobantes pendientes; vacía si todos están cobrados.
     */
    List<ComprobanteResponse> listarPendientes();

    /**
     * Devuelve todos los comprobantes de un odontólogo específico.
     *
     * @param odontologoId ID del odontólogo en ms-auth.
     * @return lista de comprobantes del odontólogo, en cualquier estado.
     */
    List<ComprobanteResponse> listarPorOdontologo(Long odontologoId);

    /**
     * Calcula el saldo deudor total de un odontólogo (suma de comprobantes PENDIENTES).
     *
     * @param odontologoId ID del odontólogo en ms-auth.
     * @return saldo pendiente en pesos; {@link BigDecimal#ZERO} si no tiene deuda.
     */
    BigDecimal saldoPendienteOdontologo(Long odontologoId);

    /**
     * Busca un comprobante por su ID interno.
     *
     * @param id ID del comprobante.
     * @return el comprobante encontrado.
     * @throws com.gs.ms_finanzas.exception.ResourceNotFoundException si no existe.
     */
    ComprobanteResponse buscarPorId(Long id);

    /**
     * Emite un nuevo comprobante de deuda al odontólogo por un trabajo entregado.
     *
     * <p>El estado inicial del comprobante es {@code PENDIENTE}.
     * Se valida que no exista otro comprobante activo para el mismo pedido.</p>
     *
     * @param request datos del comprobante (pedido, odontólogo, monto, fecha de emisión).
     * @return el comprobante creado con su número asignado.
     * @throws com.gs.ms_finanzas.exception.ConflictException si ya existe un comprobante para ese pedido.
     */
    ComprobanteResponse emitir(ComprobanteRequest request);

    /**
     * Marca un comprobante como cobrado, registrando la fecha de cobro actual.
     *
     * @param id ID del comprobante a cobrar.
     * @return el comprobante actualizado con estado {@code COBRADO}.
     * @throws com.gs.ms_finanzas.exception.ResourceNotFoundException si no existe.
     * @throws com.gs.ms_finanzas.exception.ConflictException si ya estaba cobrado.
     */
    ComprobanteResponse registrarCobro(Long id);

    /**
     * Sincroniza el monto del comprobante de un pedido cuando se corrige su
     * precio DESPUÉS de entregado. No hace nada si el pedido todavía no tiene
     * comprobante emitido.
     *
     * @param pedidoId ID del pedido en ms-pedidos.
     * @param nuevoMonto el nuevo monto a facturar.
     * @throws com.gs.ms_finanzas.exception.BusinessException si el nuevo monto es menor a lo ya cobrado.
     */
    void actualizarMontoPorPedido(Long pedidoId, java.math.BigDecimal nuevoMonto);

    /**
     * Genera el ranking de odontólogos con deuda pendiente, ordenado de mayor a menor deuda.
     *
     * <p>Solo incluye odontólogos con al menos un comprobante en estado {@code PENDIENTE}.
     * Cada fila agrupa todos los comprobantes del odontólogo y calcula:
     * <ul>
     *   <li>Saldo total pendiente.</li>
     *   <li>Cantidad de comprobantes pendientes.</li>
     *   <li>Días transcurridos desde el primer vencimiento.</li>
     *   <li>Severidad: {@code AL_DIA}, {@code LEVE}, {@code MODERADA}, {@code GRAVE} o {@code CRITICA}.</li>
     * </ul></p>
     *
     * @return lista de cuentas corrientes ordenada por deuda descendente; vacía si nadie debe.
     */
    List<CuentaCorrienteOdontologoResponse> rankingMorosos();

    /**
     * Igual que {@link #rankingMorosos()} pero incluye también a los
     * odontólogos que ya saldaron toda su deuda (quedan con {@code totalDeuda
     * = 0} y severidad {@code AL_DIA}). Es la fuente para la pestaña "Todos"
     * de Cuentas Corrientes — antes esa pestaña usaba el mismo dato que "Solo
     * morosos" y por eso nunca mostraba diferencia alguna.
     *
     * @return lista de cuentas corrientes de todos los odontólogos con al menos un comprobante emitido.
     */
    List<CuentaCorrienteOdontologoResponse> listarTodasCuentas();

    /**
     * Registra un pago manual a la cuenta corriente de un odontólogo, imputándolo
     * a sus comprobantes con saldo (más viejos primero, parcial o total) e
     * ingresando el dinero a la caja según el medio.
     *
     * @param odontologoId ID del odontólogo.
     * @param request monto, medio (efectivo/transferencia), fecha y nota.
     * @return resumen del pago: monto imputado, comprobantes afectados y saldo restante.
     * @throws com.gs.ms_finanzas.exception.BusinessException si el odontólogo no tiene deudas pendientes.
     */
    PagoCuentaCorrienteResponse registrarPagoCuentaCorriente(Long odontologoId, PagoCuentaCorrienteRequest request);

    /**
     * Histórico de pagos a cuenta corriente de un odontólogo (más recientes primero).
     *
     * @param odontologoId ID del odontólogo.
     * @return lista de pagos registrados; vacía si no hizo ninguno.
     */
    List<PagoCuentaCorrienteResponse> historialPagosOdontologo(Long odontologoId);
}
