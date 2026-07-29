package com.gs.ms_auth.config;

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
 * Autenticación por key interna para el tráfico servicio-a-servicio hacia ms-auth
 * (hoy: la ingesta de auditoría desde los otros MS).
 *
 * <p>Los MS no tienen un JWT propio para llamar a ms-auth — el JWT del request es
 * del usuario, y para el bot ni siquiera hay JWT. Por eso, igual que el
 * {@code BotApiKeyFilter} de ms-finanzas, se usa una key fija en el header
 * {@code X-Internal-Key}: si coincide, la request queda autenticada como el
 * principal de servicio {@code servicio-interno} con {@code ROLE_INTERNAL}.</p>
 *
 * <p>Solo aplica a las rutas internas (no toca el resto de la API, que sigue con
 * JWT normal). La comparación es en tiempo constante para no filtrar por timing.</p>
 */
@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Internal-Key";
    private static final String RUTA_INTERNA = "/api/auth/auditoria/ingest";

    @Value("${GS_INTERNAL_API_KEY:}")
    private String internalKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (request.getRequestURI().endsWith(RUTA_INTERNA) && claveValida(request.getHeader(HEADER))) {
            var auth = new UsernamePasswordAuthenticationToken(
                    "servicio-interno", null,
                    List.of(new SimpleGrantedAuthority("ROLE_INTERNAL")));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        chain.doFilter(request, response);
    }

    private boolean claveValida(String key) {
        if (key == null || internalKey == null || internalKey.isBlank()) return false;
        return java.security.MessageDigest.isEqual(
                internalKey.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                key.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
