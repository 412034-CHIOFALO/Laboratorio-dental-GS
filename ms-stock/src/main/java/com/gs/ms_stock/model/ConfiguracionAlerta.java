package com.gs.ms_stock.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Configuración singleton de alertas de stock.
 * Siempre existe un único registro con id = 1.
 * El admin configura desde la UI el número de WhatsApp al que se envían las alertas.
 */
@Entity
@Table(name = "configuracion_alerta_stock")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracionAlerta {

    /** Clave fija: siempre 1. */
    @Id
    private Long id;

    /** Número de WhatsApp del administrador (formato internacional: 5491155443322). */
    @Column(name = "admin_whatsapp_phone", length = 30)
    private String adminWhatsappPhone;

    /** Activar/desactivar alertas de stock bajo por WhatsApp. */
    @Column(name = "alertas_activas", nullable = false)
    @Builder.Default
    private boolean alertasActivas = false;
}
