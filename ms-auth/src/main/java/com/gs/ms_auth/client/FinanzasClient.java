package com.gs.ms_auth.client;

import com.gs.ms_auth.client.dto.CrearEmpleadoRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Cliente Feign hacia ms-finanzas.
 *
 * Lo usa UsuarioService para dar de alta automáticamente el sueldo de un
 * empleado (TECNICO/ADMINISTRATIVO/ADMIN) apenas se activa su cuenta —
 * ms-finanzas mantiene su propia tabla de empleados, separada de ms-auth.
 *
 * Resolución vía Eureka. El JWT del request actual (el del ADMINISTRATIVO que
 * activa) se propaga con {@link com.gs.ms_auth.config.FeignAuthConfig}.
 */
@FeignClient(name = "ms-finanzas", fallback = FinanzasClientFallback.class)
public interface FinanzasClient {

    @PostMapping("/api/finanzas/sueldos/empleados")
    Object crearEmpleado(@RequestBody CrearEmpleadoRequest request);
}
