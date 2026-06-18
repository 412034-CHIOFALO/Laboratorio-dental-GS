package com.gs.ms_pedidos.service;

import com.gs.ms_pedidos.model.Odontologo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Dispara notificaciones WhatsApp a odontólogos cuando su pedido está listo.
 * Llama al endpoint interno del bot (POST /api/bot/notificar).
 *
 * Si el bot no está disponible o el odontólogo no tiene teléfono registrado,
 * el error se loguea pero NO propaga — la transición de estado no debe fallar
 * por un problema de notificación.
 */
@Service
@Slf4j
public class NotificacionBotService {

    private final RestClient restClient;

    public NotificacionBotService(
            @Value("${gs.bot.uri:http://localhost:3001}") String botUri,
            @Value("${gs.bot.api-key:}") String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl(botUri)
                .defaultHeader("X-Bot-Api-Key", apiKey)
                .build();
    }

    /**
     * Envía un mensaje WhatsApp al odontólogo avisando que su pedido está LISTO.
     * Fire-and-forget: los errores no se propagan.
     */
    public void notificarPedidoListo(String nroPedido, String trabajo, Odontologo odontologo) {
        if (odontologo == null || odontologo.getTelefono() == null || odontologo.getTelefono().isBlank()) {
            log.debug("[Bot] Odontólogo sin teléfono — no se envía notificación para {}", nroPedido);
            return;
        }
        try {
            restClient.post()
                    .uri("/api/bot/notificar")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "telefono",  odontologo.getTelefono(),
                            "nombre",    odontologo.getNombre(),
                            "nroPedido", nroPedido,
                            "trabajo",   trabajo
                    ))
                    .retrieve()
                    .toBodilessEntity();
            log.info("[Bot] Notificación WhatsApp enviada a {} ({})", odontologo.getNombre(), nroPedido);
        } catch (Exception e) {
            log.warn("[Bot] No se pudo enviar notificación WhatsApp para {}: {}", nroPedido, e.getMessage());
        }
    }
}
