package com.gs.ms_finanzas.service;

import com.gs.ms_finanzas.dto.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Servicio de gestión integral de sueldos del personal del laboratorio.
 *
 * <p>Maneja tres aspectos principales:
 * <ol>
 *   <li><b>Configuración</b>: alta/actualización de la config de sueldo de cada empleado
 *       (frecuencia de pago y monto base).</li>
 *   <li><b>Devengado</b>: saldo acumulado que el laboratorio le debe a cada empleado;
 *       crece con el tiempo y disminuye con cada pago.</li>
 *   <li><b>Pagos</b>: registro manual (desde la app) y automático (desde el bot de WhatsApp)
 *       con algoritmo de <i>cascada</i> para manejar sobrantes entre ciclos.</li>
 * </ol></p>
 *
 * <p>Es el único sistema de sueldos del módulo: gestiona el devengado por
 * empleado y registra los pagos (manuales, por bot y en efectivo).</p>
 */
public interface IGestionSueldoService {

    /**
     * Devuelve todos los empleados activos con su configuración de sueldo y estado de cuenta.
     *
     * @return lista de empleados con config y saldo devengado; vacía si no hay ninguno.
     */
    List<EmpleadoSueldoResponse> listarEmpleados();

    /**
     * Busca un empleado por su ID de usuario en ms-auth.
     *
     * @param usuarioId ID del usuario en ms-auth.
     * @return datos del empleado con su configuración y saldo devengado.
     * @throws com.gs.ms_finanzas.exception.ResourceNotFoundException si no existe configuración para ese usuario.
     */
    EmpleadoSueldoResponse buscarEmpleado(Long usuarioId);

    /**
     * Crea o actualiza la configuración de sueldo de un empleado.
     *
     * <p>Si ya existe una configuración para el {@code usuarioId}, la actualiza.
     * Si no existe, la crea con saldo devengado y sobrante en cero.</p>
     *
     * @param usuarioId ID del usuario en ms-auth.
     * @param req       configuración a aplicar (frecuencia de pago y monto base).
     * @return la configuración actualizada.
     */
    EmpleadoSueldoResponse guardarConfig(Long usuarioId, ConfigSueldoRequest req);

    /**
     * Registra un pago de sueldo desde la aplicación web (pago manual).
     *
     * <p>El monto se descuenta del saldo devengado del empleado. Si supera el devengado,
     * se aplica la política de {@link com.gs.ms_finanzas.model.ManejoSobrante} indicada
     * en el request.</p>
     *
     * @param req datos del pago (empleado, monto, fecha, política de sobrante).
     * @return detalle del pago registrado con el nuevo saldo devengado.
     * @throws com.gs.ms_finanzas.exception.ResourceNotFoundException si el empleado no tiene configuración.
     */
    PagoSueldoResponse registrarPago(PagoSueldoRequest req);

    /**
     * Procesa un comprobante de pago enviado por el bot de WhatsApp al grupo.
     *
     * <p>El bot extrae el receptor y el monto del comprobante, e intenta resolverlo
     * contra empleados y proveedores por número de teléfono o nombre.
     * El resultado siempre se devuelve en el body (nunca arroja excepción de negocio):
     * <ul>
     *   <li>{@link com.gs.ms_finanzas.model.EstadoRegistroBot#REGISTRADO} — pago aplicado.</li>
     *   <li>{@link com.gs.ms_finanzas.model.EstadoRegistroBot#RECHAZADO} — receptor desconocido.</li>
     *   <li>{@link com.gs.ms_finanzas.model.EstadoRegistroBot#DUPLICADO} — comprobante ya procesado.</li>
     * </ul></p>
     *
     * @param req datos del comprobante procesado por el bot.
     * @return resultado del procesamiento con estado, mensaje y datos del receptor resuelto.
     */
    RegistroPagoBotResponse registrarPagoAutomatico(PagoAutomaticoRequest req);

