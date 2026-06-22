package com.gs.ms_stock.dto;

import lombok.Data;

/** Payload para actualizar la configuración de alertas de stock. */
@Data
public class ConfiguracionAlertaRequest {

    /** Número de WhatsApp del administrador en formato internacional (5491155443322). */
    private String adminWhatsappPhone;

    /** true para activar el envío de alertas por WhatsApp cuando un material baja del mínimo. */
    private boolean alertasActivas;
}
