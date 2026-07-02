package com.gs.ms_stock.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** Payload para actualizar la configuración de alertas de stock. */
@Data
public class ConfiguracionAlertaRequest {

    /** Número de WhatsApp del administrador en formato internacional (5491155443322). */
    @Pattern(regexp = "^[0-9]{8,15}$|^$",
             message = "El número de WhatsApp debe tener solo dígitos en formato internacional (ej: 5493511234567)")
    private String adminWhatsappPhone;

    /** true para activar el envío de alertas por WhatsApp cuando un material baja del mínimo. */
    private boolean alertasActivas;
}