    /**
     * Ajusta manualmente el saldo devengado de un empleado.
     *
     * <p>Operación de corrección para reconciliaciones contables. No genera un pago real;
     * únicamente modifica el campo {@code saldoDevengado} de la configuración.</p>
     *
     * @param usuarioId     ID del usuario en ms-auth.
     * @param nuevoDevengado valor absoluto que reemplaza el saldo devengado actual.
     * @return la configuración actualizada con el nuevo devengado.
     * @throws com.gs.ms_finanzas.exception.ResourceNotFoundException si el empleado no tiene configuración.
     */
    EmpleadoSueldoResponse ajustarDevengado(Long usuarioId, BigDecimal nuevoDevengado);

    /**
     * Devuelve el historial de pagos de un empleado específico.
     *
     * @param usuarioId ID del usuario en ms-auth.
     * @return lista de pagos del empleado, ordenados por fecha descendente.
     * @throws com.gs.ms_finanzas.exception.ResourceNotFoundException si el empleado no tiene configuración.
     */
    List<PagoSueldoResponse> historialPagos(Long usuarioId);

    /**
     * Devuelve el historial de todos los pagos de sueldos del laboratorio.
     *
     * @return lista global de pagos (manuales y del bot), ordenados por fecha descendente.
     */
    List<PagoSueldoResponse> historialPagosGlobal();

    /**
     * Devuelve la bitácora de todos los registros procesados por el bot de WhatsApp.
     *
     * <p>Incluye pagos exitosos (a empleados y proveedores), rechazos y duplicados.
     * Útil para auditoría y diagnóstico del funcionamiento del bot.</p>
     *
     * @return lista de registros del bot, ordenados por fecha-hora descendente.
     */
    List<RegistroPagoBotResponse> listarRegistrosBot();

    /**
     * Calcula el total de saldo devengado pendiente de pago de todos los empleados activos.
     *
     * @return suma de todos los saldos devengados; {@link BigDecimal#ZERO} si no hay devengado.
     */
    BigDecimal totalDevengado();

    /**
     * Genera una URL pre-firmada temporal (MinIO) para ver o descargar el comprobante
     * de un pago de sueldo.
     *
     * @param pagoId ID del pago de sueldo.
     * @return URL temporal válida por tiempo limitado, o {@code null} si el pago no tiene comprobante.
     * @throws com.gs.ms_finanzas.exception.ResourceNotFoundException si el pago no existe.
     */
    String urlComprobante(Long pagoId);

    /**
     * Genera una URL pre-firmada temporal (MinIO) para ver el comprobante
     * asociado a un registro del bot de WhatsApp.
     *
     * @param registroId ID del registro del bot.
     * @return URL temporal válida por tiempo limitado, o {@code null} si no tiene comprobante adjunto.
     * @throws com.gs.ms_finanzas.exception.ResourceNotFoundException si el registro no existe.
     */
    String urlComprobanteRegistro(Long registroId);

    /**
     * Registra un pago en efectivo declarado en el grupo de WhatsApp.
     * El registro queda en estado PENDIENTE hasta que el administrativo lo confirme.
     */
    RegistroPagoBotResponse registrarPagoEfectivo(PagoEfectivoRequest req);

    /**
     * Confirma un pago en efectivo PENDIENTE: aplica el sueldo al empleado
     * (o descuenta la deuda del proveedor) y registra el egreso de la caja física.
     */
    RegistroPagoBotResponse confirmarEfectivo(Long registroId);

    /**
     * Rechaza un pago en efectivo PENDIENTE, dejando trazabilidad del motivo.
     */
    RegistroPagoBotResponse rechazarEfectivo(Long registroId, String motivo);

    /**
     * Lista todos los registros del bot en estado PENDIENTE (efectivo sin confirmar).
     */
    List<RegistroPagoBotResponse> listarPendientesEfectivo();
}
