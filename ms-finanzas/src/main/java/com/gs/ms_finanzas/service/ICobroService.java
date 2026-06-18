package com.gs.ms_finanzas.service;

import com.gs.ms_finanzas.dto.CobroRequest;
import com.gs.ms_finanzas.dto.RegistroCobroResponse;

/**
 * Servicio para el registro de cobros efectuados a odontólogos.
 *
 * <p>Un cobro puede ser:
 * <ul>
 *   <li><b>EFECTIVO</b>: el odontólogo paga en mano al laboratorio.</li>
 *   <li><b>TRANSFERENCIA</b>: transferencia bancaria directa al laboratorio.</li>
 *   <li><b>TRIANGULADO</b>: el odontólogo paga directamente a un proveedor en nombre
 *       del laboratorio; se impacta la caja de COMPENSACION.</li>
 * </ul></p>
 */
public interface ICobroService {

    /**
     * Registra un cobro y actualiza la cuenta corriente del odontólogo.
     *
     * <p>El proceso aplica el monto recibido contra los comprobantes pendientes del
     * odontólogo en orden de emisión (FIFO). Si el monto cubre varios comprobantes,
     * todos quedan en estado {@code COBRADO}. Si cubre parcialmente uno, el saldo
     * restante queda pendiente.</p>
     *
     * <p>Adicionalmente, actualiza el saldo de la caja correspondiente al tipo de cobro.</p>
     *
     * @param request datos del cobro (odontólogo, monto, tipo de cobro, fecha).
     * @return resumen del cobro con los comprobantes afectados y el nuevo saldo.
     * @throws com.gs.ms_finanzas.exception.ResourceNotFoundException si el odontólogo no tiene comprobantes.
     * @throws com.gs.ms_finanzas.exception.BusinessException si el monto es inválido.
     */
    RegistroCobroResponse registrarCobro(CobroRequest request);
}
