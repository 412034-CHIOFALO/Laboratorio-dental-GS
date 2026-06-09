package com.gys.ms_finanzas.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Autenticación por API key para el bot de WhatsApp.
 *
 * El bot NO usa JWT (que expira en 1 h) sino una API key fija que manda en el
 * header {@code X-Bot-Api-Key}. Si la key coincide con la configurada, se lo
 * autentica como un usuario de servicio con ROLE_ADMIN, así puede registrar
 * pagos automáticos sin renovar tokens.
 *
 * Solo aplica al endpoint del bot ({@code /sueldos/pago-automatico}); el resto
 * de la API sigue protegido por JWT normal.
 */
@Component
public class BotApiKeyFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Bot-Api-Key";
    private static final String RUTA_BOT = "/api/finanzas/sueldos/pago-automatico";

    @Value("${gs.bot.api-key:}")
    private String botApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (request.getRequestURI().endsWith(RUTA_BOT)) {
            String key = request.getHeader(HEADER);
            if (botApiKey != null && !botApiKey.isBlank() && botApiKey.equals(key)) {
                var auth = new UsernamePasswordAuthenticationToken(
                        "gs-bot", null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(request, response);
    }
}
