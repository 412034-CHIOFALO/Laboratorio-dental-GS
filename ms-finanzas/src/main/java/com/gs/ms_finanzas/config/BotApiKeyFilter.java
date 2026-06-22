package com.gs.ms_finanzas.config;

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
import java.util.Set;

/**
 * Autenticación por API key para el bot de WhatsApp.
 *
 * El bot NO usa JWT (que expira en 1 h) sino una API key fija que manda en el
 * header {@code X-Bot-Api-Key}. Si la key coincide con la configurada, se lo
 * autentica como un usuario de servicio con ROLE_ADMIN, así puede registrar
 * pagos automáticos sin renovar tokens.
 *
 * Aplica a los endpoints del bot ({@code /sueldos/pago-automatico} para
 * transferencias y {@code /sueldos/pago-efectivo} para borradores de efectivo);
 * el resto de la API sigue protegido por JWT normal.
 */
@Component
public class BotApiKeyFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Bot-Api-Key";
    private static final Set<String> RUTAS_BOT = Set.of(
            "/api/finanzas/sueldos/pago-automatico",
            "/api/finanzas/sueldos/pago-efectivo");

    @Value("${gs.bot.api-key:}")
    private String botApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (esRutaBot(request.getRequestURI())) {
            String key = request.getHeader(HEADER);
            if (claveValida(key)) {
                var auth = new UsernamePasswordAuthenticationToken(
                        "gs-bot", null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(request, response);
    }

    private boolean esRutaBot(String uri) {
        return RUTAS_BOT.stream().anyMatch(uri::endsWith);
    }

    /**
     * Compara la API key en tiempo constante (MessageDigest.isEqual) para no
     * filtrar información por timing. Si no hay key configurada, siempre rechaza.
     */
    private boolean claveValida(String key) {
        if (key == null || botApiKey == null || botApiKey.isBlank()) return false;
        return java.security.MessageDigest.isEqual(
                botApiKey.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                key.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
