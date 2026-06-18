package com.gs.ms_finanzas.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Pago detectado por el bot de WhatsApp a partir de un comprobante.
 *
 * El bot identifica al receptor (técnico/integrante) por su teléfono y/o
 * nombre, parsea el emisor del pie del mensaje, y manda este request.
 *
 * El receptor se resuelve por:
 *   1. receptorUsuarioId (si el bot ya lo mapeó), o
 *   2. receptorTelefono (el sistema busca el empleado con ese teléfono)
 */
@Data
public class PagoAutomaticoRequest {

    /** Id del empleado receptor, si el bot ya lo conoce. Opcional. */
    private Long receptorUsuarioId;

    /** Teléfono del receptor — usado para resolver el empleado si no vino el id. */
    private String receptorTelefono;

    /** Nombre del receptor (del pie del mensaje) — resolución por nombre. */
    private String receptorNombre;

    @NotNull @Positive(message = "El monto debe ser mayor a cero")
    private BigDecimal monto;

    private LocalDate fecha;

    // ── Trazabilidad del bot ──
    /** Quién mandó el comprobante al grupo (integrante del lab). */
    private String cargadoPorNombre;
    private String cargadoPorTelefono;

    /** Quién emitió el pago (del pie del mensaje, típicamente un odontólogo). */
    private String emisor;

    private String comprobanteUrl;
    private String grupoOrigen;
    private String nota;

    /** Nro de operación del comprobante — para evitar registrar duplicados. */
    private String idOperacion;

    // ── Archivo del comprobante (para guardarlo en MinIO) ──
    /** Contenido del comprobante en base64 (el bot lo manda así). */
    private String comprobanteBase64;
    private String comprobanteMime;
    private String comprobanteNombre;
}
