package com.gs.ms_finanzas.service;

import com.gs.ms_finanzas.dto.*;
import com.gs.ms_finanzas.model.TipoCaja;

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
     * Actualiza la configuración de sueldo de un empleado ya dado de alta.
     *
     * @param usuarioId ID del usuario en ms-auth.
     * @param req       configuración a aplicar (frecuencia de pago y monto base).
     * @return la configuración actualizada.
     * @throws com.gs.ms_finanzas.exception.ResourceNotFoundException si el empleado no fue dado de alta con {@link #crearEmpleado}.
     */
    EmpleadoSueldoResponse guardarConfig(Long usuarioId, ConfigSueldoRequest req);

    /**
     * Da de alta a un integrante del laboratorio en el módulo de sueldos.
     *
     * <p>ms-finanzas mantiene su propia tabla de empleados, denormalizada de
     * ms-auth: un usuario nuevo (creado en Usuarios) no es reconocido acá ni
     * por el bot de WhatsApp hasta que se lo da de alta con este método.</p>
     *
     * @param req datos del empleado (usuarioId, nombre, rol, teléfono y configuración inicial).
     * @return la configuración recién creada, con saldo devengado y sobrante en cero.
     * @throws com.gs.ms_finanzas.exception.ConflictException si ya existe una configuración para ese usuarioId.
     */
    EmpleadoSueldoResponse crearEmpleado(CrearEmpleadoRequest req);

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
     * Devuelve la clave del objeto (MinIO) del comprobante de un pago de sueldo,
     * para que el controller lo sirva en streaming a través del propio backend.
     *
     * @param pagoId ID del pago de sueldo.
     * @return clave del objeto en MinIO.
     * @throws com.gs.ms_finanzas.exception.ResourceNotFoundException si el pago no existe.
     * @throws com.gs.ms_finanzas.exception.BusinessException si el pago no tiene comprobante guardado.
     */
    String objectKeyComprobante(Long pagoId);

    /**
     * Devuelve la clave del objeto (MinIO) del comprobante asociado a un registro
     * del bot de WhatsApp, para que el controller lo sirva en streaming.
     *
     * @param registroId ID del registro del bot.
     * @return clave del objeto en MinIO.
     * @throws com.gs.ms_finanzas.exception.ResourceNotFoundException si el registro no existe.
     * @throws com.gs.ms_finanzas.exception.BusinessException si el registro no tiene comprobante adjunto.
     */
    String objectKeyComprobanteRegistro(Long registroId);

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

    /**
     * Algoritmo de cascada: sugiere cómo distribuir un cobro entre los empleados
     * activos con saldo devengado pendiente (en orden alfabético) y, con lo que
     * sobra, propone asignarlo a {@code cajaRemanente}.
     *
     * <p>Es puramente un cálculo — no registra nada. El administrativo revisa la
     * propuesta (puede ajustarla) y confirma cada línea con los endpoints
     * habituales de pago de sueldo / movimiento de caja.</p>
     *
     * @param monto          importe del cobro a distribuir.
     * @param cajaRemanente  caja a la que se propone asignar lo que sobre.
     * @return la distribución sugerida.
     */
    DistribucionCascadaResponse sugerirCascada(BigDecimal monto, TipoCaja cajaRemanente);

    /**
     * Devenga automáticamente el sueldo de todos los empleados activos, prorrateado
     * por día corrido según su {@link com.gs.ms_finanzas.model.FrecuenciaPago}.
     *
     * <p>Para cada empleado activo, calcula los días transcurridos desde el último
     * cálculo ({@code ultimoDevengoCalculado}) — o desde su fecha de alta si es la
     * primera vez — y le acredita {@code montoBase / frecuencia.diasDeCiclo()} por
     * cada día. Así un empleado que se dio de alta a mitad de mes ya tiene devengado
     * proporcional desde ese mismo día, sin esperar a que cierre el ciclo completo.</p>
     *
     * <p>Pensado para correr una vez por día (ver el scheduler), pero es idempotente
     * en el sentido de que nunca vuelve a contar un día ya devengado — se puede
     * invocar manualmente sin miedo a duplicar devengado.</p>
     */
    void devengarDiario();
}
