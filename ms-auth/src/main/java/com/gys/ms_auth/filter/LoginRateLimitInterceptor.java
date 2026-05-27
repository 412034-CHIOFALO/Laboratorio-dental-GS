package com.gys.ms_auth.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter para el endpoint POST /api/auth/login.
 *
 * Algoritmo: Token Bucket (Bucket4j)
 *   - Capacidad: 5 intentos
 *   - Recarga: 5 tokens cada 1 minuto (greedy — recarga continua)
 *   - Clave: IP del cliente (o X-Forwarded-For si hay proxy)
 *
 * Efecto práctico:
 *   - 5 intentos por minuto por IP
 *   - Al sexto intento dentro del minuto → HTTP 429 Too Many Requests
 *   - Después de 1 minuto sin intentos → se restablecen los 5 tokens
 *
 * NOTA: Esta implementación usa memoria local (ConcurrentHashMap).
 *       Para producción con múltiples instancias se recomienda usar
 *       Bucket4j con Redis como backend distribuido.
 */
public class LoginRateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(LoginRateLimitInterceptor.class);
    private static final String LOGIN_PATH = "/api/auth/login";
    private static final int CAPACITY = 5;
    private static final Duration REFILL_PERIOD = Duration.ofMinutes(1);

    // Un Bucket por IP — se crea lazily en el primer intento
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        if (!HttpMethod.POST.matches(request.getMethod()) ||
            !LOGIN_PATH.equals(request.getRequestURI())) {
            return true; // Solo aplica al login
        }

        String ip = resolveClientIp(request);
        Bucket bucket = buckets.computeIfAbsent(ip, this::newBucket);

        if (bucket.tryConsume(1)) {
            // Informativo: tokens restantes
            long remaining = bucket.getAvailableTokens();
            response.addHeader("X-RateLimit-Remaining", String.valueOf(remaining));
            return true;
        }

        log.warn("[GYS-SECURITY] Rate limit excedido para IP: {}", ip);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
            "{\"error\": \"Demasiados intentos de login. Esperá 1 minuto antes de volver a intentar.\"}"
        );
        return false;
    }

    private Bucket newBucket(String ip) {
        return Bucket.builder()
            .addLimit(Bandwidth.builder()
                .capacity(CAPACITY)
                .refillGreedy(CAPACITY, REFILL_PERIOD)
                .build())
            .build();
    }

    /**
     * Extrae la IP real del cliente teniendo en cuenta proxies/load balancers.
     * Prioridad: X-Forwarded-For → X-Real-IP → RemoteAddr
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        return request.getRemoteAddr();
    }
}
