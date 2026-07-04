package com.gs.ms_stock.service;

import com.gs.ms_stock.model.Material;
import com.gs.ms_stock.repository.ConfiguracionAlertaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Envía alertas de stock bajo al administrador via WhatsApp (a través del bot interno).
 * La notificación es async (sendAsync) para no bloquear el movimiento de stock.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlertaStockService {

    @Value("${gs.bot.url:http://localhost:3001}")
    private String botUrl;

    @Value("${gs.bot.api-key:}")
    private String botApiKey;

    private final ConfiguracionAlertaRepository configRepo;

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public void notificarStockBajo(Material material) {
        configRepo.findById(1L).ifPresent(config -> {
            if (!config.isAlertasActivas()) return;
            String phone = config.getAdminWhatsappPhone();
            if (phone == null || phone.isBlank()) {
                log.warn("[STOCK-ALERTA] Alertas activas pero sin número de WhatsApp configurado.");
                return;
            }

            String texto =
                "⚠️ *Alerta de Stock — Laboratorio GS*\n" +
                "Material: *" + escape(material.getNombre()) + "*\n" +
                "Stock actual: *" + material.getStockActual() + " " + escape(material.getUnidadMedida()) + "*\n" +
                "Mínimo configurado: " + material.getStockMinimo() + " " + escape(material.getUnidadMedida()) + "\n" +
                "_Gestione la reposición en el sistema._";

            String json = "{\"telefono\":\"" + escape(phone) + "\",\"texto\":\"" + escapeJson(texto) + "\"}";

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(botUrl + "/api/bot/mensaje"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .timeout(Duration.ofSeconds(10));
            if (!botApiKey.isBlank()) reqBuilder.header("X-Bot-Api-Key", botApiKey);

            HTTP.sendAsync(reqBuilder.build(), HttpResponse.BodyHandlers.discarding())
                .whenComplete((res, err) -> {
                    if (err != null) log.warn("[STOCK-ALERTA] Error enviando alerta: {}", err.getMessage());
                    else log.info("[STOCK-ALERTA] Alerta enviada a {} para '{}'", phone, material.getNombre());
                });
        });
    }

    private String escape(String s) {
        return s == null ? "" : s;
    }

    private String escapeJson(String s) {
        return s == null ? "" : s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}
