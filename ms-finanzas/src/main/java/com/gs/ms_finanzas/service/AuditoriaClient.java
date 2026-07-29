package com.gs.ms_finanzas.service;

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
 * Envía eventos de auditoría de ms-finanzas al registro central de ms-auth
 * ({@code POST /api/auth/auditoria/ingest}).
 *
 * <p>Fire-and-forget: el método es {@code @Async} y traga cualquier error, así
 * la auditoría NUNCA bloquea ni rompe la operación de negocio. Si ms-auth está
 * caído, el evento se pierde (best-effort, aceptable para este sistema — no hay
 * broker) y solo queda un warning en el log.</p>
 *
 * <p>El usuario se resuelve del SecurityContext: para operaciones de usuario es
 * el 'sub' del JWT; para las del bot es "gs-bot" (lo pone BotApiKeyFilter).</p>
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

    /**
     * Registra un evento de auditoría de forma asíncrona.
     *
     * @param tipo    categoría (PAGO, COBRO, CAJA, PROVEEDOR, SUELDO, ...)
     * @param accion  descripción corta de la operación
     * @param entidad entidad afectada (ej: "Comprobante COMP-202607-0007")
     * @param detalle contexto adicional (montos, medio de pago, etc.)
     */
    @Async
    public void registrar(String tipo, String accion, String entidad, String detalle) {
        if (internalKey == null || internalKey.isBlank()) return; // sin key no auditamos (evita 403 en loop)
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
