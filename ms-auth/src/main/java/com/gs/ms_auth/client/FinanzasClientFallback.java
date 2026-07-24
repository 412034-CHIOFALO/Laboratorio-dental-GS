package com.gs.ms_auth.client;

import com.gs.ms_auth.client.dto.CrearEmpleadoRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Fallback de FinanzasClient: se ejecuta si ms-finanzas no responde, o si
 * responde con error (ej: 409 porque el empleado ya estaba dado de alta).
 *
 * Best-effort a propósito: la activación del usuario en ms-auth NUNCA debe
 * fallar por esto. Si el alta automática no se pudo completar, queda
 * disponible el alta manual desde Finanzas → Sueldos → "Nuevo empleado".
 */
@Component
public class FinanzasClientFallback implements FinanzasClient {

    private static final Logger log = LoggerFactory.getLogger(FinanzasClientFallback.class);

    @Override
    public Object crearEmpleado(CrearEmpleadoRequest request) {
        log.warn("[GS-AUTH] No se pudo dar de alta automáticamente en sueldos al usuario {} " +
                "(ms-finanzas no disponible, o ya estaba dado de alta). Se puede completar a mano " +
                "desde Finanzas → Sueldos → \"Nuevo empleado\".", request.usuarioId());
        return null;
    }
}
