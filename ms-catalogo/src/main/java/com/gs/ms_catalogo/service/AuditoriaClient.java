package com.gs.ms_catalogo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Envía eventos de auditoría de ms-catalogo al registro central de ms-auth
 * ({@code POST /api/auth/auditoria/ingest}).
 *
 * <p>Fire-and-forget: {@code @Async} + traga errores, así la auditoría nunca
 * bloquea ni rompe la operación de negocio. Best-effort (sin broker): si ms-auth
 * está caído el evento se pierde y queda un warning en el log.</p>
 */
@Component
public class AuditoriaClient {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaClient.class);

    private final RestClient restClient;
    private final String internalKey;

    public AuditoriaClient(@Value("${AUTH_URI:http://ms-auth:8081}") String authUri,
                           @Value("${GS_INTERNAL_API_KEY:}") String internalKey) {
        this.restClient = RestClient.builder().baseUrl(authUri).build();
        this.internalKey = internalKey;
    }

    @Async
    public void registrar(String tipo, String accion, String entidad, String detalle) {
        if (internalKey == null || internalKey.isBlank()) return;
        try {
            restClient.post()
                    .uri("/api/auth/auditoria/ingest")
                    .header("X-Internal-Key", internalKey)
                    .body(Map.of(
                            "usuario", usuarioActual(),
                            "tipo", tipo,
                            "accion", accion,
                            "entidad", entidad,
                            "detalle", detalle != null ? detalle : ""))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("[AUDIT] No se pudo registrar el evento '{}' en ms-auth: {} (se ignora)", accion, e.getMessage());
        }
    }

    private String usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getName() != null) ? auth.getName() : "sistema";
    }
}
