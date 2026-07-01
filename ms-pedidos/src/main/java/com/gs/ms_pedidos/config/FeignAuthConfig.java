package com.gs.ms_pedidos.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Propaga el header Authorization (JWT) del request HTTP actual a todas las
 * llamadas Feign salientes de ms-pedidos.
 *
 * <p>Sin esto, las llamadas internas (ms-stock para descontar stock, ms-finanzas
 * para emitir el comprobante al entregar) viajarían sin token y serían
 * rechazadas (401/403) por los endpoints protegidos del destino.</p>
 */
@Configuration
public class FeignAuthConfig {

    @Bean
    public RequestInterceptor authForwardingInterceptor() {
        return template -> {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return;
            HttpServletRequest req = attrs.getRequest();
            String auth = req.getHeader("Authorization");
            if (auth != null && !auth.isBlank()) {
                template.header("Authorization", auth);
            }
        };
    }
}
