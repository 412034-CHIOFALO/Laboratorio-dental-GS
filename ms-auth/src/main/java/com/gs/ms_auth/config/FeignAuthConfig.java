package com.gs.ms_auth.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Propaga el header Authorization (JWT) del request HTTP actual a las
 * llamadas Feign salientes de ms-auth.
 *
 * <p>Sin esto, la llamada a ms-finanzas para dar de alta el sueldo de un
 * empleado recién activado viajaría sin token y sería rechazada (401/403).</p>
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
