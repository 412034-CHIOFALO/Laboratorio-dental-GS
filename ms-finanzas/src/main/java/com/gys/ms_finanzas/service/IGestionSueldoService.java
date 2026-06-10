package com.gys.ms_finanzas.service;

import com.gys.ms_finanzas.dto.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Gestión de sueldos del personal: configuración, devengado, pagos (manuales
 * y del bot) e histórico. Separado del {@link ISueldoService} legacy (por mes).
 */
public interface IGestionSueldoService {

    List<EmpleadoSueldoResponse> listarEmpleados();

    EmpleadoSueldoResponse buscarEmpleado(Long usuarioId);

    /** Alta/actualización de la config de sueldo de un empleado. */
    EmpleadoSueldoResponse guardarConfig(Long usuarioId, ConfigSueldoRequest req);

    /** Pago manual desde la app. */
    PagoSueldoResponse registrarPago(PagoSueldoRequest req);

    /** Comprobante procesado por el bot de WhatsApp (sueldo, proveedor o rechazo). */
    RegistroPagoBotResponse registrarPagoAutomatico(PagoAutomaticoRequest req);

    /** Ajuste manual del saldo devengado (corrección). */
    EmpleadoSueldoResponse ajustarDevengado(Long usuarioId, BigDecimal nuevoDevengado);

    List<PagoSueldoResponse> historialPagos(Long usuarioId);

    List<PagoSueldoResponse> historialPagosGlobal();

    /** Historial de TODO lo que procesó el bot (sueldos, proveedores y rechazos). */
    List<RegistroPagoBotResponse> listarRegistrosBot();

    BigDecimal totalDevengado();

    /** URL temporal para ver el comprobante guardado de un pago (o null). */
    String urlComprobante(Long pagoId);

    /** URL temporal para ver el comprobante de un registro del bot. */
    String urlComprobanteRegistro(Long registroId);
}
